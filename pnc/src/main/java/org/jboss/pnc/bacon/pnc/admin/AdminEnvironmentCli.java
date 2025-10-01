package org.jboss.pnc.bacon.pnc.admin;

import java.util.Map;
import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.dto.requests.EnvironmentDeprecationRequest;
import org.jboss.pnc.enums.SystemImageType;

import picocli.CommandLine;

/**
 * Differs from the {@link org.jboss.pnc.bacon.pnc.EnvironmentCli} in a way that these commands can be used only by
 * admins, not by regular users.
 */
@CommandLine.Command(
        name = "environment",
        description = "environment",
        subcommands = {
                AdminEnvironmentCli.Create.class,
                AdminEnvironmentCli.Deprecate.class })
public class AdminEnvironmentCli {

    private static final ClientCreator<EnvironmentClient> CREATOR = new ClientCreator<>(EnvironmentClient::new);

    @CommandLine.Command(
            name = "create",
            description = "Create a new environment",
            footer = Constant.EXAMPLE_TEXT
                    + "$ bacon pnc admin environment create --name=\"OpenJDK 21.0.2+13; Mx 7.4.1; Mvn 3.9.6\" --description \"OpenJDK 21.0.2+13; Mx 7.4.1; Mvn 3.9.6 [builder-rhel-8-j21.0.2-13-mx7.4.1-mvn3.9.6:1.0.42]\" --systemImageRepoUrl=quay.io/rh-newcastle --systemImageId=builder-rhel-8-j21.0.2-13-mx7.4.1-mvn3.9.6:1.0.42 --systemImageType=DOCKER_IMAGE --attributes=JDK=21.0.2-13 --attributes=MAVEN=3.9.6 --attributes=OS=Linux --attributes=Mx=7.4.1")
    public static class Create extends JSONCommandHandler implements Callable<Integer> {

        @CommandLine.Option(required = true, names = "--name", description = "Name of the environment.")
        private String name;

        @CommandLine.Option(required = true, names = "--description", description = "Description of the environment.")
        private String description;

        @CommandLine.Option(
                required = true,
                names = "--systemImageRepoUrl",
                description = "System image repository URL of the environment.")
        private String systemImageRepoUrl;

        @CommandLine.Option(
                required = true,
                names = "--systemImageId",
                description = "System image id of the environment.")
        private String systemImageId;

        @CommandLine.Option(required = true, names = "--attributes", description = "Attributes of the environment.")
        private Map<String, String> attributes;

        @CommandLine.Option(
                required = true,
                names = "--systemImageType",
                description = "System image type of the environment.")
        private SystemImageType systemImageType;

        @CommandLine.Option(
                required = false,
                defaultValue = "false",
                names = "--deprecated",
                description = "Boolean flag whether the environment should be marked as deprecated.")
        private boolean deprecated;

        @CommandLine.Option(
                required = false,
                defaultValue = "false",
                names = "--hidden",
                description = "Boolean flag whether the environment should be marked as hidden.")
        private boolean hidden;

        @Override
        public Integer call() throws Exception {
            Environment environment = Environment.builder()
                    .name(name)
                    .description(description)
                    .systemImageRepositoryUrl(systemImageRepoUrl)
                    .systemImageId(systemImageId)
                    .attributes(attributes)
                    .systemImageType(systemImageType)
                    .deprecated(deprecated)
                    .hidden(hidden)
                    .build();

            try (EnvironmentClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(getJsonOutput(), client.createNew(environment));
                return 0;
            }
        }
    }

    @CommandLine.Command(
            name = "deprecate",
            description = "Deprecate an environment",
            footer = Constant.EXAMPLE_TEXT + "$ bacon pnc admin environment deprecate --replacementEnvironmentId=42 41")
    public static class Deprecate implements Callable<Integer> {

        @CommandLine.Parameters(description = "ID of the environment, which should be deprecated.")
        private String id;

        @CommandLine.Option(
                required = true,
                names = "--replacementEnvironmentId",
                description = "ID of the environment, which replaces environment specified by the parameter.")
        private String replacementId;

        @Override
        public Integer call() throws Exception {
            EnvironmentDeprecationRequest deprecationRequest = EnvironmentDeprecationRequest.builder()
                    .replacementEnvironmentId(replacementId)
                    .build();

            try (EnvironmentClient client = CREATOR.newClientAuthenticated()) {
                client.deprecate(id, deprecationRequest);
                return 0;
            }
        }
    }
}
