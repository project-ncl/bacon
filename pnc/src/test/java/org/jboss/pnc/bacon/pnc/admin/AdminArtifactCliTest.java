package org.jboss.pnc.bacon.pnc.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.jboss.pnc.bacon.pnc.AdminCli;
import org.jboss.pnc.bacon.pnc.ArtifactCli;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class AdminArtifactCliTest {

    @Test
    void shouldRegisterArtifactBlacklistUnderAdminOnly() {
        CommandLine adminCommand = new CommandLine(new AdminCli());
        CommandLine adminArtifactCommand = adminCommand.getSubcommands().get("artifact");

        assertTrue(adminCommand.getSubcommands().containsKey("artifact"));
        assertTrue(adminArtifactCommand.getSubcommands().containsKey("blacklist"));
        assertFalse(new CommandLine(new ArtifactCli()).getSubcommands().containsKey("blacklist"));
    }

    @Test
    void shouldParseBlacklistReasonAliasAndArtifactIds() throws Exception {
        CommandLine adminCommand = new CommandLine(new AdminCli());

        CommandLine.ParseResult parseResult = adminCommand
                .parseArgs("artifact", "blacklist", "--reason", "Lightwell rebuild", "123", "456");

        Object blacklistCommand = parseResult.subcommand().subcommand().commandSpec().userObject();
        assertInstanceOf(AdminArtifactCli.Blacklist.class, blacklistCommand);
        assertEquals("Lightwell rebuild", getField(blacklistCommand, "description"));
        assertEquals(List.of("123", "456"), getField(blacklistCommand, "artifactIds"));
    }

    @Test
    void shouldParseBlacklistDescriptionAlias() throws Exception {
        CommandLine adminCommand = new CommandLine(new AdminCli());

        CommandLine.ParseResult parseResult = adminCommand
                .parseArgs("artifact", "blacklist", "--description", "Lightwell rebuild", "123");

        Object blacklistCommand = parseResult.subcommand().subcommand().commandSpec().userObject();
        assertInstanceOf(AdminArtifactCli.Blacklist.class, blacklistCommand);
        assertEquals("Lightwell rebuild", getField(blacklistCommand, "description"));
        assertEquals(List.of("123"), getField(blacklistCommand, "artifactIds"));
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
