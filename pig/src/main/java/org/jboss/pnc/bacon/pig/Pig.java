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

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.SubCommandHelper;
import org.jboss.pnc.bacon.common.exception.TodoException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@GroupCommandDefinition(
        name = "pig",
        description = "PfG tool",
        groupCommands = {
                Pig.Configure.class,
                Pig.Build.class,
                Pig.GenerateRepository.class,
                Pig.GenerateLicenses.class,
                Pig.GenerateJavadocs.class,
                Pig.GenerateSources.class,
                Pig.GenerateSharedContentAnalysis.class,
                Pig.GenerateDocuments.class,
                Pig.GenerateScripts.class,
                Pig.TriggerAddOns.class
        })
public class Pig extends SubCommandHelper {

    @CommandDefinition(name = "configure", description = "Configure")
    public class Configure extends SubCommandHelper {

        @Option(shortName = 'c',
                overrideRequired = true,
                defaultValue = "build-config.yaml",
                description = "location of configuration file")
        private String config;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                System.out.println("configLocation: " + config);
                throw new TodoException();

            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build")
    public class Build extends SubCommandHelper {

        @Option(shortName = 'c',
                overrideRequired = true,
                defaultValue = "build-config.yaml",
                description = "location of configuration file")
        private String config;

        @Option(shortName = 'b',
                overrideRequired = true,
                description = "id of the group to build. Exactly one of {config, build-group} has to be provided")
        private Integer buildGroupId;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                System.out.println("configLocation: " + config);
                throw new TodoException();

            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "repo", description = "GenerateRepository")
    public class GenerateRepository extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "licenses", description = "GenerateLicenses")
    public class GenerateLicenses extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "javadocs", description = "GenerateJavadocs")
    public class GenerateJavadocs extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sources", description = "GenerateSources")
    public class GenerateSources extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "shared-content", description = "GenerateSharedContentAnalysis")
    public class GenerateSharedContentAnalysis extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "docs", description = "GenerateDocuments")
    public class GenerateDocuments extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "scripts", description = "GenerateScripts")
    public class GenerateScripts extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "addons", description = "Addons")
    public class TriggerAddOns extends SubCommandHelper {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            CommandResult result = super.execute(commandInvocation);

            if (result == CommandResult.FAILURE) {
                throw new TodoException();
            }
            return CommandResult.SUCCESS;
        }
    }
}
