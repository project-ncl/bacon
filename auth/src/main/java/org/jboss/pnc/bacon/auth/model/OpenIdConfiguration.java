package org.jboss.pnc.bacon.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenIdConfiguration {

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("device_authorization_endpoint")
    private String deviceAuthorizationEndpoint;
}
