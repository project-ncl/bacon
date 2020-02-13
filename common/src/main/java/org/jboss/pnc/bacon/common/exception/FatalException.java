package org.jboss.pnc.bacon.common.exception;

/**
 * Throw this exception instead of using System.exit(1)
 */
public class FatalException extends RuntimeException {

    public FatalException() {
        super("FatalException: Don't try to recover");
    }
}
