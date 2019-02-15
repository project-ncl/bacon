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

import org.jboss.pnc.bacon.common.SubCommandHelper;
import picocli.CommandLine;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@CommandLine.Command(name = "pnc", mixinStandardHelpOptions = true,
        description = "PNC sub-command",
        subcommands = {
                BrewPush.class,
                Build.class,
                BuildConfiguration.class,
                Environment.class,
                GenerateTools.class,
                GroupBuild.class,
                GroupBuildConfiguration.class,
                Product.class,
                ProductMilestone.class,
                ProductRelease.class,
                ProductVersion.class,
                Project.class,
                Repository.class
        })
public class Pnc extends SubCommandHelper {
}
