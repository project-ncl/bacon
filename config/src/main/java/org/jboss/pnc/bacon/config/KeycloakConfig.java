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
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.common.Fail;

@Data
@Slf4j
public class KeycloakConfig implements Validate {

    // Url of Keycloak server
    private String url;

    // Realm of keycloak
    private String realm;

    // Client to use to authenticate. Only used for regular users, not for service accounts
    private String clientId;

    // username: can be a regular user or service account
    private String username;

    // password: used if regular user
    private String password;

    // clientSecret used if user is a service account
    private String clientSecret;

    public boolean isServiceAccount() {
        return clientSecret != null && !clientSecret.isEmpty();
    }

    public void validate() {

        Validate.validateUrl(url, "Keycloak");

        Fail.failIfNull(realm, "The Keycloak realm has to be specified");
        Fail.failIfNull(username, "The username in the Keycloak config has to be specified");

        if (isServiceAccount()) {
            log.debug("clientSecret is in the config file! Assuming this is a service account");
        } else {
            log.debug("clientSecret is not specified in the config file! Assuming this is a regular user");
            Fail.failIfNull(clientId, "You need to specify the client id for your regular account in the config file");
            Fail.failIfNull(password, "You need to specify the password for your regular account in the config file");
        }

    }
}
