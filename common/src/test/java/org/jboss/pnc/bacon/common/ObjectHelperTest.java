package org.jboss.pnc.bacon.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.qos.logback.classic.Level;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

@ExtendWith(SystemStubsExtension.class)
class ObjectHelperTest {

    @SystemStub
    private SystemOut systemOut;

    @Test
    void printJson() throws Exception {
        Map<String, String> testSubject = new HashMap<>();

        testSubject.put("test", "subject");

        ObjectHelper.print(true, testSubject);

        String expected = String.format("{\"test\":\"subject\"}%n");

        assertEquals(expected, systemOut.getText());
    }

    @Test
    void printYaml() throws Exception {
        Map<String, String> testSubject = new HashMap<>();

        testSubject.put("test", "subject");

        ObjectHelper.print(false, testSubject);

        String expected = String.format("---%ntest: \"subject\"%n%n");

        assertEquals(expected, systemOut.getText());
    }

    @Test
    void setRootLoggingLevel() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        ObjectHelper.setRootLoggingLevel(Level.TRACE);
        assertSame(Level.TRACE, root.getLevel());
    }

    @Test
    void setLoggingLevel() {
        String loggerName = "test";

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(loggerName);

        ObjectHelper.setLoggingLevel("test", Level.ERROR);
        assertSame(Level.ERROR, logger.getLevel());
    }

    @Test
    void isLogDebug() {
        ObjectHelper.setRootLoggingLevel(Level.INFO);
        assertFalse(ObjectHelper.isLogDebug());

        ObjectHelper.setRootLoggingLevel(Level.DEBUG);
        assertTrue(ObjectHelper.isLogDebug());

        ObjectHelper.setRootLoggingLevel(Level.TRACE);
        assertTrue(ObjectHelper.isLogDebug());
    }
}
