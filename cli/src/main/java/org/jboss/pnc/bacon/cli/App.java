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
import org.jline.builtins.Options;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    @Option(
            names = { Constant.JSON_OUTPUT, "--jsonOutput" },
            description = "use json for output (default to yaml)",
            scope = INHERIT)
    private boolean jsonOutput = false;

    /**
     * Set the verbosity of logback if the verbosity flag is set
     */
    @Option(names = { "-v", "--verbose" }, description = "Verbose output", scope = INHERIT)
    public static void setVerbosityIfPresent(boolean verbose) {

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
            builtins.rename(Builtins.Command.TTOP, "top");
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
                        .variable(LineReader.HISTORY_FILE_SIZE, 100)
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

                log.info("Tooltips enabled ; press Alt-s to disable");
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
            return commandLine.setExecutionStrategy(parseResult -> executionStrategy(commandLine, parseResult))
                    .execute(args);
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

    private int executionStrategy(CommandLine commandLine, CommandLine.ParseResult parseResult) {
        if (!versionInfoRequested) { // Don't print version information if we are outputting version explicitly
            commandLine.printVersionHelp(System.err);
        }
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

    // Explicit inline override of picocli jline3 until picocli 4.6 is released fixing
    // https://github.com/remkop/picocli/issues/1175

    /**
     * Compiles SystemCompleter for command completion and implements a method commandDescription() that provides
     * command descriptions for JLine TailTipWidgets to be displayed in terminal status bar. SystemCompleter implements
     * the JLine 3 {@link Completer} interface. SystemCompleter generates completion candidates for the specified
     * command line based on the {@link CommandLine.Model.CommandSpec} that this {@code PicocliCommands} was constructed
     * with.
     *
     * @since 4.1.2
     */
    static class PicocliCommands implements CommandRegistry {
        private final Supplier<Path> workDir;
        private final CommandLine cmd;
        private final Set<String> commands;
        private final Map<String, String> aliasCommand = new HashMap<>();

        public PicocliCommands(Path workDir, CommandLine cmd) {
            this(() -> workDir, cmd);
        }

        public PicocliCommands(Supplier<Path> workDir, CommandLine cmd) {
            this.workDir = workDir;
            this.cmd = cmd;
            commands = cmd.getCommandSpec().subcommands().keySet();
            for (String c : commands) {
                for (String a : cmd.getSubcommands().get(c).getCommandSpec().aliases()) {
                    aliasCommand.put(a, c);
                }
            }
        }

        /**
         *
         * @param command
         * @return true if PicocliCommands contains command
         */
        @Override
        public boolean hasCommand(String command) {
            return commands.contains(command) || aliasCommand.containsKey(command);
        }

        @Override
        public SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            List<String> all = new ArrayList<>();
            all.addAll(commands);
            all.addAll(aliasCommand.keySet());
            out.add(all, new PicocliCompleter());
            return out;
        }

        private class PicocliCompleter implements Completer {

            public PicocliCompleter() {
            }

            @Override
            public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
                assert commandLine != null;
                assert candidates != null;
                String word = commandLine.word();
                List<String> words = commandLine.words();
                CommandLine sub = findSubcommandLine(words, commandLine.wordIndex());
                if (sub == null) {
                    return;
                }
                if (word.startsWith("-")) {
                    String buffer = word.substring(0, commandLine.wordCursor());
                    int eq = buffer.indexOf('=');
                    for (CommandLine.Model.OptionSpec option : sub.getCommandSpec().options()) {
                        if (option.arity().max() == 0 && eq < 0) {
                            addCandidates(candidates, Arrays.asList(option.names()));
                        } else {
                            if (eq > 0) {
                                String opt = buffer.substring(0, eq);
                                if (Arrays.asList(option.names()).contains(opt)
                                        && option.completionCandidates() != null) {
                                    addCandidates(
                                            candidates,
                                            option.completionCandidates(),
                                            buffer.substring(0, eq + 1),
                                            "",
                                            true);
                                }
                            } else {
                                addCandidates(candidates, Arrays.asList(option.names()), "", "=", false);
                            }
                        }
                    }
                } else {
                    addCandidates(candidates, sub.getSubcommands().keySet());
                    for (CommandLine s : sub.getSubcommands().values()) {
                        addCandidates(candidates, Arrays.asList(s.getCommandSpec().aliases()));
                    }
                }
            }

            private void addCandidates(List<Candidate> candidates, Iterable<String> cands) {
                addCandidates(candidates, cands, "", "", true);
            }

            private void addCandidates(
                    List<Candidate> candidates,
                    Iterable<String> cands,
                    String preFix,
                    String postFix,
                    boolean complete) {
                for (String s : cands) {
                    candidates.add(
                            new Candidate(
                                    AttributedString.stripAnsi(preFix + s + postFix),
                                    s,
                                    null,
                                    null,
                                    null,
                                    null,
                                    complete));
                }
            }

        }

        private CommandLine findSubcommandLine(List<String> args, int lastIdx) {
            CommandLine out = cmd;
            for (int i = 0; i < lastIdx; i++) {
                if (!args.get(i).startsWith("-")) {
                    out = findSubcommandLine(out, args.get(i));
                    if (out == null) {
                        break;
                    }
                }
            }
            return out;
        }

        private static CommandLine findSubcommandLine(CommandLine cmdline, String command) {
            for (CommandLine s : cmdline.getSubcommands().values()) {
                if (s.getCommandName().equals(command)
                        || Arrays.asList(s.getCommandSpec().aliases()).contains(command)) {
                    return s;
                }
            }
            return null;
        }

        /**
         *
         * @param args
         * @return command description for JLine TailTipWidgets to be displayed in terminal status bar.
         */
        @Override
        public CmdDesc commandDescription(List<String> args) {
            CommandLine sub = findSubcommandLine(args, args.size());
            if (sub == null) {
                return null;
            }
            CommandLine.Model.CommandSpec spec = sub.getCommandSpec();
            CommandLine.Help cmdhelp = new picocli.CommandLine.Help(spec);
            List<AttributedString> main = new ArrayList<>();
            Map<String, List<AttributedString>> options = new HashMap<>();
            String synopsis = AttributedString
                    .stripAnsi(spec.usageMessage().sectionMap().get("synopsis").render(cmdhelp).toString());
            main.add(Options.HelpException.highlightSyntax(synopsis.trim(), Options.HelpException.defaultStyle()));
            // using JLine help highlight because the statement below does not work well...
            // main.add(new
            // AttributedString(spec.usageMessage().sectionMap().get("synopsis").render(cmdhelp).toString()));
            for (CommandLine.Model.OptionSpec o : spec.options()) {
                String key = Arrays.stream(o.names()).collect(Collectors.joining(" "));
                List<AttributedString> val = new ArrayList<>();
                for (String d : o.description()) {
                    val.add(new AttributedString(d));
                }
                if (o.arity().max() > 0) {
                    key += "=" + o.paramLabel();
                }
                options.put(key, val);
            }
            return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
        }

        @Override
        public List<String> commandInfo(String command) {
            List<String> out = new ArrayList<>();
            CommandLine.Model.CommandSpec spec = cmd.getSubcommands().get(command).getCommandSpec();
            CommandLine.Help cmdhelp = new picocli.CommandLine.Help(spec);
            String description = AttributedString
                    .stripAnsi(spec.usageMessage().sectionMap().get("description").render(cmdhelp));
            out.addAll(Arrays.asList(description.split(System.lineSeparator())));
            return out;
        }

        @Override
        public Object invoke(CommandRegistry.CommandSession session, String command, Object[] args) throws Exception {
            List<String> arguments = new ArrayList<>();
            arguments.add(command);
            arguments.addAll(Arrays.stream(args).map(Object::toString).collect(Collectors.toList()));
            cmd.execute(arguments.toArray(new String[0]));
            return null;
        }

        @Override
        public Set<String> commandNames() {
            return commands;
        }

        @Override
        public Map<String, String> commandAliases() {
            return aliasCommand;
        }
    }
}
