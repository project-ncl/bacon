package org.jboss.pnc.bacon.common;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ObjectHelper {

    public static void executeIfNotNull(Object value, Runnable run) {
        if (value != null) {
            run.run();
        }
    }

    public static ObjectMapper getOutputMapper(boolean json) {
        return json ? new ObjectMapper(new JsonFactory()) : new ObjectMapper(new YAMLFactory());
    }
}
