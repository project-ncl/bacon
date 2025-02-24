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

import java.util.Collection;
import java.util.Optional;

import org.jboss.pnc.bacon.common.cli.AbstractGetSpecificCommand;
import org.jboss.pnc.bacon.common.cli.AbstractListCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.EnvironmentClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Environment;

import picocli.CommandLine.Command;

@Command(
        name = "environment",
        description = "environment",
        subcommands = { EnvironmentCli.Get.class, EnvironmentCli.List.class })
public class EnvironmentCli {

    private static final ClientCreator<EnvironmentClient> CREATOR = new ClientCreator<>(EnvironmentClient::new);

    @Command(name = "get", description = "Get an environment by its id")
    public static class Get extends AbstractGetSpecificCommand<Environment> {

        @Override
        public Environment getSpecific(String id) throws ClientException {
            try (EnvironmentClient client = CREATOR.newClient()) {
                return client.getSpecific(id);
            }
        }
    }

    @Command(name = "list", description = "List environments")
    public static class List extends AbstractListCommand<Environment> {

        @Override
        public Collection<Environment> getAll(String sort, String query) throws RemoteResourceException {
            try (EnvironmentClient client = CREATOR.newClient()) {
                return client.getAll(Optional.ofNullable(sort), Optional.ofNullable(query)).getAll();
            }
        }
    }

}
