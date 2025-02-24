package org.jboss.pnc.bacon.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.pnc.bacon.auth.spi.KeycloakClient;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.junit.jupiter.api.Test;

class KeycloakClientImplTest {

    @Test
    void testGetServiceAccountFatalExceptionThrownOnTLSCertException() {
        KeycloakClient keycloakClient = new KeycloakClientImpl();
        try {
            keycloakClient.getCredentialServiceAccount(
                    "https://self-signed.badssl.com",
                    "true-love",
                    "bassackwards",
                    "only-me");
        } catch (Exception e) {
            assertSame(FatalException.class, e.getCause().getClass());
        }
    }
}
