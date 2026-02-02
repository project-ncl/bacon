package org.jboss.pnc.bacon.test.pig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.bacon.test.CLIExecutor.CONFIG_LOCATION;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.pnc.bacon.common.deliverables.DeliverableRecord;
import org.jboss.pnc.bacon.common.deliverables.DeliverableRegistry;
import org.jboss.pnc.bacon.common.deliverables.DeliverableType;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.provenance.InvocationInfo;
import org.jboss.pnc.bacon.pig.impl.addons.provenance.ProvenanceAddOn;
import org.jboss.pnc.bacon.test.AbstractTest;
import org.jboss.pnc.bacon.test.TestType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Integration test for provenance generation + redaction/sanitization rules.
 *
 * Runs by:
 * - initializing PigContext directly with the integration-tests build-config.yaml
 * - registering a dummy deliverable
 * - executing ProvenanceAddOn.trigger()
 * - asserting key provenance fields + sanitization behavior
 */
@Tag(TestType.MOCK_ONLY)
class PigProvenanceTest extends AbstractTest {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    private static final Pattern RFC3339_Z = Pattern
            .compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z$");

    @Test
    void shouldGenerateProvenanceAndSanitizeSecretsButKeepAllowlistedFields(@TempDir Path tmp) throws Exception {
        // Arrange
        Path configDir = CONFIG_LOCATION;
        assertThat(configDir.resolve("build-config.yaml")).exists();

        Path targetDir = tmp.resolve("target");
        Files.createDirectories(targetDir);

        String commandLineWithSecrets = "./bacon.jar pig run "
                + "--serviceSecretValue=supersecret "
                + "--password=abc123 "
                + "-Dmy.token=topsecret "
                + "--releaseStorageUrl https://user:pass@example.com/storage "
                + ". -p ./ --profile quarkus-prod";

        DeliverableRegistry registry = new DeliverableRegistry();

        InvocationInfo inv = InvocationInfo.capture(
                "0.1.0-test",
                commandLineWithSecrets,
                Instant.now(),
                Map.of("build-config.preprocessed.sha256", "ababbebe"));

        PigContext ctx = PigContext.init(
                true,
                configDir,
                targetDir.toString(),
                null,
                Collections.emptyMap(),
                registry,
                inv);

        ctx.initFullVersion(false);
        ctx.configureTargetDirectories();

        // Create a dummy deliverable inside the release directory
        Path releaseDir = Path.of(ctx.getReleasePath());
        assertThat(releaseDir).exists();

        Path deliverable = releaseDir.resolve("dummy-deliverable.zip");
        Files.write(deliverable, "hello".getBytes(StandardCharsets.UTF_8));

        registry.register(
                DeliverableRecord.create(
                        DeliverableType.OTHER,
                        deliverable,
                        "addon:provenance",
                        Map.of("note", "integration-test")));

        // Trigger the add-on
        ProvenanceAddOn addOn = new ProvenanceAddOn(
                ctx.getPigConfiguration(),
                Collections.emptyMap(),
                ctx.getReleasePath(),
                ctx.getExtrasPath());
        addOn.trigger();

        // Assert: provenance file exists
        Path provenanceDir = Path.of(ctx.getExtrasPath()).resolve("provenance");
        Path provenanceFile = provenanceDir.resolve("dummy-deliverable.zip.provenance.json");

        assertThat(provenanceDir).exists();
        assertThat(provenanceFile).exists();

        JsonNode root = JSON.readTree(Files.readString(provenanceFile));

        /* Predicate.RunDetails.Metadata */
        // Assert: timestamps are RFC3339 strings (NOT numeric epoch seconds)
        JsonNode startedOn = root.at("/predicate/runDetails/metadata/startedOn");
        JsonNode finishedOn = root.at("/predicate/runDetails/metadata/finishedOn");
        String invocationId = root.at("/predicate/runDetails/metadata/invocationId").asText();
        assertThat(startedOn.isTextual()).isTrue();
        assertThat(finishedOn.isTextual()).isTrue();
        assertThat(startedOn.asText()).matches(RFC3339_Z);
        assertThat(finishedOn.asText()).matches(RFC3339_Z);
        assertThat(invocationId).isNotBlank();

        /* Predicate.BuildDefinition.ExternalParameters */
        // Assert: commandLine got sanitized (no raw secrets)
        String emittedCommandLine = root.at("/predicate/buildDefinition/externalParameters/commandLine").asText();
        assertThat(emittedCommandLine).doesNotContain("supersecret");
        assertThat(emittedCommandLine).doesNotContain("abc123");
        assertThat(emittedCommandLine).doesNotContain("topsecret");
        assertThat(emittedCommandLine).doesNotContain("user:pass");
        assertThat(emittedCommandLine).contains("[REDACTED]"); // at least one replacement happened

        // Assert: allowlisted fields are NOT redacted
        String version = root.at("/predicate/buildDefinition/externalParameters/pigConfiguration/version").asText();
        assertThat(version).isEqualTo("7.1.0");
        assertThat(version).isNotEqualTo("[REDACTED]");
        String scmRevision = root
                .at("/predicate/buildDefinition/externalParameters/pigConfiguration/builds/0/scmRevision")
                .asText();
        assertThat(scmRevision).isEqualTo("7.1.x");
        assertThat(scmRevision).isNotEqualTo("[REDACTED]");

        /* Predicate.BuildDefinition.InternalParameters */
        String deliverableType = root.at("/predicate/buildDefinition/internalParameters/deliverableType").asText();
        assertThat(deliverableType).isNotBlank();
        assertThat(deliverableType).isEqualTo("OTHER");
        String createdBy = root.at("/predicate/buildDefinition/internalParameters/createdBy").asText();
        assertThat(createdBy).isNotBlank();
        assertThat(createdBy).isEqualTo("addon:provenance");

        /* Predicate.BuildDefinition.ResolvedDependencies */
        String dep0Name = root.at("/predicate/buildDefinition/resolvedDependencies/0/name").asText();
        assertThat(dep0Name).isEqualTo("bacon");
        String dep0Uri = root.at("/predicate/buildDefinition/resolvedDependencies/0/uri").asText();
        assertThat(dep0Uri).isEqualTo("https://github.com/project-ncl/bacon");
        String buildConfigName = root.at("/predicate/buildDefinition/resolvedDependencies/1/name")
                .asText();
        assertThat(buildConfigName).isNotBlank();
        assertThat(buildConfigName).isNotEqualTo("[REDACTED]");
        String pigConfigDirName = root.at("/predicate/buildDefinition/resolvedDependencies/2/name")
                .asText();
        assertThat(pigConfigDirName).isNotBlank();
        assertThat(pigConfigDirName).isNotEqualTo("[REDACTED]");

        // Assert: subject digest exists
        String sha256 = root.at("/subject/0/digest/sha256").asText();
        assertThat(sha256).matches("^[a-f0-9]{64}$");
    }
}
