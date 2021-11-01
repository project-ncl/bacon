package org.jboss.pnc.bacon.pig;

import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.mapping.BuildConfigMapping;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.dto.BuildConfiguration;
import picocli.CommandLine;

@CommandLine.Command(
        name = "export",
        description = "Export feature",
        subcommands = { PigExport.BuildConfigExport.class })
public class PigExport {

    @CommandLine.Command(
            name = "build-config",
            description = "Export a build-config in a format that can be injected into PiG's build-config.yaml")
    public static class BuildConfigExport extends AbstractGetSpecificCommand<BuildConfig> {

        private static final ClientCreator<BuildConfigurationClient> CREATOR = new ClientCreator<>(
                BuildConfigurationClient::new);

        @Override
        public BuildConfig getSpecific(String id) throws ClientException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                BuildConfiguration bc = client.getSpecific(id);
                return BuildConfigMapping.toBuildConfig(bc);
            }
        }
    }
}
