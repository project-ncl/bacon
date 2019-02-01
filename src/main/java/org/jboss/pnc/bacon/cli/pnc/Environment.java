package org.jboss.pnc.bacon.cli.pnc;

import org.jboss.pnc.bacon.cli.SubCommandHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "environment", mixinStandardHelpOptions = true)
public class Environment extends SubCommandHelper {

    @CommandLine.Command(name = "get", mixinStandardHelpOptions = true)
    public void get() {

    }

    @CommandLine.Command(name = "list", mixinStandardHelpOptions = true)
    public void list() {

    }

}
