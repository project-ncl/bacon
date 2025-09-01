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

public class KeycloakConfig implements Validate {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KeycloakConfig.class);
    // Url of Keycloak server
    private String url;
    // Realm of keycloak
    private String realm;
    // Client to use to authenticate. Only used for regular users, not for service accounts
    private String clientId;
    // username: can be a regular user or service account
    private String username;
    // clientSecret used if user is a service account
    private String clientSecret;

    public boolean isServiceAccount() {
        return clientSecret != null && !clientSecret.isEmpty();
    }

    @Override
    public void validate() {
        Validate.validateUrl(url, "Keycloak");
        Validate.checkIfNull(realm, "The Keycloak realm has to be specified");
        Validate.checkIfNull(username, "The username in the Keycloak config has to be specified");
        if (isServiceAccount()) {
            log.debug("clientSecret is in the config file! Assuming this is a service account");
        } else {
            log.debug("clientSecret is not specified in the config file! Assuming this is a regular user");
            Validate.checkIfNull(
                    clientId,
                    "You need to specify the client id for your regular account in the config file");
        }
    }

    @java.lang.SuppressWarnings("all")
    public KeycloakConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getUrl() {
        return this.url;
    }

    @java.lang.SuppressWarnings("all")
    public String getRealm() {
        return this.realm;
    }

    @java.lang.SuppressWarnings("all")
    public String getClientId() {
        return this.clientId;
    }

    @java.lang.SuppressWarnings("all")
    public String getUsername() {
        return this.username;
    }

    @java.lang.SuppressWarnings("all")
    public String getClientSecret() {
        return this.clientSecret;
    }

    @java.lang.SuppressWarnings("all")
    public void setUrl(final String url) {
        this.url = url;
    }

    @java.lang.SuppressWarnings("all")
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @java.lang.SuppressWarnings("all")
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsername(final String username) {
        this.username = username;
    }

    @java.lang.SuppressWarnings("all")
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof KeycloakConfig))
            return false;
        final KeycloakConfig other = (KeycloakConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$url = this.getUrl();
        final java.lang.Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url))
            return false;
        final java.lang.Object this$realm = this.getRealm();
        final java.lang.Object other$realm = other.getRealm();
        if (this$realm == null ? other$realm != null : !this$realm.equals(other$realm))
            return false;
        final java.lang.Object this$clientId = this.getClientId();
        final java.lang.Object other$clientId = other.getClientId();
        if (this$clientId == null ? other$clientId != null : !this$clientId.equals(other$clientId))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$clientSecret = this.getClientSecret();
        final java.lang.Object other$clientSecret = other.getClientSecret();
        if (this$clientSecret == null ? other$clientSecret != null : !this$clientSecret.equals(other$clientSecret))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof KeycloakConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final java.lang.Object $realm = this.getRealm();
        result = result * PRIME + ($realm == null ? 43 : $realm.hashCode());
        final java.lang.Object $clientId = this.getClientId();
        result = result * PRIME + ($clientId == null ? 43 : $clientId.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $clientSecret = this.getClientSecret();
        result = result * PRIME + ($clientSecret == null ? 43 : $clientSecret.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "KeycloakConfig(url=" + this.getUrl() + ", realm=" + this.getRealm() + ", clientId=" + this.getClientId()
                + ", username=" + this.getUsername() + ", clientSecret=" + this.getClientSecret() + ")";
    }
}
