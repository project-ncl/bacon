package org.jboss.pnc.bacon.cli.pnc;

import org.jboss.pnc.bacon.cli.SubCommandHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "brew-push", mixinStandardHelpOptions = true)
public class BrewPush extends SubCommandHelper {
}
