package org.jboss.pnc.bacon.pig;

import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.mapping.BuildConfigMapping;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import picocli.CommandLine;

import java.util.Optional;

@CommandLine.Command(
        name = "export",
        description = "Export feature",
        subcommands = { PigExport.BuildConfigExport.class })
public class PigExport {

    @CommandLine.Command(
            name = "build-config",
            description = "Export a build-config in a format that can be injected into PiG's build-config.yaml")
    public static class BuildConfigExport extends AbstractGetSpecificCommand<BuildConfig> {

        @CommandLine.Option(
                names = "--revision",
                description = "Revision of the build config. By default the current revision is used.")
        private Integer revision;

        @CommandLine.Option(
                names = "--environment-by-name",
                description = "Export config with environment name instead of environment image id.")
        private boolean envByName = false;

        @CommandLine.Option(
                names = "--rename",
                description = "Use this name for the exported build config instead of the original one.")
        private String rename;

        private static final ClientCreator<BuildConfigurationClient> CREATOR = new ClientCreator<>(
                BuildConfigurationClient::new);

        @Override
        public BuildConfig getSpecific(String id) throws ClientException {
            try (BuildConfigurationClient client = CREATOR.newClient()) {
                BuildConfiguration bc = client.getSpecific(id);
                BuildConfigMapping.GeneratorOptions opts = BuildConfigMapping.GeneratorOptions.builder()
                        .useEnvironmentName(envByName)
                        .nameOverride(Optional.ofNullable(rename))
                        .build();
                if (revision != null) {
                    BuildConfigurationRevision bcr = client.getRevision(id, revision);
                    return BuildConfigMapping.toBuildConfig(bc, bcr, opts);
                } else {
                    return BuildConfigMapping.toBuildConfig(bc, opts);
                }
            }
        }
    }
}
