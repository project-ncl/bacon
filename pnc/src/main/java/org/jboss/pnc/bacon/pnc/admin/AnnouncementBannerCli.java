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

import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.GenericSettingClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
        name = "announcement-banner",
        description = "Announcement banner related tasks",
        subcommands = {
                AnnouncementBannerCli.SetAnnouncementBanner.class,
                AnnouncementBannerCli.UnsetAnnouncementBanner.class,
                AnnouncementBannerCli.GetAnnouncementBanner.class })
public class AnnouncementBannerCli {

    private static final ClientCreator<GenericSettingClient> CREATOR = new ClientCreator<>(GenericSettingClient::new);

    @Command(
            name = "set",
            description = "This will set the announcement banner",
            footer = "$ bacon pnc admin announcement-banner set \"Wash your hands!")
    public static class SetAnnouncementBanner implements Callable<Integer> {
        @Parameters(description = "Announcement")
        private String announcement;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                client.setAnnouncementBanner(announcement);
                return 0;
            }
        }
    }

    @Command(name = "unset", description = "This will unset the announcement banner")
    public static class UnsetAnnouncementBanner implements Callable<Integer> {
        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                client.setAnnouncementBanner("");
                return 0;
            }
        }
    }

    @Command(name = "get", description = "This will get the announcement banner, if any set")
    public static class GetAnnouncementBanner implements Callable<Integer> {
        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClient()) {
                System.out.println(client.getAnnouncementBanner().getBanner());
                return 0;
            }
        }
    }
}
