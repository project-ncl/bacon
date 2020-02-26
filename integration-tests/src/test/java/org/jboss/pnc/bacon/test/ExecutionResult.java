package org.jboss.pnc.bacon.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 *
 * @author jbrazdil
 */
public class ExecutionResult {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    static {
        YAML_MAPPER.registerModule(new JavaTimeModule());
        JSON_MAPPER.registerModule(new JavaTimeModule());
    }

    private final String output;
    private final String error;
    private final int retval;

    public ExecutionResult(String output, String error, int retval) {
        this.output = output;
        this.error = error;
        this.retval = retval;
    }

    public String getOutput() {
        return output;
    }

    /**
     * Maps the output from YAML format to object.
     */
    public <T> T fromYAML(Class<T> clazz) throws JsonProcessingException {
        return YAML_MAPPER.readValue(output, clazz);
    }

    /**
     * Maps the output from JSON format to object.
     */
    public <T> T fromJSON(Class<T> clazz) throws JsonProcessingException {
        return JSON_MAPPER.readValue(output, clazz);
    }

    public String getError() {
        return error;
    }

    public int getRetval() {
        return retval;
    }

}
