package org.jboss.pnc.bacon.pig;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 2/22/19
 */
public class PigException extends RuntimeException {
    public PigException(String message) {
        super(message);
    }

    public PigException(String message, Throwable cause) {
        super(message, cause);
    }
}
