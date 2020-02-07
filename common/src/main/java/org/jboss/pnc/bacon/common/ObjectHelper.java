package org.jboss.pnc.bacon.common;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ObjectHelper {

    public static void executeIfNotNull(Object value, Runnable run) {
        if (value != null) {
            run.run();
        }
    }

    private static ObjectMapper getOutputMapper(boolean json) {
        return json ? new ObjectMapper(new JsonFactory()) : new ObjectMapper(new YAMLFactory());
    }

    public static void print(boolean json, Object o) throws JsonProcessingException {
        System.out.println(getOutputMapper(json).writeValueAsString(o));
    }
}
