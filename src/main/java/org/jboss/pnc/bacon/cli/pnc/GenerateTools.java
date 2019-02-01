package org.jboss.pnc.bacon.cli.pnc;

import org.jboss.pnc.bacon.cli.SubCommandHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "generate", mixinStandardHelpOptions = true,
                     description = "Further tools to generate artifacts")
public class GenerateTools extends SubCommandHelper {

    @CommandLine.Command(name = "repo-list", mixinStandardHelpOptions = true)
    public void generateRepositoryList() {
        // TODO: should this exist given PIG might implement it also?
    }

    @CommandLine.Command(name = "sources-zip", mixinStandardHelpOptions = true)
    public void generateSourcesZip() {
        // TODO: should this exist given PIG might implement it also?
    }

    @CommandLine.Command(name = "make-mead", mixinStandardHelpOptions = true)
    public void makeMead() {

    }

}
