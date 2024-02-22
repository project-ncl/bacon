package org.jboss.pnc.bacon.common;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.junit.jupiter.api.Assertions.*;

class ObjectHelperTest {

    @Test
    void printJson() throws Exception {
        String actual = tapSystemOut(() -> {
            Map<String, String> testSubject = new HashMap<>();

            testSubject.put("test", "subject");

            ObjectHelper.print(true, testSubject);
        });

        String expected = String.format("{\"test\":\"subject\"}%n");

        assertEquals(expected, actual);
    }

    @Test
    void printYaml() throws Exception {
        String actual = tapSystemOut(() -> {
            Map<String, String> testSubject = new HashMap<>();

            testSubject.put("test", "subject");

            ObjectHelper.print(false, testSubject);
        });

        String expected = String.format("---%ntest: \"subject\"%n%n");

        assertEquals(expected, actual);
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
}
