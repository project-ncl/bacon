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

        if (accessToken == null || refreshToken == null || accessTokenExpiresIn == null
                || refreshTokenExpiresIn == null) {
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
