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
package org.jboss.pnc.bacon.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class LdapClient {

    private final String ldapUsernamePassword;

    public LdapClient(String ldapUsernamePassword) {
        this.ldapUsernamePassword = ldapUsernamePassword;
    }

    /**
     * Get the token
     *
     * @return access token
     */
    public String getBasicAuthHeaderValue() {
        return "Basic " + Base64.getEncoder().encodeToString(ldapUsernamePassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build the {@code Basic} HTTP {@code Authorization} header value for the given {@code username:password} pair, if
     * one is configured.
     *
     * @param ldapUsernamePassword the {@code username:password} value from the active profile, may be {@code null} or
     *        empty
     * @return the {@code Basic <base64>} header value, or an empty optional when no credentials are configured
     */
    public static Optional<String> basicAuthHeaderValue(String ldapUsernamePassword) {
        if (ldapUsernamePassword == null || ldapUsernamePassword.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LdapClient(ldapUsernamePassword).getBasicAuthHeaderValue());
    }
}
