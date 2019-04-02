package org.jboss.pnc.bacon.pig.impl.pnc;

public class NoSuccessfulBuildException extends RuntimeException {
    public NoSuccessfulBuildException(int buildConfigId) {
        super("Unable to find a successful build of build config '" + buildConfigId + "'");
    }
}
