package org.jboss.pnc.bacon.auth.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
@JsonDeserialize(builder = Credential.Builder.class)
@Slf4j
public class Credential {

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
    }
}
