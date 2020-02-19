package org.jboss.pnc.bacon.common;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectHelper {

    public static void executeIfNotNull(Object value, Runnable run) {
        if (value != null) {
            run.run();
        }
    }

    private static ObjectMapper getOutputMapper(boolean json) {
        ObjectMapper om = json ? new ObjectMapper(new JsonFactory()) : new ObjectMapper(new YAMLFactory());
        om.registerModule(new JavaTimeModule());
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }

    public static void print(boolean json, Object o) throws JsonProcessingException {
        System.out.println(getOutputMapper(json).writeValueAsString(o));
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
