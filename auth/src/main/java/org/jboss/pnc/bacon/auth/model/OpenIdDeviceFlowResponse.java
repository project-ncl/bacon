package org.jboss.pnc.bacon.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenIdDeviceFlowResponse {

    @JsonProperty("device_code")
    private String deviceCode;

    @JsonProperty("user_code")
    private String userCode;

    @JsonProperty("verification_uri")
    private String verificationUrl;

    @JsonProperty("expires_in")
    private int expiresInSeconds;

    // minimum interval in seconds to ping if token available
    @JsonProperty("interval")
    private int intervalSeconds;
}
