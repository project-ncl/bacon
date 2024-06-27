package org.jboss.pnc.bacon.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectHelper {

    public static Level LOG_LEVEL_SILENT = Level.ERROR;

    private static YAMLFactory createYAMLFactory() {
        YAMLFactory yamlFactory = new YAMLFactory();
        return yamlFactory.configure(YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS, true)
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
    }

    private static JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        return jsonFactory;
    }

    private static ObjectMapper getOutputMapper(boolean json) {
        ObjectMapper om = new ObjectMapper(json ? createJsonFactory() : createYAMLFactory());
        om.registerModule(new JavaTimeModule());
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }

    /**
     * Print the object in YAML format by default, unless the json parameter is set to true
     *
     * If the root logger is set to LOG_LEVEL_SILENT or more, nothing is printed
     *
     * @param json whether to print JSON instead of YAML
     * @param o Object to print
     * @throws JsonProcessingException
     */
    public static void print(boolean json, Object o) throws JsonProcessingException {
        if (!getLogger(Logger.ROOT_LOGGER_NAME).getLevel().isGreaterOrEqual(LOG_LEVEL_SILENT)) {
            System.out.println(getOutputMapper(json).writeValueAsString(o));
        }
    }

    public static void setRootLoggingLevel(Level level) {
        ch.qos.logback.classic.Logger root = getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
    }

    public static boolean isLogDebug() {
        ch.qos.logback.classic.Logger root = getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        // INFO has level 20,000, DEBUG has level 10,000
        return root.getLevel().levelInt <= Level.DEBUG_INT;
    }

    public static void setLoggingLevel(String loggerName, Level level) {
        ch.qos.logback.classic.Logger logger = getLogger(loggerName);
        logger.setLevel(level);
    }

    private static ch.qos.logback.classic.Logger getLogger(String loggerName) {
        return (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(loggerName);
    }
}
