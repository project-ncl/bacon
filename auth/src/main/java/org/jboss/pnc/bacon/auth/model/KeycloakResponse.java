package org.jboss.pnc.bacon.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * Unit in second
     */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * Unit in second
     */
    @JsonProperty("refresh_expires_in")
    private int refreshExpiresIn;

    @java.lang.SuppressWarnings("all")
    public KeycloakResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public String getAccessToken() {
        return this.accessToken;
    }

    @java.lang.SuppressWarnings("all")
    public String getRefreshToken() {
        return this.refreshToken;
    }

    /**
     * Unit in second
     */
    @java.lang.SuppressWarnings("all")
    public int getExpiresIn() {
        return this.expiresIn;
    }

    /**
     * Unit in second
     */
    @java.lang.SuppressWarnings("all")
    public int getRefreshExpiresIn() {
        return this.refreshExpiresIn;
    }

    @JsonProperty("access_token")
    @java.lang.SuppressWarnings("all")
    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    @JsonProperty("refresh_token")
    @java.lang.SuppressWarnings("all")
    public void setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Unit in second
     */
    @JsonProperty("expires_in")
    @java.lang.SuppressWarnings("all")
    public void setExpiresIn(final int expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Unit in second
     */
    @JsonProperty("refresh_expires_in")
    @java.lang.SuppressWarnings("all")
    public void setRefreshExpiresIn(final int refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof KeycloakResponse))
            return false;
        final KeycloakResponse other = (KeycloakResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.getExpiresIn() != other.getExpiresIn())
            return false;
        if (this.getRefreshExpiresIn() != other.getRefreshExpiresIn())
            return false;
        final java.lang.Object this$accessToken = this.getAccessToken();
        final java.lang.Object other$accessToken = other.getAccessToken();
        if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken))
            return false;
        final java.lang.Object this$refreshToken = this.getRefreshToken();
        final java.lang.Object other$refreshToken = other.getRefreshToken();
        if (this$refreshToken == null ? other$refreshToken != null : !this$refreshToken.equals(other$refreshToken))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof KeycloakResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getExpiresIn();
        result = result * PRIME + this.getRefreshExpiresIn();
        final java.lang.Object $accessToken = this.getAccessToken();
        result = result * PRIME + ($accessToken == null ? 43 : $accessToken.hashCode());
        final java.lang.Object $refreshToken = this.getRefreshToken();
        result = result * PRIME + ($refreshToken == null ? 43 : $refreshToken.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "KeycloakResponse(accessToken=" + this.getAccessToken() + ", refreshToken=" + this.getRefreshToken()
                + ", expiresIn=" + this.getExpiresIn() + ", refreshExpiresIn=" + this.getRefreshExpiresIn() + ")";
    }
}
