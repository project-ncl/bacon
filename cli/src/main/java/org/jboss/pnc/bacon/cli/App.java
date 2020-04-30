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
import lombok.extern.slf4j.Slf4j;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
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

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/13/18
 */

@Slf4j
@GroupCommandDefinition(name = "bacon", description = "Bacon CLI", groupCommands = { Pnc.class, Da.class, Pig.class })
public class App extends AbstractCommand {

    public void run(String[] args) throws Exception {

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
        } else {
            registry = AeshCommandRegistryBuilder.builder().command(this.getClass()).create();

            CommandRuntime runtime = AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
            try {
                runtime.executeCommand(buildCLIOutput(args));

            } catch (OptionParserException | RequiredOptionException ex) {
                log.error("Missing argument/option: {}", ex.getMessage());
                throw new FatalException();
            } catch (CommandLineParserException ex) {
                log.error("Wrong arguments: {}", ex.getMessage());
                throw new FatalException();
            } catch (RuntimeException ex) {
                if (ex.getMessage().contains(FatalException.class.getCanonicalName())) {
                    throw new FatalException();
                }
                // if stacktrace not thrown from aesh
                if (!ex.getCause().getClass().getCanonicalName().contains("aesh")) {
                    log.error("Stacktrace", ex);
                }

                // signal that an error has occurred
                throw new FatalException();
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
            app.run(args);
        } catch (FatalException e) {
            System.exit(1);
        }
    }
}
