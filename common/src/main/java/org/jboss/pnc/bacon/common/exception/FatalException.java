package org.jboss.pnc.bacon.common.exception;

import org.jboss.pnc.mavenmanipulator.common.ExceptionHelper;
import org.slf4j.helpers.MessageFormatter;

/**
 * Throw this exception instead of using System.exit(1)
 */
public class FatalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Object[] params;

    private String formattedMessage;

    public FatalException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FatalException(final String string, final Object... params) {
        super(string, ExceptionHelper.getThrowableCandidate(params));
        this.params = params;
    }

    @Override
    public synchronized String getMessage() {
        if (formattedMessage == null) {
            formattedMessage = MessageFormatter.arrayFormat(super.getMessage(), params).getMessage();
        }
        return formattedMessage;
    }
}
