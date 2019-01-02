package org.jboss.pnc.bacon.cli.pnc;

import picocli.CommandLine;

@CommandLine.Command(name = "project", mixinStandardHelpOptions = true)
public class Project {

    @CommandLine.Command(name = "create", mixinStandardHelpOptions = true)
    public void create() {
    }

    @CommandLine.Command(name = "get", mixinStandardHelpOptions = true)
    public void get() {
    }

    @CommandLine.Command(name = "list", mixinStandardHelpOptions = true)
    public void list() {
    }

    @CommandLine.Command(name = "update", mixinStandardHelpOptions = true)
    public void update() {
    }

    @CommandLine.Command(name = "delete", mixinStandardHelpOptions = true)
    public void delete() {
    }
}
