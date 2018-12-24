package org.jboss.pnc.bacon.pig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public class Build {

    private final BuildDependency buildDependency;

    @Inject
    public Build(BuildDependency buildDependency) {
        this.buildDependency = buildDependency;
    }

    // main entry point of the build process
    public String execute(Input input) {
        System.out.println("Execute build for group: " + input.getBuildGroup());
        final String doSomethingResult = buildDependency.doSomething(input.getConfigLocation());
        System.out.println(doSomethingResult);
        return doSomethingResult;
    }

    // main input that the build step needs
    // should be populated by the command line config and whatever other configuration is applicable
    public static class Input {

        private final String configLocation;

        private final Integer buildGroup;

        public Input(String configLocation, Integer buildGroup) {
            Objects.requireNonNull(configLocation);
            this.configLocation = configLocation;
            this.buildGroup = buildGroup;
        }

        public String getConfigLocation() {
            return configLocation;
        }

        public Integer getBuildGroup() {
            return buildGroup;
        }
    }
}
