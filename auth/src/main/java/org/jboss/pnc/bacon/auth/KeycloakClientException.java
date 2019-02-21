package org.jboss.pnc.bacon.auth;

public class KeycloakClientException extends Exception {
    public KeycloakClientException(Throwable e) {
        super(e);
    }
}
