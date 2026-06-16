package org.jboss.pnc.bacon.pnc.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.dto.ArtifactRevision;
import org.jboss.pnc.enums.ArtifactQuality;

import picocli.CommandLine;

/**
 * Admin-only artifact operations.
 */
@CommandLine.Command(
        name = "artifact",
        description = "Admin artifact operations",
        subcommands = {
                AdminArtifactCli.Blacklist.class
        })
public class AdminArtifactCli {

    private static final ClientCreator<ArtifactClient> CREATOR = new ClientCreator<>(ArtifactClient::new);

    @CommandLine.Command(
            name = "blacklist",
            description = "Set artifact quality to BLACKLISTED",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc admin artifact blacklist --description \"Lightwell rebuild\" 123 456")
    public static class Blacklist extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Parameters(description = "PNC artifact ID(s) to blacklist")
        private List<String> artifactIds;

        @CommandLine.Option(
                names = { "--description", "--reason" },
                required = true,
                description = "Reason stored on the artifact quality revision")
        private String description;

        @Override
        public Integer call() throws Exception {
            if (artifactIds == null || artifactIds.isEmpty()) {
                throw new FatalException("You need to specify at least one artifact ID to blacklist");
            }

            List<ArtifactRevision> revisions = new ArrayList<>();
            try (ArtifactClient client = CREATOR.newClientAuthenticated()) {
                for (String artifactId : artifactIds) {
                    revisions.add(
                            client.createQualityLevelRevision(
                                    artifactId,
                                    ArtifactQuality.BLACKLISTED.name(),
                                    description));
                }
            }

            ObjectHelper.print(getJsonOutput(), revisions);
            return 0;
        }
    }
}
