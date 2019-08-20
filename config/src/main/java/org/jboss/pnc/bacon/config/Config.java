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
package org.jboss.pnc.bacon.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/14/18
 */
@Data
@Slf4j
public class Config {
    private String keycloakUrl;
    private PncConfig pnc;
    private DaConfig da;
    private IndyConfig indy;
    private KeycloakConfig keycloak;

    private Map<String, Map<String, ?>> addOns;

    private static Config instance;

    public static Config instance() {

        if (instance == null) {

            String configLocation = System.getProperty("config", "config.yaml");

            try {
                Config.initialize(configLocation);
            } catch (IOException e) {
                System.err.println("Configuration file not found. " +
                        "Please create a config file and either name it 'config.yaml' and put it in the working directory" +
                        " or specify it with -Dconfig");
            }
        }
        return instance;
    }

    public static void initialize(String configLocation) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        File configFile = new File(configLocation);

        if (!configFile.exists()) {

            log.error("Config file: '{}' does not exist", configLocation);
            throw new IOException("Config file " + configLocation + " does not exist!");

        } else if (configFile.length() == 0) {

            log.warn("Config file: {} has no content", configLocation);
            instance = new Config();

        } else {

            instance = mapper.readValue(new File(configLocation), Config.class);

        }
    }
}
