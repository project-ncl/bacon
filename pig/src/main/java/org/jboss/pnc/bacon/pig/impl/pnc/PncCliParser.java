/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig.impl.pnc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/3/17
 */
public class PncCliParser {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Map<String, ?>> parseList(List<String> responseContent) {
        return parseList(join(responseContent, "\n"));
    }

    public static <T> List<T> parseList(List<String> responseContent, TypeReference<List<T>> resultType) {
        return parseList(join(responseContent, "\n"), resultType);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, ?>> parseList(String responseContent) {
        try {
            return StringUtils.isBlank(responseContent) ?
                    Collections.emptyList()
                    : mapper.readValue(responseContent, List.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse " + responseContent, e);
        }
    }

    public static <T> List<T> parseList(String responseContent, TypeReference<List<T>> resultType) {
        try {
            return StringUtils.isBlank(responseContent) ?
                    Collections.emptyList()
                    : mapper.readValue(responseContent, resultType);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse " + responseContent, e);
        }
    }

    public static <T> T parse(List<String> strings, TypeReference<T> resultType){
        String responseContent = join(strings, "\n");
        try {
            return mapper.readValue(responseContent, resultType);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to parse " + responseContent);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ?> parse(List<String> strings) {
        String responseContent = join(strings, "\n");
        try {
            return mapper.readValue(responseContent, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse " + responseContent, e);
        }
    }

    private PncCliParser() {
    }
}
