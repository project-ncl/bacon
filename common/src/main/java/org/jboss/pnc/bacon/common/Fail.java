package org.jboss.pnc.bacon.common;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.exception.FatalException;

@Slf4j
public class Fail {

    public static void fail(String reason) {
        failIfNull(null, reason);
    }

    public static void failIfNull(Object object, String reason) {
        if (object == null) {
            log.error(reason);
            throw new FatalException();
        }
    }
}
