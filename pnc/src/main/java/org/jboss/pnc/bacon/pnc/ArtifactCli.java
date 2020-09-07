package org.jboss.pnc.bacon.pnc;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.dto.Artifact;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Slf4j
@Command(
        name = "artifact",
        description = "Artifact",
        subcommands = { ArtifactCli.Get.class, ArtifactCli.ListFromHash.class })
public class ArtifactCli {

    private static final ClientCreator<ArtifactClient> CREATOR = new ClientCreator<>(ArtifactClient::new);

    @Command(
            name = "get",
            description = "Get an artifact by its id",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact get 10")
    public static class Get extends AbstractGetSpecificCommand<Artifact> {

        @Override
        public Artifact getSpecific(String id) throws ClientException {
            try (ArtifactClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(
            name = "list-from-hash",
            description = "List artifacts based on hash",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc artifact list-from-hash --md5 stiritup")
    public static class ListFromHash extends JSONCommandHandler implements Callable<Integer> {
        @Option(names = "--md5")
        private String md5;

        @Option(names = "--sha1")
        private String sha1;

        @Option(names = "--sha256")
        private String sha256;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            if (md5 == null && sha1 == null && sha256 == null) {
                log.error("You need to use at least one hash option!");
                return 1;
            } else {
                try (ArtifactClient client = CREATOR.newClient()) {
                    ObjectHelper.print(getJsonOutput(), client.getAll(sha256, md5, sha1));
                    return 0;
                }
            }
        }
    }
}
