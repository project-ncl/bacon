package org.jboss.pnc.bacon.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.SSLHandshakeException;

import org.jboss.pnc.bacon.auth.model.OidcCredential;
import org.jboss.pnc.bacon.auth.model.OidcResponse;
import org.jboss.pnc.bacon.auth.model.OpenIdConfiguration;
import org.jboss.pnc.bacon.auth.model.OpenIdDeviceFlowResponse;
import org.jboss.pnc.bacon.auth.spi.OidcClient;
import org.jboss.pnc.bacon.common.exception.FatalException;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: add caching
 * TODO: add getCredential support
 */
@Slf4j
public class OidcClientImpl implements OidcClient {

    private static final int MAX_RETRIES = 10;

    static {
        // configure Unirest to use Jackson
        Unirest.config().setObjectMapper(new JacksonObjectMapper());
    }

    /**
     * Implementation for regular user login using device code flow
     */
    @Override
    public OidcCredential getCredential(String authServerUrl, String client)
            throws OidcClientException {

        String authServerDeviceFlowEndpoint = getOpenIdConfiguration(authServerUrl).getDeviceAuthorizationEndpoint();
        String authServerTokenEndpoint = getOpenIdConfiguration(authServerUrl).getTokenEndpoint();

        // Send the initial request with the client to the the user code and device code
        MultipartBody initialRequest = Unirest.post(authServerDeviceFlowEndpoint)
                .field("client_id", client)
                .field("scope", "");

        OpenIdDeviceFlowResponse deviceFlowResponse = getResponseWithRetries(
                initialRequest,
                OpenIdDeviceFlowResponse.class);

        // the user opens the url, enters the user code to identify the request, and optionally logs-in if not already
        // using System.out, but I'm unsure if I should use log.info instead.
        System.out.println("Open the url in your browser: " + deviceFlowResponse.getVerificationUrl());
        System.out.println("And enter the code: " + deviceFlowResponse.getUserCode());

        // now we poll the token endpoint. If the user entered the code properly, then the token endpoint should
        // return the access token
        Instant firstRequest = null;

        while (true) {
            // initial sleep because users will take time to enter the code
            sleep(deviceFlowResponse.getIntervalSeconds() * 1000L);

            if (firstRequest == null) {
                // note the time we did our first request
                firstRequest = Instant.now();
            }

            MultipartBody polled = Unirest.post(authServerTokenEndpoint)
                    .field("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .field("client_id", client)
                    .field("device_code", deviceFlowResponse.getDeviceCode());

            HttpResponse<OidcResponse> response = polled.asObject(OidcResponse.class);
            if (response.getStatus() == 200) {
                OidcResponse oidcResponse = response.getBody();
                return OidcCredential.builder()
                        .authServerUrl(authServerUrl)
                        .accessToken(oidcResponse.getAccessToken())
                        .refreshToken(oidcResponse.getRefreshToken())
                        .accessTokenExpiresIn(Instant.now().plus(oidcResponse.getExpiresIn(), ChronoUnit.SECONDS))
                        .refreshTokenExpiresIn(
                                Instant.now().plus(oidcResponse.getRefreshExpiresIn(), ChronoUnit.SECONDS))
                        .build();
            } else if (Instant.now().getEpochSecond() - firstRequest.getEpochSecond() > deviceFlowResponse
                    .getExpiresInSeconds()) {
                throw new FatalException("The code is now expired. Giving up");
            } else {
                log.info("Will retry polling for token in: {} seconds", deviceFlowResponse.getIntervalSeconds());
            }
        }
    }

    /**
     * Using the client credentials flow for Machine 2 Machine communication
     *
     * @param authServerUrl
     * @param serviceAccountUsername
     * @param secret
     *
     * @return
     * @throws OidcClientException
     */
    @Override
    public OidcCredential getCredentialServiceAccount(
            String authServerUrl,
            String serviceAccountUsername,
            String secret) throws OidcClientException {

        try {

            log.debug("Getting token via clientServiceAccountUsername / secret");
            String authServerTokenEndpoint = getOpenIdConfiguration(authServerUrl).getTokenEndpoint();

            MultipartBody body = Unirest.post(authServerTokenEndpoint)
                    .field("grant_type", "client_credentials")
                    .field("client_id", serviceAccountUsername)
                    .field("client_secret", secret);

            OidcResponse response = getResponseWithRetries(body, OidcResponse.class);
            Instant now = Instant.now();

            return OidcCredential.builder()
                    .authServerUrl(authServerUrl)
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

    public static OpenIdConfiguration getOpenIdConfiguration(String authServerUrl) {
        String discoveryUrl = authServerUrl + "/.well-known/openid-configuration";
        return getResponseWithRetries(Unirest.get(discoveryUrl), OpenIdConfiguration.class);
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
    private static <T> T getResponseWithRetries(HttpRequest<?> body, Class<T> clazz) throws UnirestException {

        int retries = 0;

        while (true) {
            try {
                HttpResponse<T> postResponse = body.asObject(clazz);
                return postResponse.getBody();
            } catch (UnirestException e) {
                if (e.getCause().getClass().equals(SSLHandshakeException.class)) {
                    throw new FatalException(
                            "Cannot reach the Oidc server because of missing TLS certificates",
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
    private static void sleepExponentially(int retries) {

        long amountOfSleep = (long) (100 * Math.pow(2, retries));
        log.debug("Sleeping for {} seconds", String.format("%.1f", amountOfSleep / 1000.0));
        sleep(amountOfSleep);
    }

    private static void sleep(long sleepInMilliseconds) {
        try {
            Thread.sleep(sleepInMilliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
