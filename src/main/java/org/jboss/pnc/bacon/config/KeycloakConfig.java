package org.jboss.pnc.bacon.config;

import lombok.Data;

@Data
public class KeycloakConfig {

    // Url of Keycloak server
    private String url;

    // Realm of keycloak
    private String realm;

    // Client to use to authenticate. Can be a regular client or a client service account
    private String clientId;

    // username and password: used if client is a regular client
    private String username;
    private String password;

    // clientSecret used if client is a client service account
    private String clientSecret;
}
