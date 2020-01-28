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
package org.jboss.pnc.bacon.pnc.admin;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.GenericSettingClient;

@GroupCommandDefinition(name = "maintenance-mode", description = "Maintenance mode related tasks", groupCommands = {
        MaintenanceModeCli.ActivateMaintenanceMode.class, MaintenanceModeCli.DeactivateMaintenanceMode.class,
        MaintenanceModeCli.StatusMaintenanceMode.class })
public class MaintenanceModeCli extends AbstractCommand {

    private static GenericSettingClient clientCache;

    private static GenericSettingClient getClient() {
        if (clientCache == null) {
            clientCache = new GenericSettingClient(PncClientHelper.getPncConfiguration(false));
        }
        return clientCache;
    }

    private static GenericSettingClient getClientAuthenticated() {
        if (clientCache == null) {
            clientCache = new GenericSettingClient(PncClientHelper.getPncConfiguration(true));
        }
        return clientCache;
    }

    @CommandDefinition(name = "activate", description = "This will disable any new builds from being accepted")
    public class ActivateMaintenanceMode extends AbstractCommand {

        @Argument(required = true, description = "Reason")
        private String reason;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                getClientAuthenticated().activateMaintenanceMode(reason);

            });
        }
    }

    @CommandDefinition(name = "deactivate", description = "Deactivate maintenance mode and accept new builds")
    public class DeactivateMaintenanceMode extends AbstractCommand {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                getClientAuthenticated().deactivateMaintenanceMode();
                ;

            });
        }
    }

    @CommandDefinition(name = "status", description = "Know whether we are in maintenance mode or not")
    public class StatusMaintenanceMode extends AbstractCommand {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                if (getClient().isInMaintenanceMode()) {
                    System.out.println("PNC is in maintenance mode");
                    System.out.println(getClient().getAnnouncementBanner().getBanner());
                } else {
                    System.out.println("PNC is NOT in maintenance mode");
                }

            });
        }
    }
}
