/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig;

import java.util.Objects;

public class Build {

    private final BuildDependency buildDependency;

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
