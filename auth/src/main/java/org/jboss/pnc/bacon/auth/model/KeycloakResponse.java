package org.jboss.pnc.bacon.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakResponse {

    @JsonProperty(value = "access_token")
    private String accessToken;

    @JsonProperty(value = "refresh_token")
    private String refreshToken;

    /** Unit in second */
    @JsonProperty(value = "expires_in")
    private int expiresIn;

    /** Unit in second */
    @JsonProperty(value = "refresh_expires_in")
    private int refreshExpiresIn;
}
