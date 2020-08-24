package org.jboss.pnc.bacon.common;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObjectHelperTest {

    @Test
    void executeIfNotNull() {
        ObjectHelper.executeIfNotNull(null, () -> fail("I shouldn't be run"));

        boolean[] called = { false };
        ObjectHelper.executeIfNotNull("not null", () -> {
            called[0] = true;
        });
        assertTrue(called[0], "The runnable should be executed");
    }

    @Test
    void printJson() throws Exception {
        PrintStream old = System.out;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(stream, false, StandardCharsets.UTF_8.name());
        System.setOut(ps);
        Map<String, String> testSubject = Maps.newHashMap();
        testSubject.put("test", "subject");

        ObjectHelper.print(true, testSubject);
        System.out.flush();
        System.setOut(old);

        // FIXME: Ideally, we wouldn't need to trim, but we may get a different final newline
        // FIXME: See <https://github.com/project-ncl/bacon/issues/349> for details
        String output = stream.toString().trim();
        assertEquals("{\"test\":\"subject\"}", output);
    }

    @Test
    void printYaml() throws Exception {
        PrintStream old = System.out;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(stream, false, StandardCharsets.UTF_8.name());
        System.setOut(ps);
        Map<String, String> testSubject = Maps.newHashMap();
        testSubject.put("test", "subject");

        ObjectHelper.print(false, testSubject);

        System.out.flush();
        System.setOut(old);

        // FIXME: Ideally, we wouldn't need to trim, but we may get a different final newline
        // FIXME: See <https://github.com/project-ncl/bacon/issues/349> for details
        String output = stream.toString().trim();
        assertEquals("---\ntest: \"subject\"", output);
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
