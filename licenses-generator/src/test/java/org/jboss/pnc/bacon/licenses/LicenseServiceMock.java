/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
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
package org.jboss.pnc.bacon.licenses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.undertow.Undertow;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 10/23/17
 */
public class LicenseServiceMock {

    public static final String LICENSES = "LICENSES";
    private static final String responseTemplate = "[\n" + "  {\n" + "    \"project\": \"antlr2:2.7.7.redhat-7\",\n"
            + "    \"root pom GAV\": \"antlr:antlr:2.7.7.redhat-7\",\n" + "    \"scope\": \"PROJECT\",\n"
            + "    \"SCM info\": [\n" + "      {\n" + "        \"name\": \"internal\",\n"
            + "        \"type\": \"git\",\n" + "        \"url\": \"git://git.app.eng.bos.redhat.com/antlr2.git\",\n"
            + "        \"revision\": \"bd0ab97\"\n" + "      }\n" + "    ],\n" + "    \"licenses\": [\n" + "      "
            + LICENSES + " " + "    ],\n" + "    \"license determination type\": {\n" + "      \"id\": \"14\",\n"
            + "      \"name\": \"License file, readme file, missing in pom file\"\n" + "    },\n"
            + "    \"license determination hints\": [\n" + "      {\n" + "        \"name\": \"license file\",\n"
            + "        \"values\": [\n" + "          \"LICENSE.txt\"\n" + "        ]\n" + "      },\n" + "      {\n"
            + "        \"name\": \"pom file\",\n" + "        \"value\": \"pom.xml\"\n" + "      },\n" + "      {\n"
            + "        \"name\": \"readme file\",\n" + "        \"values\": [\n" + "          \"README.txt:2:33\"\n"
            + "        ]\n" + "      }\n" + "    ],\n" + "    \"determined by\": \"mminar\",\n"
            + "    \"determined date\": \"2017-07-19\"\n" + "  }\n" + "]";

    private final Map<String, String> licensesPerGav = new HashMap<>();
    private Undertow server;

    public void addLicenses(String gav, String... licenses) {
        String licensesAsString = Arrays.stream(licenses).collect(Collectors.joining(", "));
        String licenseData = responseTemplate.replace(LICENSES, licensesAsString);
        licensesPerGav.put(gav, licenseData);
    }

    /**
     * expose endpoint at given path
     *
     * @return port number
     */
    public String start(String path) {
        server = Undertow.builder().addHttpListener(0, "localhost").setHandler(exchange -> {
            if (!path.equals(exchange.getRelativePath())) {
                exchange.setStatusCode(404);
            }
            String gav = exchange.getQueryParameters().get("gav").getFirst();
            String maybeLicenseInfo = licensesPerGav.get(gav);
            if (maybeLicenseInfo == null) {
                exchange.setStatusCode(404);
            } else {
                exchange.setStatusCode(200);
                exchange.getResponseSender().send(maybeLicenseInfo);
            }
        }).build();
        server.start();
        return constructUrl(path);
    }

    private String constructUrl(String path) {
        Undertow.ListenerInfo listener = server.getListenerInfo().get(0);
        return listener.getProtcol() + ":/" + listener.getAddress().toString() + path;
    }

    public void stop() {
        server.stop();
    }
}
