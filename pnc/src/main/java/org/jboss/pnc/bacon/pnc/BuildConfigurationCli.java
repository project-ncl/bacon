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

import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommandDefinition;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;

@GroupCommandDefinition(
        name = "brew-config",
        description = "brew-config",
        groupCommands = {
                BuildConfigurationCli.Create.class,
                BuildConfigurationCli.Get.class,
                BuildConfigurationCli.List.class,
                BuildConfigurationCli.Update.class,
                BuildConfigurationCli.Delete.class
        })
public class BuildConfigurationCli extends AbstractCommand {

    @CommandDefinition(name = "create", description = "Create a build configuration")
    public class Create extends AbstractCommand {
    }

    @CommandDefinition(name = "get", description = "Get build configurations")
    public class Get extends AbstractCommand {
    }

    @CommandDefinition(name = "list", description = "List build configurations")
    public class List extends AbstractCommand {
    }

    @CommandDefinition(name = "update", description = "Update a build configuration")
    public class Update extends AbstractCommand {
    }

    @CommandDefinition(name = "delete", description = "Delete a build configuration")
    public class Delete extends AbstractCommand {
    }

}
