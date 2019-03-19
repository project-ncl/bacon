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
package org.jboss.pnc.bacon.common.cli;

import lombok.extern.slf4j.Slf4j;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;

/**
 * Class used to provide a default implementation for List* type commands.
 *
 * The subclass only needs to implement the 'getAll' method to teach it how to retrieve all the contents
 * @param <T>
 */
@Slf4j
public abstract class AbstractListCommand<T> extends AbstractCommand {

    @Option(description = "Sort order (using RSQL)")
    private String sort;

    @Option(description = "Query parameter (using RSQL)")
    private String query;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        return super.executeHelper(commandInvocation, () -> {

                for (T item : getAll(sort, query)) {
                    // TODO: to print in JSON or YAML format
                    System.out.println(item);
                }
        });
    }

    public abstract RemoteCollection<T> getAll(String sort, String query) throws RemoteResourceException;
}
