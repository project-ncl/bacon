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

import lombok.Data;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/14/18
 */
@Data
public class Config {
    private String keycloakUrl;
    private PncConfig pnc;
    private DaConfig da;
    private IndyConfig indy;
    private KeycloakConfig keycloak;

    private Map<String, Map<String, ?>> addOns;

    private static Config instance;

    public static Config instance() {
        return instance;
    }

    public static void initialize(String configLocation) throws IOException {
        Yaml yaml = new Yaml(new Constructor(Config.class));

        try (InputStream in = new FileInputStream(configLocation)) {
            instance = yaml.load(in);
        }
    }
}
