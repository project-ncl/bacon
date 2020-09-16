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

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.AnsiConsole;
import org.jboss.bacon.da.Da;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.VersionProvider;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pig.Pig;
import org.jboss.pnc.bacon.pnc.Pnc;
import org.jline.builtins.Builtins;
import org.jline.builtins.SystemRegistry;
import org.jline.builtins.SystemRegistryImpl;
import org.jline.builtins.Widgets.TailTipWidgets;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static picocli.CommandLine.ScopeType.INHERIT;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/13/18
 */

@Slf4j
@Command(name = "bacon", versionProvider = VersionProvider.class, subcommands = { Da.class, Pig.class, Pnc.class })
public class App {

    private String profile = "default";

    private String configPath = null;

    public static void main(String[] args) {
        System.exit(new App().run(args));
    }

    @SuppressWarnings("FieldMayBeFinal")
    @Option(names = Constant.JSON_OUTPUT, description = "use json for output (default to yaml)", scope = INHERIT)
    private boolean jsonOutput = false;

    /**
     * Set the verbosity of logback if the verbosity flag is set
     */
    @Option(names = { "-v", "--verbose" }, description = "Verbose output", scope = INHERIT)
    public void setVerbosityIfPresent(boolean verbose) {

        if (verbose) {
            ObjectHelper.setRootLoggingLevel(Level.DEBUG);

            // Add more loggers that you want to switch to DEBUG here
            ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.DEBUG);

            log.debug("Log level set to DEBUG");
        }
    }

    @Option(names = "--profile", description = "PNC Configuration profile", scope = INHERIT)
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Set the path to config file if the configPath flag or environment variable is set
     */
    @Option(names = { "-p", "--configPath" }, description = "Path to PNC configuration folder", scope = INHERIT)
    public void setConfigurationFileLocation(String configPath) {
        this.configPath = configPath;
    }

    // Both help/version commands could use standard mixin but in order to propagate the help via inheritance it is
    // explicitly defined here. These could be removed once https://github.com/remkop/picocli/issues/1164 is complete.
    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message", scope = INHERIT)
    boolean usageHelpRequested;

    @Option(names = { "-V", "--version" }, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    public int run(String[] args) {

        CommandLine commandLine = new CommandLine(this);
        commandLine.setExecutionExceptionHandler(new ExceptionMessageHandler());
        commandLine.setUsageHelpAutoWidth(true);

        if (args.length == 0) {
            // From https://github.com/remkop/picocli/wiki/JLine-3-Examples and
            // https://github.com/remkop/picocli/tree/master/picocli-shell-jline3
            AnsiConsole.systemInstall();
            // set up JLine built-in commands
            Builtins builtins = new Builtins(App::workDir, null, null);
            builtins.rename(org.jline.builtins.Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");

            PicocliCommands picocliCommands = new PicocliCommands(App::workDir, commandLine);
            init();

            Parser parser = new DefaultParser();

            try (Terminal terminal = TerminalBuilder.builder().build()) {
                SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, App::workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);

                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .parser(parser)
                        .variable(LineReader.HISTORY_FILE, Constant.HISTORY)
                        .variable(LineReader.HISTORY_FILE_SIZE, 50)
                        .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                        .build();
                builtins.setLineReader(reader);
                TailTipWidgets tailTipWidgets = new TailTipWidgets(
                        reader,
                        systemRegistry::commandDescription,
                        5,
                        TailTipWidgets.TipType.COMPLETER);
                tailTipWidgets.enable();
                KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
                keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

                log.info("Tooltips disabled ; press Alt-s to enable");
                String prompt = "prompt> ";

                // start the shell and process input until the user quits with Ctrl-D
                String line;
                while (true) {
                    try {
                        systemRegistry.cleanUp();
                        line = reader.readLine(prompt, null, (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (UserInterruptException e) {
                        // Ignore
                    } catch (EndOfFileException e) {
                        AnsiConsole.systemUninstall();
                        return 0;
                    } catch (Exception e) {
                        systemRegistry.trace(e);
                    }
                }
            } catch (IOException e) {
                throw new FatalException("Unable to construct terminal console", e);
            }
        } else {
            return commandLine.setExecutionStrategy(this::executionStrategy).execute(args);
        }
    }

    private void init() {
        if (configPath != null) {
            setConfigLocation(configPath, "flag");
        } else if (System.getenv(Constant.CONFIG_ENV) != null) {
            setConfigLocation(System.getenv(Constant.CONFIG_ENV), "environment variable");
        } else {
            setConfigLocation(Constant.DEFAULT_CONFIG_FOLDER, "constant");
        }
    }

    private void setConfigLocation(String configLocation, String source) {
        Config.configure(configLocation, Constant.CONFIG_FILE_NAME, profile);
        log.debug("Config file set from {} with profile {} to {}", source, profile, Config.getConfigFilePath());
    }

    private int executionStrategy(CommandLine.ParseResult parseResult) {
        init(); // custom initialization to be done before executing any command or subcommand
        return new CommandLine.RunLast().execute(parseResult); // default execution strategy
    }

    private static Path workDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    private static class ExceptionMessageHandler implements IExecutionExceptionHandler {

        /**
         * Handles an {@code Exception} that occurred while executing the {@code Runnable} or {@code Callable} command
         * and returns an exit code suitable for returning from {@link picocli.CommandLine#execute(String...)}.
         *
         * @param ex the Exception thrown by the {@code Runnable}, {@code Callable} or {@code Method} user object of the
         *        command
         * @param cmd the CommandLine representing the command or subcommand where the exception occurred
         * @param parseResult the result of parsing the command line arguments
         * @return an exit code
         */
        @Override
        public int handleExecutionException(Exception ex, CommandLine cmd, CommandLine.ParseResult parseResult) {
            log.error(ex.getMessage());
            log.debug("Full trace", ex);

            return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
        }
    }
}
