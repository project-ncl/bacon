/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.addons.provenance;

import java.net.InetAddress;
import java.time.Instant;
import java.util.*;

public record EnvironmentInfo(
        String hostname,
        OsInfo os,
        JavaInfo java,
        Map<String, String> ci,
        String collectedAt) {
    public record OsInfo(String name, String version, String arch) {
    }

    public record JavaInfo(String runtimeVersion, String vendor) {
    }

    public static EnvironmentInfo collect() {
        String hostname = safeHostname();

        OsInfo os = new OsInfo(
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.version", "unknown"),
                System.getProperty("os.arch", "unknown"));

        JavaInfo java = new JavaInfo(
                System.getProperty("java.runtime.version", System.getProperty("java.version", "unknown")),
                System.getProperty("java.vendor", "unknown"));

        Map<String, String> ci = collectCiAllowlisted();

        return new EnvironmentInfo(
                hostname,
                os,
                java,
                ci,
                Instant.now().toString());
    }

    private static String safeHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return System.getenv().getOrDefault("HOSTNAME", "unknown");
        }
    }

    private static Map<String, String> collectCiAllowlisted() {
        // Let's try to capture any meaningful env variable
        Map<String, String> env = System.getenv();
        Map<String, String> out = new LinkedHashMap<>();

        putIfPresent(out, env, "JENKINS_URL");
        putIfPresent(out, env, "JOB_NAME");
        putIfPresent(out, env, "BUILD_NUMBER");
        putIfPresent(out, env, "BUILD_ID");
        putIfPresent(out, env, "BUILD_TAG");
        putIfPresent(out, env, "BUILD_URL");

        putIfPresent(out, env, "GIT_URL");
        putIfPresent(out, env, "GIT_COMMIT");
        putIfPresent(out, env, "BRANCH_NAME");
        putIfPresent(out, env, "CHANGE_ID");
        putIfPresent(out, env, "CHANGE_URL");

        putIfPresent(out, env, "CI");
        putIfPresent(out, env, "RUNNER_NAME");
        putIfPresent(out, env, "RUN_ID");

        return out;
    }

    private static void putIfPresent(Map<String, String> out, Map<String, String> env, String key) {
        String v = env.get(key);
        if (v != null && !v.isBlank())
            out.put(key, v);
    }
}
