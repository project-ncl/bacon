/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
