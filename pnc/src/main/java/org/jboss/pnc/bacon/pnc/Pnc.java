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
package org.jboss.pnc.bacon.pnc;

import picocli.CommandLine.Command;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/13/18
 */
@Command(
        name = "pnc",
        description = "PNC sub-command",
        subcommands = {
                AdminCli.class,
                ArtifactCli.class,
                BrewPushCli.class,
                BuildCli.class,
                BuildConfigCli.class,
                EnvironmentCli.class,
                DependencyAnalysisCli.class,
                GroupBuildCli.class,
                GroupConfigCli.class,
                ProductCli.class,
                ProductMilestoneCli.class,
                ProductReleaseCli.class,
                ProductVersionCli.class,
                ProjectCli.class,
                ScmRepositoryCli.class,
                WhoAmICli.class })
public class Pnc {
}
