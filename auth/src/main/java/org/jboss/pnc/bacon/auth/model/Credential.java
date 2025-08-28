package org.jboss.pnc.bacon.auth.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = Credential.Builder.class)
public class Credential {

    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Credential.class);

    private String keycloakBaseUrl;

    private String realm;

    private String client;

    private String username;

    private String accessToken;
    private String refreshToken;

    private Instant accessTokenExpiresIn;
    private Instant refreshTokenExpiresIn;

    @JsonIgnore
    public boolean isRefreshTokenValid() {

        if (accessToken == null || refreshToken == null || accessTokenExpiresIn == null
                || refreshTokenExpiresIn == null) {
            return false;
        } else {
            // return whether we have reached the expiry date or not
            return Instant.now().until(refreshTokenExpiresIn, ChronoUnit.SECONDS) > 0;
        }
    }

    @JsonIgnore
    public boolean isAccessTokenValid() {

        if (!isRefreshTokenValid()) {
            return false;
        } else {
            return Instant.now().until(accessTokenExpiresIn, ChronoUnit.MINUTES) > 1;
        }
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        @java.lang.SuppressWarnings("all")
        private String keycloakBaseUrl;
        @java.lang.SuppressWarnings("all")
        private String realm;
        @java.lang.SuppressWarnings("all")
        private String client;
        @java.lang.SuppressWarnings("all")
        private String username;
        @java.lang.SuppressWarnings("all")
        private String accessToken;
        @java.lang.SuppressWarnings("all")
        private String refreshToken;
        @java.lang.SuppressWarnings("all")
        private Instant accessTokenExpiresIn;
        @java.lang.SuppressWarnings("all")
        private Instant refreshTokenExpiresIn;

        @java.lang.SuppressWarnings("all")
        Builder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder keycloakBaseUrl(final String keycloakBaseUrl) {
            this.keycloakBaseUrl = keycloakBaseUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder realm(final String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder client(final String client) {
            this.client = client;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder accessToken(final String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder refreshToken(final String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder accessTokenExpiresIn(final Instant accessTokenExpiresIn) {
            this.accessTokenExpiresIn = accessTokenExpiresIn;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Credential.Builder refreshTokenExpiresIn(final Instant refreshTokenExpiresIn) {
            this.refreshTokenExpiresIn = refreshTokenExpiresIn;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public Credential build() {
            return new Credential(
                    this.keycloakBaseUrl,
                    this.realm,
                    this.client,
                    this.username,
                    this.accessToken,
                    this.refreshToken,
                    this.accessTokenExpiresIn,
                    this.refreshTokenExpiresIn);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "Credential.Builder(keycloakBaseUrl=" + this.keycloakBaseUrl + ", realm=" + this.realm + ", client="
                    + this.client + ", username=" + this.username + ", accessToken=" + this.accessToken
                    + ", refreshToken=" + this.refreshToken + ", accessTokenExpiresIn=" + this.accessTokenExpiresIn
                    + ", refreshTokenExpiresIn=" + this.refreshTokenExpiresIn + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    Credential(
            final String keycloakBaseUrl,
            final String realm,
            final String client,
            final String username,
            final String accessToken,
            final String refreshToken,
            final Instant accessTokenExpiresIn,
            final Instant refreshTokenExpiresIn) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.realm = realm;
        this.client = client;
        this.username = username;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    @java.lang.SuppressWarnings("all")
    public static Credential.Builder builder() {
        return new Credential.Builder();
    }

    @java.lang.SuppressWarnings("all")
    public Credential.Builder toBuilder() {
        return new Credential.Builder().keycloakBaseUrl(this.keycloakBaseUrl)
                .realm(this.realm)
                .client(this.client)
                .username(this.username)
                .accessToken(this.accessToken)
                .refreshToken(this.refreshToken)
                .accessTokenExpiresIn(this.accessTokenExpiresIn)
                .refreshTokenExpiresIn(this.refreshTokenExpiresIn);
    }

    @java.lang.SuppressWarnings("all")
    public String getKeycloakBaseUrl() {
        return this.keycloakBaseUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getRealm() {
        return this.realm;
    }

    @java.lang.SuppressWarnings("all")
    public String getClient() {
        return this.client;
    }

    @java.lang.SuppressWarnings("all")
    public String getUsername() {
        return this.username;
    }

    @java.lang.SuppressWarnings("all")
    public String getAccessToken() {
        return this.accessToken;
    }

    @java.lang.SuppressWarnings("all")
    public String getRefreshToken() {
        return this.refreshToken;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getAccessTokenExpiresIn() {
        return this.accessTokenExpiresIn;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getRefreshTokenExpiresIn() {
        return this.refreshTokenExpiresIn;
    }

    @java.lang.SuppressWarnings("all")
    public void setKeycloakBaseUrl(final String keycloakBaseUrl) {
        this.keycloakBaseUrl = keycloakBaseUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @java.lang.SuppressWarnings("all")
    public void setClient(final String client) {
        this.client = client;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsername(final String username) {
        this.username = username;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    @java.lang.SuppressWarnings("all")
    public void setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccessTokenExpiresIn(final Instant accessTokenExpiresIn) {
        this.accessTokenExpiresIn = accessTokenExpiresIn;
    }

    @java.lang.SuppressWarnings("all")
    public void setRefreshTokenExpiresIn(final Instant refreshTokenExpiresIn) {
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Credential))
            return false;
        final Credential other = (Credential) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$keycloakBaseUrl = this.getKeycloakBaseUrl();
        final java.lang.Object other$keycloakBaseUrl = other.getKeycloakBaseUrl();
        if (this$keycloakBaseUrl == null ? other$keycloakBaseUrl != null
                : !this$keycloakBaseUrl.equals(other$keycloakBaseUrl))
            return false;
        final java.lang.Object this$realm = this.getRealm();
        final java.lang.Object other$realm = other.getRealm();
        if (this$realm == null ? other$realm != null : !this$realm.equals(other$realm))
            return false;
        final java.lang.Object this$client = this.getClient();
        final java.lang.Object other$client = other.getClient();
        if (this$client == null ? other$client != null : !this$client.equals(other$client))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$accessToken = this.getAccessToken();
        final java.lang.Object other$accessToken = other.getAccessToken();
        if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken))
            return false;
        final java.lang.Object this$refreshToken = this.getRefreshToken();
        final java.lang.Object other$refreshToken = other.getRefreshToken();
        if (this$refreshToken == null ? other$refreshToken != null : !this$refreshToken.equals(other$refreshToken))
            return false;
        final java.lang.Object this$accessTokenExpiresIn = this.getAccessTokenExpiresIn();
        final java.lang.Object other$accessTokenExpiresIn = other.getAccessTokenExpiresIn();
        if (this$accessTokenExpiresIn == null ? other$accessTokenExpiresIn != null
                : !this$accessTokenExpiresIn.equals(other$accessTokenExpiresIn))
            return false;
        final java.lang.Object this$refreshTokenExpiresIn = this.getRefreshTokenExpiresIn();
        final java.lang.Object other$refreshTokenExpiresIn = other.getRefreshTokenExpiresIn();
        if (this$refreshTokenExpiresIn == null ? other$refreshTokenExpiresIn != null
                : !this$refreshTokenExpiresIn.equals(other$refreshTokenExpiresIn))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Credential;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $keycloakBaseUrl = this.getKeycloakBaseUrl();
        result = result * PRIME + ($keycloakBaseUrl == null ? 43 : $keycloakBaseUrl.hashCode());
        final java.lang.Object $realm = this.getRealm();
        result = result * PRIME + ($realm == null ? 43 : $realm.hashCode());
        final java.lang.Object $client = this.getClient();
        result = result * PRIME + ($client == null ? 43 : $client.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $accessToken = this.getAccessToken();
        result = result * PRIME + ($accessToken == null ? 43 : $accessToken.hashCode());
        final java.lang.Object $refreshToken = this.getRefreshToken();
        result = result * PRIME + ($refreshToken == null ? 43 : $refreshToken.hashCode());
        final java.lang.Object $accessTokenExpiresIn = this.getAccessTokenExpiresIn();
        result = result * PRIME + ($accessTokenExpiresIn == null ? 43 : $accessTokenExpiresIn.hashCode());
        final java.lang.Object $refreshTokenExpiresIn = this.getRefreshTokenExpiresIn();
        result = result * PRIME + ($refreshTokenExpiresIn == null ? 43 : $refreshTokenExpiresIn.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Credential(keycloakBaseUrl=" + this.getKeycloakBaseUrl() + ", realm=" + this.getRealm() + ", client="
                + this.getClient() + ", username=" + this.getUsername() + ", accessToken=" + this.getAccessToken()
                + ", refreshToken=" + this.getRefreshToken() + ", accessTokenExpiresIn="
                + this.getAccessTokenExpiresIn() + ", refreshTokenExpiresIn=" + this.getRefreshTokenExpiresIn() + ")";
    }
}
