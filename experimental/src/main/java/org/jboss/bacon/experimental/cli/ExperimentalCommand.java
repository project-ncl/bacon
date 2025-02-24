package org.jboss.bacon.experimental.cli;

import java.util.concurrent.Callable;

import org.jboss.pnc.bacon.common.cli.JSONCommandHandler;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.ConfigProfile;

public abstract class ExperimentalCommand extends JSONCommandHandler implements Callable<Integer> {

    @Override
    public Integer call() {
        ConfigProfile activeProfile = Config.instance().getActiveProfile();
        if (!activeProfile.isEnableExperimental()) {
            throw new FatalException("Running Experimental is not enabled. See documentation how to enable it.");
        }
        return runCommand();
    }

    public void run() {
    }

    public int runCommand() {
        run();
        return 0;
    }
}
