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


import org.jboss.pnc.bacon.pig.impl.utils.OSCommandExecutor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 7/14/17
 */
public class PncDao {
    public static List<String> invoke(String command) {
        return invoke(command, 1);
    }

    public static List<String> invoke(String command, int attempts) {
        return OSCommandExecutor.executor("pnc " + command)
                .redirectErrorStream(false)
                .failOnInvalidStatusCode()
                .retrying(attempts)
                .exec()
                .getOut();
    }

    public static Integer invokeAndGetResultId(String command) {
        return invokeAndGetResultId(command, 1);
    }

    public static Integer invokeAndGetResultId(String command, int attempts) {
        return (Integer) invokeAndParse(command, attempts).get("id");
    }

    public static List<Map<String, ?>> invokeAndParseList(String command) {
        return invokeAndParseList(command, 4);
    }

    public static List<Map<String, ?>> invokeAndParseList(String command, int attempts) {
        List<String> output = invoke(command, attempts);
        try {
            return PncCliParser.parseList(output);
        } catch (Exception any) {
            throw new RuntimeException("Failed to parse the output of " + command, any);
        }
    }

    public static List<Map<String, ?>> invokeAndParseListRetryingWithTimeout(
            String command,
            int timeout,
            int attempts) {
        List<String> output = OSCommandExecutor.executor("pnc " + command)
                .redirectErrorStream(false)
                .failOnInvalidStatusCode()
                .timeout(timeout)
                .retrying(attempts)
                .exec()
                .getOut();
        try {
            return PncCliParser.parseList(output);
        } catch (Exception any) {
            throw new RuntimeException("Failed to parse the output of " + command, any);
        }
    }

    public static Set<Integer> invokeAndGetResultIds(String command) {
        return invokeAndGetResultIds(command, 1);
    }

    public static Set<Integer> invokeAndGetResultIds(String command, int attempts) {
        return invokeAndParseList(command, attempts)
                .stream()
                .map(m -> (Integer)m.get("id"))
                .collect(Collectors.toSet());
    }

    public static Map<String, ?> invokeAndParse(String command) {
        return invokeAndParse(command, 1);
    }
    public static Map<String, ?> invokeAndParse(String command, int attempts) {
        List<String> output = invoke(command, attempts);
        try {
            return PncCliParser.parse(output);
        } catch (Exception any) {
            throw new RuntimeException("Failed to parse the output of " + command, any);
        }
    }

    private PncDao() {
    }
}
