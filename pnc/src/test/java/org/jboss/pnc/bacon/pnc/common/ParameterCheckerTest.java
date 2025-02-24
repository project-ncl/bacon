package org.jboss.pnc.bacon.pnc.common;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.pnc.bacon.common.exception.FatalException;
import org.junit.jupiter.api.Test;

class ParameterCheckerTest {
    @Test
    void verifyRebuildOption() {
        try {
            ParameterChecker.checkRebuildModeOption("FOOBAR");
            fail("No exception thrown");
        } catch (FatalException ex) {
            assertTrue(ex.getMessage().contains("FOOBAR"));
        }
    }
}
