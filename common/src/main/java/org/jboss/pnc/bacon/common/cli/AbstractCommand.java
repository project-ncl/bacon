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
 * Provides a default implementation of 'execute' to print the help usage if the '-h' option is used.
 *
 * Also provides 'executeHelper', which accepts a lambda to print the help usate if the '-h' option is provided, or
 * run the lambda otherwise
 */
@Slf4j
public class AbstractCommand implements Command {

    public interface SubCommandExecuteInterface {
        void call() throws ClientException;
    }

    @Option(shortName = 'h', overrideRequired = true, hasValue = false, description = "print help")
    private boolean help = false;

    @Option(shortName = 'V', overrideRequired = true, hasValue = false, description = "print version")
    private boolean version = false;


    private static final String VERSION_OUTPUT = Constants.VERSION + " (" + Constants.COMMIT_ID_SHA + ")";


    public boolean printHelpOrVersionIfPresent(CommandInvocation commandInvocation) {

        Shell shell = commandInvocation.getShell();

        boolean activated = false;

        if (help) {
            shell.writeln(commandInvocation.getHelpInfo());
            activated = true;
        }

        if (version) {
            shell.writeln(VERSION_OUTPUT);
            activated = true;
        }

        return activated;
    }


    /**
     * Print help method if a group command is invoked
     * @param commandInvocation
     * @return
     */
    private boolean printHelpIfNoFinalCommandSelected(CommandInvocation commandInvocation) {
        GroupCommandDefinition groupCommandDefinition = this.getClass().getAnnotation(GroupCommandDefinition.class);


        if (groupCommandDefinition == null) {
            return false;
        } else {

            Shell shell = commandInvocation.getShell();
            shell.writeln(commandInvocation.getHelpInfo());
            return true;
        }
    }

    /**
     * Default implementation of execute method. For now it just tries to figure out if the user used the '-h' option
     * and print the help usage if yes.
     *
     * If the user used the '-h' option, it'll return CommandResult.SUCCESS, else CommandResult.FAILURE
     *
     * @param commandInvocation
     * @return
     * @throws CommandException
     * @throws InterruptedException
     */
    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        return executePrivate(commandInvocation);
    }

    /**
     * executePrivate is present so that executeHelper can reference to this implementation instead of the
     * overridden execute method in a sub-class
     *
     * @param commandInvocation
     * @return
     * @throws CommandException
     * @throws InterruptedException
     */
    private CommandResult executePrivate(CommandInvocation commandInvocation) {
        boolean helpNoFinalCommandPrinted = false;
        boolean helpOrVersionPrinted = printHelpOrVersionIfPresent(commandInvocation);

        if (!helpOrVersionPrinted) {
            helpNoFinalCommandPrinted = printHelpIfNoFinalCommandSelected(commandInvocation);
        }

        if (helpOrVersionPrinted || helpNoFinalCommandPrinted) {
            return CommandResult.SUCCESS;
        } else {
            return CommandResult.FAILURE;
        }
    }

    public CommandResult executeHelper(CommandInvocation commandInvocation, SubCommandExecuteInterface callback)
            throws CommandException, InterruptedException {

        CommandResult result = executePrivate(commandInvocation);

        if (result == CommandResult.SUCCESS) {
            // user used the -h option
            return CommandResult.SUCCESS;
        }

        try {
            callback.call();
        } catch (Exception e) {
            log.error("Something wrong happened", e);
            return CommandResult.FAILURE;
        }
        return CommandResult.SUCCESS;
    }
}