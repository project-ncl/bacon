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
package org.jboss.pnc.bacon.cli;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Execution;
import org.aesh.command.Executor;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.builder.CommandBuilder;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.parser.RequiredOptionException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.terminal.formatting.Color;
import org.aesh.readline.terminal.formatting.TerminalColor;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.jboss.bacon.da.Da;
import org.jboss.pnc.bacon.common.cli.AbstractCommand;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.pig.Pig;
import org.jboss.pnc.bacon.pnc.Pnc;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/13/18
 */

@Slf4j
@GroupCommandDefinition(name = "bacon", description = "Bacon CLI", groupCommands = { Pnc.class, Da.class, Pig.class })
public class App extends AbstractCommand {

    public int run(String[] args) throws Exception {

        CommandRegistry registry;

        if (args.length == 0) {
            registry = AeshCommandRegistryBuilder.builder()
                    .command(Pnc.class)
                    .command(Da.class)
                    .command(Pig.class)
                    .command(CommandBuilder.builder().name("exit").command(commandInvocation -> {
                        commandInvocation.stop();
                        return CommandResult.SUCCESS;
                    }).create())
                    .create();
            SettingsBuilder<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> builder = SettingsBuilder
                    .builder()
                    .logging(true)
                    .enableAlias(false)
                    .enableExport(false)
                    .enableMan(true)
                    .enableSearchInPaging(true)
                    .readInputrc(false)
                    .commandRegistry(registry);

            ReadlineConsole console = new ReadlineConsole(builder.build());
            console.setPrompt(
                    new Prompt(
                            new TerminalString(
                                    "[bacon@console]$ ",
                                    new TerminalColor(Color.DEFAULT, Color.DEFAULT, Color.Intensity.BRIGHT))));

            console.start();

            return 0;
        } else {
            registry = AeshCommandRegistryBuilder.builder().command(this.getClass()).create();

            CommandRuntime runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
            try {
                // Code below copied directly from 'executeCommand' - instead of running:
                // runtime.executeCommand(buildCLIOutput(args));
                // which allows us to grab the CommandResult and thereby get the return code.
                // TODO : Remove once https://github.com/aeshell/aesh/issues/323 is fixed and released.
                Executor executor = runtime.buildExecutor(buildCLIOutput(args));
                Execution exec;
                CommandResult commandResult = null;
                while ((exec = executor.getNextExecution()) != null) {
                    try {
                        commandResult = exec.execute();
                    } catch (CommandException cmd) {
                        if (exec.getResultHandler() != null) {
                            exec.getResultHandler().onExecutionFailure(CommandResult.FAILURE, cmd);
                        }
                        throw cmd;
                    } catch (CommandValidatorException | CommandLineParserException e) {
                        if (exec.getResultHandler() != null) {
                            exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, e);
                        }
                        throw e;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        if (exec.getResultHandler() != null) {
                            exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, e);
                        }
                        throw e;
                    } catch (Exception e) {
                        if (exec.getResultHandler() != null) {
                            exec.getResultHandler().onValidationFailure(CommandResult.FAILURE, e);
                        }
                        throw new RuntimeException(e);
                    }
                }
                return commandResult.getResultValue();
            } catch (OptionParserException | RequiredOptionException e) {
                throw new FatalException("Missing argument/option: {}", e.getMessage(), e);
            } catch (CommandLineParserException e) {
                throw new FatalException("Wrong arguments: {}", e.getMessage(), e);
            } catch (RuntimeException e) {
                if (e.getMessage().contains(FatalException.class.getCanonicalName())) {
                    throw e;
                }
                // if stacktrace not thrown from aesh
                if (!e.getCause().getClass().getCanonicalName().contains("aesh")) {
                    log.error("Stacktrace", e);
                }

                // signal that an error has occurred
                throw new FatalException("Unknown RuntimeException", e);
            }
        }
    }

    private static String buildCLIOutput(String[] args) {
        return "bacon "
                + Arrays.stream(args).map(s -> s.replaceAll("([\"' \\\\])", "\\\\$1")).collect(Collectors.joining(" "));
    }

    public static void main(String[] args) throws Exception {
        try {
            App app = new App();
            System.exit(app.run(args));
        } catch (FatalException e) {
            log.error(e.getMessage());
            log.debug("Full trace", e);
            System.exit(1);
        }
    }
}
