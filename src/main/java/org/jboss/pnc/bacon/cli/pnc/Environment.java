package org.jboss.pnc.bacon.cli.pnc;

import picocli.CommandLine;

@CommandLine.Command(name = "environment", mixinStandardHelpOptions = true)
public class Environment {

    @CommandLine.Command(name = "get", mixinStandardHelpOptions = true)
    public void get() {

    }

    @CommandLine.Command(name = "list", mixinStandardHelpOptions = true)
    public void list() {

    }

}
