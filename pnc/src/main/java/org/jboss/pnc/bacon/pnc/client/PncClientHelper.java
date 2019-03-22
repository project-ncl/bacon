/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc.client;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.client.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class PncClientHelper {

    private static Configuration configuration;

    public static Configuration getPncConfiguration() {

        if (configuration == null) {
            setup();
        }
        return configuration;
    }

    public static void setup() {

        Config config = Config.instance();
        String url = config.getPnc().getUrl();

        if (url == null || url.isEmpty()) {
            fail("PNC Url is not specified in the config file!");
        }

        try {
            URI uri = new URI(url);

            failIfNull(uri.getScheme(), "You need to specify the protocol of the PNC URL in the config file");
            failIfNull(uri.getHost(), "You need to specify the host of the PNC URL in the config file");

            configuration = Configuration.builder()
                    .protocol(uri.getScheme())
                    .host(uri.getHost())
                    .pageSize(50)
                    .build();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void fail(String reason) {
        failIfNull(null, reason);
    }

    private static void failIfNull(Object object, String reason) {
        if (object == null) {
            log.error(reason);
            System.exit(1);
        }
    }
}
