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
package org.jboss.pnc.bacon.cli;

import org.jboss.pnc.bacon.pig.Build;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static org.jboss.pnc.bacon.CDILauncher.getEntrypointProvider;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@Command(name = "pig", mixinStandardHelpOptions = true)
public class Pig {
    @Command(name = "configure", mixinStandardHelpOptions = true)
    public void configure(
            @Option(names = {"-c", "--config"},
                    defaultValue = "build-config.yaml",
                    description = "location of the configuration file")
                    String configLocation) {
        System.out.println("configLocation: " + configLocation);
        throw new TodoException();
    }

    @Command(name = "build", mixinStandardHelpOptions = true)
    public void build(
            @Option(
                    names = {"-c", "--config"},
                    defaultValue = "build-config.yaml",
                    description = "location of the configuration file") String configLocation,
            @Option(
                    names = {"-b", "--build-group"},
                    description = "id of the group to build. Exactly one of {config, build-group} has to be provided") Integer buildGroupId) {


        getEntrypointProvider(Build.class).execute(new Build.Input(configLocation, buildGroupId));
    }

    @Command(name = "repo", mixinStandardHelpOptions = true)
    public void generateRepository() {
        throw new TodoException();
    }

    @Command(name = "licenses", mixinStandardHelpOptions = true)
    public void generateLicenses() {
        throw new TodoException();
    }

    @Command(name = "javadocs", mixinStandardHelpOptions = true)
    public void generateJavadoc() {
        throw new TodoException();
    }

    @Command(name = "sources", mixinStandardHelpOptions = true)
    public void generateSources() {
        throw new TodoException();
    }

    @Command(name = "shared-content", mixinStandardHelpOptions = true)
    public void generateSharedContentAnalysis() {
        throw new TodoException();
    }

    @Command(name = "docs", mixinStandardHelpOptions = true)
    public void generateDocuments() {
        throw new TodoException();
    }

    @Command(name = "addons", mixinStandardHelpOptions = true)
    public void triggerAddOns() {
        throw new TodoException();
    }

    @Command(name = "scripts", mixinStandardHelpOptions = true)
    public void generateScripts() {
        throw new TodoException();
    }
}
