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
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.shell.Shell;
import org.jboss.pnc.bacon.common.Constants;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;

/**
 * Abstract command that implements Command
 *
 * Provides a default implementation of 'execute' for not implemented commands
 */
@Slf4j
public class AbstractNotImplementedCommand implements Command {

    /**
     * Default implementation of execute method. Just prints "Not implemented yet!"
     *
     *
     * @param commandInvocation
     * @return
     * @throws CommandException
     * @throws InterruptedException
     */
    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        log.error("Not implemented yet!");
        return CommandResult.FAILURE;
    }
}