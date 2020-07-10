package org.jboss.pnc.bacon.pnc.common;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.enums.RebuildMode;

/**
 * Helper class to check for parameter validity
 */
@Slf4j
public class ParameterChecker {

    /**
     * Check that the rebuild mode provided by the user is valid or not
     */
    public static void checkRebuildModeOption(String rebuildMode) throws FatalException {

        try {
            RebuildMode.valueOf(rebuildMode);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("The rebuild flag contains an illegal option. Possibilities are: ");
            for (RebuildMode mode : RebuildMode.values()) {
                log.error(mode.toString());
            }
            throw new FatalException();
        }
    }
}
