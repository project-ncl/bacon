package org.jboss.pnc.bacon.auth.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@ToString
public class Credential {

    private String keycloakBaseUrl;

    private String realm;

    private String client;

    private String username;

    private String accessToken;
    private String refreshToken;

    private Date accessTokenTime;
    private Date refreshTokenTime;
}
