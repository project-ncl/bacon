package org.jboss.pnc.bacon.cli.pnc;

import org.jboss.pnc.bacon.cli.SubCommandHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "product-release", mixinStandardHelpOptions = true)
public class ProductRelease extends SubCommandHelper {
}
