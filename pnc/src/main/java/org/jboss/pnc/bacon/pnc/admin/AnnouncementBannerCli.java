/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
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
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.GenericSettingClient;

@GroupCommandDefinition(
        name = "announcement-banner",
        description = "Announcement banner related tasks",
        groupCommands = {
                AnnouncementBannerCli.SetAnnouncementBanner.class,
                AnnouncementBannerCli.UnsetAnnouncementBanner.class,
                AnnouncementBannerCli.GetAnnouncementBanner.class })
public class AnnouncementBannerCli extends AbstractCommand {

    private static final ClientCreator<GenericSettingClient> CREATOR = new ClientCreator<>(GenericSettingClient::new);

    @CommandDefinition(name = "set", description = "This will set the announcement banner")
    public class SetAnnouncementBanner extends AbstractCommand {

        @Argument(required = true, description = "Announcement")
        private String announcement;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                    client.setAnnouncementBanner(announcement);
                    return 0;
                }
            });
        }

        @Override
        public String exampleText() {
            return "$ bacon pnc admin announcement-banner set \"Wash your hands!\"";
        }
    }

    @CommandDefinition(name = "unset", description = "This will unset the announcement banner")
    public class UnsetAnnouncementBanner extends AbstractCommand {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                    client.setAnnouncementBanner("");
                    return 0;
                }
            });
        }
    }

    @CommandDefinition(name = "get", description = "This will get the announcement banner, if any set")
    public class GetAnnouncementBanner extends AbstractCommand {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            return super.executeHelper(commandInvocation, () -> {
                try (GenericSettingClient client = CREATOR.newClient()) {
                    System.out.println(client.getAnnouncementBanner().getBanner());
                    return 0;
                }
            });
        }
    }
}
