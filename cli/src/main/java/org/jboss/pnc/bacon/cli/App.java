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
import org.jboss.bacon.da.Da;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.ObjectHelper;
import org.jboss.pnc.bacon.common.cli.VersionProvider;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pig.Pig;
import org.jboss.pnc.bacon.pnc.Pnc;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static picocli.CommandLine.ScopeType.INHERIT;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/13/18
 */

@Slf4j
@Command(name = "bacon.jar", versionProvider = VersionProvider.class, subcommands = { Da.class, Pig.class, Pnc.class })
public class App {

    private String configPath = null;

    private String profile = "default";

    public int run(String[] args) {
        if (args.length == 0) {

            // TODO: Implement

            // registry = AeshCommandRegistryBuilder.builder()
            // .command(Pnc.class)
            // .command(Da.class)
            // .command(Pig.class)
            // .command(CommandBuilder.builder().name("exit").command(commandInvocation -> {
            // commandInvocation.stop();
            // return CommandResult.SUCCESS;
            // }).create())
            // .create();
            // SettingsBuilder<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
            // OptionActivator, CommandActivator> builder = SettingsBuilder
            // .builder()
            // .logging(true)
            // .enableAlias(false)
            // .enableExport(false)
            // .enableMan(true)
            // .enableSearchInPaging(true)
            // .readInputrc(false)
            // .commandRegistry(registry);
            //
            // ReadlineConsole console = new ReadlineConsole(builder.build());
            // console.setPrompt(
            // new Prompt(
            // new TerminalString(
            // "[bacon@console]$ ",
            // new TerminalColor(Color.DEFAULT, Color.DEFAULT, Color.Intensity.BRIGHT))));
            //
            // console.start();
            //
            // return 0;
            throw new FatalException("NYI");
        } else {
            return new CommandLine(this).setExecutionStrategy(this::executionStrategy).execute(args);
        }
    }

    public static void main(String[] args) {
        try {
            System.exit(new App().run(args));
        } catch (FatalException e) {
            log.error(e.getMessage());
            log.debug("Full trace", e);
            System.exit(1);
        }
    }

    /**
     * Set the verbosity of logback if the verbosity flag is set
     */
    @Option(names = "-v", description = "Verbose output", scope = INHERIT)
    public void setVerbosityIfPresent(boolean verbose) {

        if (verbose) {
            ObjectHelper.setRootLoggingLevel(Level.DEBUG);

            // Add more loggers that you want to switch to DEBUG here
            ObjectHelper.setLoggingLevel("org.jboss.pnc.client", Level.DEBUG);

            log.debug("Log level set to DEBUG");
        }
    }

    @Option(names = "--profile", defaultValue = "default", description = "PNC Configuration profile", scope = INHERIT)
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Set the path to config file if the configPath flag or environment variable is set
     */
    @Option(names = "-p", description = "Path to PNC configuration folder", scope = INHERIT)
    public void setConfigurationFileLocation(String configPath) {
        if (configPath != null) {
            setConfigLocation(configPath, "flag");
        }
    }

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message", scope = INHERIT)
    boolean usageHelpRequested;

    @Option(names = { "-V", "--version" }, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    private void init() {
        if (System.getenv(Constant.CONFIG_ENV) != null) {
            setConfigLocation(System.getenv(Constant.CONFIG_ENV), "environment variable");
        } else {
            setConfigLocation(Constant.DEFAULT_CONFIG_FOLDER, "Constant");
        }
    }

    private void setConfigLocation(String configLocation, String source) {
        Config.configure(configLocation, Constant.CONFIG_FILE_NAME, profile);
        log.debug("Config file set from {} to {}", source, Config.getConfigFilePath());
    }

    private int executionStrategy(CommandLine.ParseResult parseResult) {
        init(); // custom initialization to be done before executing any command or subcommand
        return new CommandLine.RunLast().execute(parseResult); // default execution strategy
    }
}
