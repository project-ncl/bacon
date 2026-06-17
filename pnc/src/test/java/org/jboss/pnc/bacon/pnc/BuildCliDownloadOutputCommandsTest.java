package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class BuildCliDownloadOutputCommandsTest {

    @Test
    void shouldRegisterBuildOutputCommands() {
        CommandLine buildCommand = new CommandLine(new BuildCli());

        assertTrue(buildCommand.getSubcommands().containsKey("download-built-artifacts"));
        assertTrue(buildCommand.getSubcommands().containsKey("get-provenance"));
        assertTrue(buildCommand.getSubcommands().containsKey("download-build-outputs"));
    }

    @Test
    void shouldParseDownloadBuiltArtifactsCommand() throws Exception {
        CommandLine buildCommand = new CommandLine(new BuildCli());

        CommandLine.ParseResult parseResult = buildCommand.parseArgs(
                "download-built-artifacts",
                "B123",
                "--output-dir",
                "/tmp/out");

        Object command = parseResult.subcommand().commandSpec().userObject();
        assertInstanceOf(BuildCli.DownloadBuiltArtifacts.class, command);
        assertEquals("B123", getField(command, "buildId"));
        assertEquals(Path.of("/tmp/out"), getField(command, "outputDir"));
    }

    @Test
    void shouldParseGetProvenanceCommand() throws Exception {
        CommandLine buildCommand = new CommandLine(new BuildCli());

        CommandLine.ParseResult parseResult = buildCommand.parseArgs(
                "get-provenance",
                "abc123",
                "--output",
                "/tmp/provenance.json");

        Object command = parseResult.subcommand().commandSpec().userObject();
        assertInstanceOf(BuildCli.GetProvenance.class, command);
        assertEquals("abc123", getField(command, "sha256"));
        assertEquals(Path.of("/tmp/provenance.json"), getField(command, "output"));
    }

    @Test
    void shouldParseDownloadAllOutputCommand() throws Exception {
        CommandLine buildCommand = new CommandLine(new BuildCli());

        CommandLine.ParseResult parseResult = buildCommand.parseArgs(
                "download-build-outputs",
                "B123",
                "--output-dir",
                "/tmp/out");

        Object command = parseResult.subcommand().commandSpec().userObject();
        assertInstanceOf(BuildCli.DownloadAllOutput.class, command);
        assertEquals("B123", getField(command, "buildId"));
        assertEquals(Path.of("/tmp/out"), getField(command, "outputDir"));
    }

    @Test
    void shouldKeepExistingDownloadSourcesCommandRegistered() throws Exception {
        CommandLine buildCommand = new CommandLine(new BuildCli());

        CommandLine.ParseResult parseResult = buildCommand.parseArgs("download-sources", "B123");

        Object command = parseResult.subcommand().commandSpec().userObject();
        assertInstanceOf(BuildCli.DownloadSources.class, command);
        assertEquals("B123", getField(command, "id"));
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
