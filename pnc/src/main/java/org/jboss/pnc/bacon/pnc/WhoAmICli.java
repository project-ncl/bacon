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
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.client.UserClient;

@CommandDefinition(name = "whoami", description = "Returns identity of current user")
public class WhoAmICli extends AbstractCommand {

    private static final ClientCreator<UserClient> CREATOR = new ClientCreator<>(UserClient::new);

    @Option(
            shortName = 'o',
            hasValue = false,
            description = "use json for output (default to yaml)")
    private boolean jsonOutput = false;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        return super.executeHelper(commandInvocation, () -> {
            try (UserClient client = CREATOR.newClientAuthenticated()) {
                ObjectHelper.print(jsonOutput, client.getCurrentUser());
                return 0;
            }
        });
    }
}
