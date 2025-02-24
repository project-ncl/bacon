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

import java.util.Map;

import lombok.Data;

@Data
public class ConfigProfile {

    private String name;
    private String keycloakUrl;
    private PncConfig pnc;
    private DaConfig da;
    private IndyConfig indy;
    private PigConfig pig;
    private KeycloakConfig keycloak;
    private AutobuildConfig autobuild;
    private boolean enableExperimental;

    private Map<String, Map<String, ?>> addOns;
}
