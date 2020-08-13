package org.jboss.pnc.bacon.common;

import org.jboss.pnc.bacon.common.exception.FatalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FailTest {

    @Test
    void fail() {
        assertThrows(FatalException.class, () -> Fail.fail("Why not"));

    }

    @Test
    void failIfNull() {
        assertThrows(FatalException.class, () -> Fail.failIfNull(null, "Testing if null"));
        assertDoesNotThrow(() -> Fail.failIfNull("not null", "This should not be thrown"));
    }
}
