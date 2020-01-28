package org.jboss.pnc.bacon.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
    public boolean isValid() {

        if (accessToken == null || refreshToken == null || accessTokenExpiresIn == null || refreshTokenExpiresIn == null) {
            return false;
        } else {
            // return whether we have reached the expiry date or not
            return Instant.now().until(refreshTokenExpiresIn, ChronoUnit.SECONDS) > 0;
        }
    }

    public boolean needsNewAccessToken() {

        if (!isValid()) {
            return true;
        } else {
            return Instant.now().until(accessTokenExpiresIn, ChronoUnit.HOURS) < 20;
        }
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }
}
