package org.jboss.pnc.bacon.auth;

public class OidcClientException extends Exception {
    public OidcClientException(Throwable e) {
        super(e);
    }
}
