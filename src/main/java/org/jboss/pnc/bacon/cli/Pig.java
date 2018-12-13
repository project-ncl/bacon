package org.jboss.pnc.bacon.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@Command(name = "pig", mixinStandardHelpOptions = true)
public class Pig {
    @Command(name = "configure", mixinStandardHelpOptions = true)
    public void configure(
            @Option(names = {"-c", "--config"}, defaultValue = "build-config.yaml", description = "location of the configuration file")
                    String configLocation) {
        System.out.println("configLocation: " + configLocation);
        throw new TodoException();
    }

    @Command(name = "build", mixinStandardHelpOptions = true)
    public void build() {
        throw new TodoException();
    }

    @Command(name = "repo", mixinStandardHelpOptions = true)
    public void generateRepository() {
        throw new TodoException();
    }

    @Command(name = "licenses", mixinStandardHelpOptions = true)
    public void generateLicenses() {
        throw new TodoException();
    }

    @Command(name = "javadocs", mixinStandardHelpOptions = true)
    public void generateJavadoc() {
        throw new TodoException();
    }

    @Command(name = "sources", mixinStandardHelpOptions = true)
    public void generateSources() {
        throw new TodoException();
    }

    @Command(name = "shared-content", mixinStandardHelpOptions = true)
    public void generateSharedContentAnalysis() {
        throw new TodoException();
    }

    @Command(name = "docs", mixinStandardHelpOptions = true)
    public void generateDocuments() {
        throw new TodoException();
    }

    @Command(name = "addons", mixinStandardHelpOptions = true)
    public void triggerAddOns() {
        throw new TodoException();
    }

    @Command(name = "scripts", mixinStandardHelpOptions = true)
    public void generateScripts() {
        throw new TodoException();
    }
}
