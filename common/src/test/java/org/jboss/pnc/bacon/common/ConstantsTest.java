package org.jboss.pnc.bacon.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConstantsTest {

    @Test
    void makeSureVersionInjected() {
        assertFalse(Constants.VERSION.contains("@"));
        assertFalse(Constants.VERSION.isEmpty());
    }

    @Test
    void makeSureSHAInjected() {
        assertFalse(Constants.COMMIT_ID_SHA.contains("@"));
        assertFalse(Constants.COMMIT_ID_SHA.isEmpty());
    }
}
