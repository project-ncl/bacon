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

import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.GenericSettingClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
        name = "maintenance-mode",
        description = "Maintenance mode related tasks",
        subcommands = {
                MaintenanceModeCli.ActivateMaintenanceMode.class,
                MaintenanceModeCli.DeactivateMaintenanceMode.class,
                MaintenanceModeCli.StatusMaintenanceMode.class })
public class MaintenanceModeCli {

    private static final ClientCreator<GenericSettingClient> CREATOR = new ClientCreator<>(GenericSettingClient::new);

    @Command(name = "activate", description = "This will disable any new builds from being accepted")
    public static class ActivateMaintenanceMode implements Callable<Integer> {

        @Parameters(description = "Reason")
        private String reason;

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                client.activateMaintenanceMode(reason);
                return 0;
            }
        }

        // TODO: @Override
        public String exampleText() {
            return "$ bacon pnc admin maintenance-mode activate \"Switching to maintenance mode for upcoming migration\"";
        }

    }

    @Command(name = "deactivate", description = "Deactivate maintenance mode and accept new builds")
    public static class DeactivateMaintenanceMode implements Callable<Integer> {

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClientAuthenticated()) {
                client.deactivateMaintenanceMode();
                return 0;
            }
        }
    }

    @Command(name = "status", description = "Know whether we are in maintenance mode or not")
    public static class StatusMaintenanceMode implements Callable<Integer> {
        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            try (GenericSettingClient client = CREATOR.newClient()) {
                if (client.isInMaintenanceMode()) {
                    System.out.println("PNC is in maintenance mode");
                    System.out.println(client.getAnnouncementBanner().getBanner());
                } else {
                    System.out.println("PNC is NOT in maintenance mode");
                }
                return 0;
            }
        }
    }
}
