package org.jboss.pnc.bacon.common;

import ch.qos.logback.classic.Level;

public class ObjectHelper {

    public static void executeIfNotNull(Object value, Runnable run) {
        if (value != null) {
            run.run();
        }
    }

    public static void setRootLoggingLevel(Level level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
    }

    public static void setLoggingLevel(String loggerName, Level level) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(loggerName);
        logger.setLevel(level);
    }
}
