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
package org.jboss.pnc.bacon.auth.spi;

import org.jboss.pnc.bacon.auth.KeycloakClientException;
import org.jboss.pnc.bacon.auth.model.Credential;

public interface KeycloakClient {

    /**
     * Authenticate based on username and password
     *
     * @param keycloakBaseUrl
     * @param realm
     * @param client
     * @param username
     *
     * @return Credential object that contains all the information
     */
    Credential getCredential(String keycloakBaseUrl, String realm, String client, String username)
            throws KeycloakClientException;

    /**
     * Authenticate based on service account and service account secret
     *
     * @param keycloakBaseUrl
     * @param realm
     * @param serviceAccountUsername
     * @param secret
     *
     * @return Credential object that contains all the information
     */
    Credential getCredentialServiceAccount(
            String keycloakBaseUrl,
            String realm,
            String serviceAccountUsername,
            String secret) throws KeycloakClientException;

    default String keycloakEndpoint(String keycloakBaseUrl, String realm) {

        String keycloakUrl = keycloakBaseUrl;

        if (!keycloakBaseUrl.endsWith("/")) {
            keycloakUrl = keycloakBaseUrl + "/";
        }

        StringBuilder builder = new StringBuilder();

        builder.append(keycloakUrl).append("auth/realms/").append(realm).append("/protocol/openid-connect/token");

        return builder.toString();
    }
}
