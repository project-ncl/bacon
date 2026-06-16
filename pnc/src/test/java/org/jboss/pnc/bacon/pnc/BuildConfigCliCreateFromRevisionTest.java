package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class BuildConfigCliCreateFromRevisionTest {

    @Test
    void shouldRegisterCreateFromRevisionUnderBuildConfig() {
        CommandLine buildConfigCommand = new CommandLine(new BuildConfigCli());

        assertTrue(buildConfigCommand.getSubcommands().containsKey("create-from-revision"));
    }

    @Test
    void shouldParseCreateFromRevisionOptions() throws Exception {
        CommandLine buildConfigCommand = new CommandLine(new BuildConfigCli());

        CommandLine.ParseResult parseResult = buildConfigCommand.parseArgs(
                "create-from-revision",
                "--revision-file",
                "/tmp/source-bcr.json",
                "--buildConfigName",
                "source-lightwell-",
                "--description",
                "Lightwell copy",
                "--environment-id",
                "10",
                "--project-id",
                "20",
                "--scm-repository-id",
                "30",
                "--scm-revision",
                "abcdef",
                "--build-script",
                "mvn clean install",
                "--build-type",
                "MVN",
                "--product-version-id",
                "40",
                "-PALIGNMENT_PARAMETERS=-DdependencyOverride.*:*@*=",
                "--default-alignment-params",
                "-DskipAlignment",
                "--brew-pull-active=false");

        Object command = parseResult.subcommand().commandSpec().userObject();
        assertInstanceOf(BuildConfigCli.CreateFromRevision.class, command);
        assertEquals(Path.of("/tmp/source-bcr.json"), getField(command, "revisionFile"));
        assertEquals("source-lightwell-", getField(command, "buildConfigName"));
        assertEquals("Lightwell copy", getField(command, "description"));
        assertEquals("10", getField(command, "environmentId"));
        assertEquals("20", getField(command, "projectId"));
        assertEquals("30", getField(command, "scmRepositoryId"));
        assertEquals("abcdef", getField(command, "scmRevision"));
        assertEquals("mvn clean install", getField(command, "buildScript"));
        assertEquals("MVN", getField(command, "buildType"));
        assertEquals("40", getField(command, "productVersionId"));
        assertEquals("-DskipAlignment", getField(command, "defaultAlignmentParams"));
        assertEquals(Boolean.FALSE, getField(command, "brewPullActive"));
        assertEquals(
                Map.of("ALIGNMENT_PARAMETERS", "-DdependencyOverride.*:*@*="),
                getField(command, "parameters"));
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
