package org.jboss.pnc.bacon.common.cli;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.exception.FatalException;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Slf4j
public class JSONCommandHandler {
    @Spec
    public CommandSpec commandSpec;

    /**
     * Retrieve the value of the json output option.
     *
     * @return a boolean if json output is enabled.
     */
    protected boolean getJsonOutput() {
        CommandLine.Model.OptionSpec option = commandSpec.root().findOption(Constant.JSON_OUTPUT);
        if (option == null) {
            throw new FatalException("Unable to locate JSON parameter in root");
        }
        log.debug("JSON command is enabled: " + option.getValue());
        return option.getValue();
    }
}
