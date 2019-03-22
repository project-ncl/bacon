package org.jboss.pnc.bacon.pnc;

import lombok.extern.slf4j.Slf4j;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.dto.Artifact;

@Slf4j
@GroupCommandDefinition(
        name = "artifact",
        description = "Artifact",
        groupCommands = {
                ArtifactCli.Get.class,
                ArtifactCli.ListFromHash.class
        })
public class ArtifactCli extends AbstractCommand {

    private static ArtifactClient client = new ArtifactClient(PncClientHelper.getPncConfiguration());

    @CommandDefinition(name = "get", description = "Get artifact")
    public class Get extends AbstractGetSpecificCommand<Artifact> {

        @Override
        public Artifact getSpecific(int id) throws ClientException {
            return client.getSpecific(id);
        }
    }


    @CommandDefinition(name = "list-from-hash", description = "List artifacts based on hash")
    public class ListFromHash extends AbstractCommand {

        @Option
        private String md5;

        @Option
        private String sha1;

        @Option
        private String sha256;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            return super.executeHelper(commandInvocation, () -> {

                if (md5 == null && sha1 == null && sha256 == null) {
                    log.error("You need to use at least one hash option!");
                } else {
                    for (Artifact artifact : client.getAll(sha256, md5, sha1)) {
                        // TODO: print in yaml or json
                        System.out.println(artifact);
                    }
                }

            });
        }
    }
}
