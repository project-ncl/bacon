package org.jboss.pnc.bacon;

import org.jboss.pnc.bacon.cli.Da;
import org.jboss.pnc.bacon.cli.Pig;
import org.jboss.pnc.bacon.cli.Pnc;
import picocli.CommandLine;

import java.util.List;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/13/18
 */
@CommandLine.Command(name = "java -jar bacon.jar",
        mixinStandardHelpOptions = true,
        subcommands = {Pig.class, Pnc.class, Da.class}
)
public class App {

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(App.class);
        List<CommandLine> parsed = commandLine.parse(args);
        CommandLine command = deepestCommand(parsed);

        new CommandLine.RunLast().handleParseResult(command.getParseResult());
    }

    private static CommandLine deepestCommand(List<CommandLine> parsed) {
        return parsed.get(parsed.size() - 1);
    }
}
