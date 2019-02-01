package org.jboss.pnc.bacon.cli.pnc;

import org.jboss.pnc.bacon.cli.SubCommandHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "product", mixinStandardHelpOptions = true)
public class Product extends SubCommandHelper {

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
}
