package org.jboss.pnc.bacon.pig.impl.addons.provenance;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.pnc.api.slsa.dto.provenance.v1.BuildDefinition;
import org.jboss.pnc.api.slsa.dto.provenance.v1.Provenance;
import org.jboss.pnc.api.slsa.dto.provenance.v1.ResourceDescriptor;
import org.jboss.pnc.bacon.common.deliverables.DeliverableRecord;
import org.jboss.pnc.bacon.common.deliverables.DeliverableType;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for SlsaProvenanceV1Utils to verify provenance generation including git information.
 */
class SlsaProvenanceV1UtilsTest {

    private PigContext mockContext;
    private InvocationInfo mockInvocation;
    private DeliverableRecord mockDeliverable;

    @BeforeEach
    void setUp() {
        // Create a minimal PigContext for testing
        mockContext = new PigContext();

        // Set up basic configuration
        PigConfiguration config = new PigConfiguration();
        config.setVersion("1.0.0");

        // Initialize context with test values
        Path testPath = Paths.get("target", "test");
        mockContext.setPigConfiguration(config);
        mockContext.setTargetPath(testPath.toString());
        mockContext.setReleasePath(testPath.resolve("release").toString());
        mockContext.setExtrasPath(testPath.resolve("extras").toString());
        mockContext.setPrefix("test-prefix");
        mockContext.setFullVersion("1.0.0.Final");
        mockContext.setReleaseDirName("test-release");

        // Create mock invocation info
        Map<String, String> configDigests = new HashMap<>();
        configDigests.put("build-config.preprocessed.sha256", "abc123def456");

        mockInvocation = new InvocationInfo(
                "0.1.0-test",
                "bacon pig build",
                "test-invocation-id",
                Instant.now(),
                Instant.now(),
                configDigests);

        // Create mock deliverable
        Map<String, String> digests = new HashMap<>();
        digests.put("sha256", "deliverable123abc");

        mockDeliverable = DeliverableRecord.create(
                DeliverableType.MAVEN_REPO_ZIP,
                Paths.get("test-deliverable.zip"),
                "test-creator",
                Map.of());
        mockDeliverable.setDigests(digests);
    }

    @Test
    void testToProvenance_CreatesValidProvenance() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        assertNotNull(provenance, "Provenance should not be null");
        assertEquals("https://in-toto.io/Statement/v1", provenance.getType());
        assertEquals("https://slsa.dev/provenance/v1", provenance.getPredicateType());
        assertNotNull(provenance.getPredicate(), "Predicate should not be null");
        assertNotNull(provenance.getSubject(), "Subject should not be null");
        assertFalse(provenance.getSubject().isEmpty(), "Subject should not be empty");
    }

    @Test
    void testToProvenance_ContainsBuildDefinition() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        BuildDefinition buildDef = provenance.getPredicate().getBuildDefinition();
        assertNotNull(buildDef, "BuildDefinition should not be null");
        assertEquals("https://project-ncl.github.io/slsa-pnc-cli-buildtypes/workflow/v1", buildDef.getBuildType());
        assertNotNull(buildDef.getExternalParameters(), "External parameters should not be null");
        assertNotNull(buildDef.getInternalParameters(), "Internal parameters should not be null");
        assertNotNull(buildDef.getResolvedDependencies(), "Resolved dependencies should not be null");
    }

    @Test
    void testToProvenance_ContainsBaconDependency() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        List<ResourceDescriptor> deps = provenance.getPredicate().getBuildDefinition().getResolvedDependencies();

        Optional<ResourceDescriptor> baconDep = deps.stream()
                .filter(d -> "bacon".equals(d.getName()))
                .findFirst();

        assertTrue(baconDep.isPresent(), "Should contain bacon dependency");
        assertEquals("https://github.com/project-ncl/bacon", baconDep.get().getUri());
        assertNotNull(baconDep.get().getAnnotations(), "Bacon dependency should have annotations");
    }

    @Test
    void testToProvenance_ContainsPreprocessedConfigDependency() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        List<ResourceDescriptor> deps = provenance.getPredicate().getBuildDefinition().getResolvedDependencies();

        Optional<ResourceDescriptor> configDep = deps.stream()
                .filter(d -> d.getName() != null && d.getName().contains("build-config.yaml"))
                .findFirst();

        assertTrue(configDep.isPresent(), "Should contain preprocessed config dependency");
        assertNotNull(configDep.get().getDigest(), "Config dependency should have digest");
        assertTrue(configDep.get().getDigest().containsKey("sha256"), "Config should have sha256 digest");
    }

    @Test
    void testToProvenance_MayContainGitRepositoryInfo() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        List<ResourceDescriptor> deps = provenance.getPredicate().getBuildDefinition().getResolvedDependencies();

        // Check if git repository info is present (it may or may not be, depending on test environment)
        Optional<ResourceDescriptor> gitDep = deps.stream()
                .filter(d -> "repository".equals(d.getName()))
                .findFirst();

        // If present, verify its structure
        gitDep.ifPresent(dep -> {
            assertNotNull(dep.getUri(), "Git repository should have URI");
            assertNotNull(dep.getDigest(), "Git repository should have digest");
            assertTrue(dep.getDigest().containsKey("gitCommit"), "Git repository should have gitCommit in digest");

            String commitHash = dep.getDigest().get("gitCommit");
            assertNotNull(commitHash, "Commit hash should not be null");
            assertTrue(
                    commitHash.matches("[0-9a-f]{40}"),
                    "Commit hash should be a valid SHA-1 hash (40 hex chars)");

            String repoUrl = dep.getUri();
            assertTrue(
                    repoUrl.startsWith("http://") ||
                            repoUrl.startsWith("https://") ||
                            repoUrl.startsWith("git@") ||
                            repoUrl.startsWith("file://") ||
                            repoUrl.startsWith("/"),
                    "Repository URL should be a valid git URL format");
        });
    }

    @Test
    void testToProvenance_ContainsRunDetails() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        assertNotNull(provenance.getPredicate().getRunDetails(), "RunDetails should not be null");
        assertNotNull(provenance.getPredicate().getRunDetails().getBuilder(), "Builder should not be null");
        assertNotNull(provenance.getPredicate().getRunDetails().getMetadata(), "Metadata should not be null");

        // Verify builder ID format
        String builderId = provenance.getPredicate().getRunDetails().getBuilder().getId();
        assertTrue(builderId.startsWith("urn:uuid:"), "Builder ID should start with urn:uuid:");

        // Verify metadata
        assertEquals("test-invocation-id", provenance.getPredicate().getRunDetails().getMetadata().getInvocationId());
        assertNotNull(provenance.getPredicate().getRunDetails().getMetadata().getStartedOn());
        assertNotNull(provenance.getPredicate().getRunDetails().getMetadata().getFinishedOn());
    }

    @Test
    void testToProvenance_ContainsSubjectWithCorrectDigest() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        List<ResourceDescriptor> subjects = provenance.getSubject();
        assertEquals(1, subjects.size(), "Should have exactly one subject");

        ResourceDescriptor subject = subjects.get(0);
        assertEquals("test-deliverable.zip", subject.getName());
        assertNotNull(subject.getDigest(), "Subject should have digest");
        assertEquals("deliverable123abc", subject.getDigest().get("sha256"));
    }

    @Test
    void testToProvenance_ExternalParametersContainExpectedFields() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        Map<String, Object> externalParams = provenance.getPredicate().getBuildDefinition().getExternalParameters();

        assertTrue(externalParams.containsKey("commandLine"), "Should contain commandLine");
        assertTrue(externalParams.containsKey("prefix"), "Should contain prefix");
        assertTrue(externalParams.containsKey("fullVersion"), "Should contain fullVersion");
        assertTrue(externalParams.containsKey("tempBuild"), "Should contain tempBuild");
        assertTrue(externalParams.containsKey("targetPath"), "Should contain targetPath");
        assertTrue(externalParams.containsKey("releaseDirName"), "Should contain releaseDirName");

        assertEquals("bacon pig build", externalParams.get("commandLine"));
        assertEquals("test-prefix", externalParams.get("prefix"));
        assertEquals("1.0.0.Final", externalParams.get("fullVersion"));
    }

    @Test
    void testToProvenance_InternalParametersContainDeliverableInfo() {
        Provenance provenance = SlsaProvenanceV1Utils.toProvenance(mockContext, mockInvocation, mockDeliverable);

        Map<String, Object> internalParams = provenance.getPredicate().getBuildDefinition().getInternalParameters();

        assertTrue(internalParams.containsKey("deliverableType"), "Should contain deliverableType");
        assertTrue(internalParams.containsKey("createdBy"), "Should contain createdBy");
        assertTrue(internalParams.containsKey("createdAt"), "Should contain createdAt");

        assertEquals("MAVEN_REPO_ZIP", internalParams.get("deliverableType"));
        assertEquals("test-creator", internalParams.get("createdBy"));
    }

}
