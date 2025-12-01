package org.jboss.pnc.bacon.auth;

import java.time.Instant;

import javax.net.ssl.SSLHandshakeException;

import org.jboss.pnc.bacon.auth.model.OidcCredential;
import org.jboss.pnc.bacon.auth.model.OidcResponse;
import org.jboss.pnc.bacon.auth.spi.OidcClient;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.util.NotImplementedException;

import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: add caching
 * TODO: add getCredential support
 */
@Slf4j
public class OidcClientImpl implements OidcClient {

    private static final int MAX_RETRIES = 10;

    /**
     * TODO
     * I don't know if we can continue using the manual login option or if we'll have to use authorization flow
     * Manual login option is also known as 'Out-of-bounds' and discouraged and in some cases deprecated
     * The more modern way is to use device code flow, but it depends on whether the Oidc server supports it
     *
     * Another more secure way is to use Authorization code flow, but the user can only use Bacon in the same machine
     * as the browser
     */
    @Override
    public OidcCredential getCredential(String authServerTokenEndpoint, String client, String username)
            throws OidcClientException {
        throw new NotImplementedException();
    }

    /**
     * Using the client credentials flow for Machine 2 Machine communication
     *
     * @param authServerTokenEndpoint
     * @param serviceAccountUsername
     * @param secret
     *
     * @return
     * @throws OidcClientException
     */
    @Override
    public OidcCredential getCredentialServiceAccount(
            String authServerTokenEndpoint,
            String serviceAccountUsername,
            String secret) throws OidcClientException {

        try {

            log.debug("Getting token via clientServiceAccountUsername / secret");

            MultipartBody body = Unirest.post(authServerTokenEndpoint)
                    .field("grant_type", "client_credentials")
                    .field("client_id", serviceAccountUsername)
                    .field("client_secret", secret);

            OidcResponse response = getOidcResponseWithRetries(body);
            Instant now = Instant.now();

            return OidcCredential.builder()
                    .authServerTokenEndpoint(authServerTokenEndpoint)
                    .client(serviceAccountUsername)
                    .accessToken(response.getAccessToken())
                    .accessTokenExpiresIn(now.plusSeconds(response.getExpiresIn()))
                    .refreshToken(response.getRefreshToken())
                    .refreshTokenExpiresIn(now.plusSeconds(response.getRefreshExpiresIn()))
                    .build();

        } catch (Exception e) {
            throw new OidcClientException(e);
        }
    }

    /**
     * Tiny helper method that takes in a Unirest body and submits the request, and parses the response as a
     * KeycloakResponse object. This helper method add retries (up to MAX_RETRIES) if the request fails for whatever
     * reason.
     *
     * This method sleeps exponentially between retries to simulate an exponential backoff, starting at 200ms up to 102
     * seconds
     *
     * @param body Unirest body to send request
     * @return The KeycloakResponse object
     * @throws UnirestException when all hope is lost to recover
     */
    private OidcResponse getOidcResponseWithRetries(MultipartBody body) throws UnirestException {

        int retries = 0;

        while (true) {
            try {
                HttpResponse<OidcResponse> postResponse = body.asObject(OidcResponse.class);
                return postResponse.getBody();
            } catch (UnirestException e) {
                if (e.getCause().getClass().equals(SSLHandshakeException.class)) {
                    throw new FatalException(
                            "Cannot reach the Keycloak server because of missing TLS certificates",
                            e.getCause());
                }
                retries++;

                if (retries > MAX_RETRIES) {
                    // all hope is lost. Stop retrying
                    throw e;
                } else if (retries == MAX_RETRIES / 2) {
                    // let the user know as she waits
                    log.info("Having difficulty reaching {}. Retrying again...", body.getUrl());
                }
                sleepExponentially(retries);
                log.debug("Retrying to reach: {}", body.getUrl());
            }
        }
    }

    /**
     * Sleep starting from 100 milliseconds when retries = 0, and sleeping exponentially as the retries increase
     *
     * The math is millisecondsSleep = 100 * 2^(retries)
     *
     * @param retries number of retries attempted
     */
    private void sleepExponentially(int retries) {

        long amountOfSleep = (long) (100 * Math.pow(2, retries));
        log.debug("Sleeping for {} seconds", String.format("%.1f", amountOfSleep / 1000.0));

        try {
            Thread.sleep(amountOfSleep);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
