package org.jboss.pnc.bacon.common.cli;

import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.common.exception.FatalException;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public class JSONCommandHandler {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JSONCommandHandler.class);
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
        Boolean value = option.<Boolean> getValue();
        log.debug("JSON command is enabled: {}", value);
        return value;
    }
}
