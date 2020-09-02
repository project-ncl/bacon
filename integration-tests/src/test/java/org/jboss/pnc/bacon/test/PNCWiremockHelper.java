package org.jboss.pnc.bacon.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.ScenarioMappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jboss.pnc.dto.DTOEntity;
import org.jboss.pnc.dto.response.Page;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.jboss.pnc.bacon.test.PNCWiremockHelper.REST;

/**
 *
 * @author jbrazdil
 */
public class PNCWiremockHelper {

    protected static final String REST = "/pnc-rest/v2";

    private static final String TOKEN = "wiremocked-token";

    private static final String TOKEN_MESSAGE = "{" + "  \"access_token\": \"" + TOKEN + "\","
            + "  \"expires_in\": 123456," + "  \"refresh_expires_in\": 123456," + "  \"refresh_token\": \"eee\","
            + "  \"token_type\": \"bearer\"," + "  \"not-before-policy\": 123456789,"
            + "  \"session_state\": \"002f6422-4e1b-4953-8432-e8a3472cd2dc\"," + "  \"scope\": \"profile email\"" + "}";

    private ObjectMapper mapper = new ObjectMapper();

    public PNCWiremockHelper() {
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public void init() {
        stubAnnouncementBanner();
        stubAuthentication();
        stubUnauthenticatedAccess();
    }

    private void stubAuthentication() {
        stubFor(
                WireMock.post(urlMatching("/auth/realms/.*/protocol/openid-connect/token"))
                        .willReturn(okJson(TOKEN_MESSAGE)));
    }

    private void stubAnnouncementBanner() {
        stubFor(_get("/generic-setting/announcement-banner").willReturn(okJson("{\"banner\":\"\"}")));
    }

    private void stubUnauthenticatedAccess() {
        stubFor(
                WireMock.post(urlMatching(REST + "/.*"))
                        .withHeader("Authorization", absent())
                        .willReturn(
                                aResponse().withStatus(401)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("Unauthorized")));
        stubFor(
                WireMock.put(urlMatching(REST + "/.*"))
                        .withHeader("Authorization", absent())
                        .willReturn(
                                aResponse().withStatus(401)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("Unauthorized")));
        stubFor(
                WireMock.patch(urlMatching(REST + "/.*"))
                        .withHeader("Authorization", absent())
                        .willReturn(
                                aResponse().withStatus(401)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("Unauthorized")));
        stubFor(
                WireMock.delete(urlMatching(REST + "/.*"))
                        .withHeader("Authorization", absent())
                        .willReturn(
                                aResponse().withStatus(401)
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("Unauthorized")));
    }

    private MappingBuilder _get(String endpoint) {
        return WireMock.get(urlPathEqualTo(REST + endpoint));
    }

    private MappingBuilder _post(String endpoint) {
        return WireMock.post(urlPathEqualTo(REST + endpoint)).withHeader("Authorization", containing(TOKEN));
    }

    private MappingBuilder _put(String endpoint) {
        return WireMock.put(urlPathEqualTo(REST + endpoint)).withHeader("Authorization", containing(TOKEN));
    }

    private MappingBuilder _delete(String endpoint) {
        return WireMock.delete(urlPathEqualTo(REST + endpoint)).withHeader("Authorization", containing(TOKEN));
    }

    private ResponseDefinitionBuilder _jsonResponse(ResponseDefinitionBuilder builder, Object response)
            throws JsonProcessingException {
        return builder.withHeader("Content-Type", "application/json").withBody(mapper.writeValueAsString(response));
    }

    public void creation(String endpoint, Object response) throws JsonProcessingException {
        stubFor(_post(endpoint).willReturn(_jsonResponse(created(), response)));
    }

    public void get(String endpoint, DTOEntity response) throws JsonProcessingException {
        String id = Objects.requireNonNull(response.getId(), "DTO id must be set");
        stubFor(_get(endpoint + "/" + id).willReturn(_jsonResponse(ok(), response)));
    }

    public void get(String endpoint, String id) throws JsonProcessingException {
        stubFor(_get(endpoint + "/" + id).willReturn(notFound()));
    }

    public void list(String endpoint, Page<?> responsePage) throws JsonProcessingException {
        stubFor(_get(endpoint).willReturn(_jsonResponse(ok(), responsePage)));
    }

    private static final String SC_UPDATE = " update scenario";

    public <T extends DTOEntity> void update(String endpoint, T oldResponse, T updatedResponse)
            throws JsonProcessingException {
        String id = Objects.requireNonNull(oldResponse.getId(), "DTO id must be set");
        scenario(endpoint + SC_UPDATE).getEntity(endpoint, oldResponse)
                .when()
                .put(endpoint + "/" + id)
                .then()
                .getEntity(endpoint, updatedResponse);
    }

    /**
     * Creates a stateful scenario where an endpoint can return different results when something happens.
     * <p>
     * Example: <br>
     * {@code scenario("new scenario").get("/foo/1", oldBody).when().post("/foo").then().get("/foo/1", newBody);} <br>
     * The endpoint "/foo/1" will be returning oldBody untill a post request is sent to "/foo" endpoind. After that, the
     * endpoint "/foo/1" will be returning newBody.
     * </p>
     *
     * @param scenarioName Name of the scanario. Must be unique.
     * @return Scenario builder used to construct the scenario.
     */
    public ScenarioBuilder scenario(String scenarioName) {
        return new ScenarioBuilder(scenarioName);
    }

    public class ScenarioBuilder {
        private final String scenarioName;
        private String mockingState = STARTED;
        private Optional<String> nextState = Optional.empty();

        private ScenarioBuilder(String scenarioName) {
            this.scenarioName = scenarioName;
        }

        public ScenarioBuilder when() {
            nextState.ifPresent(e -> {
                throw new IllegalStateException(
                        "Defining another scenario state when curent definition is not finished yet.");
            });
            nextState = Optional.of(UUID.randomUUID().toString());
            return this;
        }

        public ScenarioBuilder then() {
            mockingState = nextState.orElseThrow(
                    () -> new IllegalStateException("Transiting to next scenario state when not defining new one."));
            return this;
        }

        public ScenarioBuilder get(String endpoint, ResponseDefinitionBuilder response) throws JsonProcessingException {
            ScenarioMappingBuilder builder = _get(endpoint).inScenario(scenarioName)
                    .whenScenarioStateIs(mockingState)
                    .willReturn(response);
            nextState.ifPresent(builder::willSetStateTo);
            stubFor(builder);
            return this;
        }

        public ScenarioBuilder post(String endpoint, ResponseDefinitionBuilder response) {
            ScenarioMappingBuilder builder = _post(endpoint).inScenario(scenarioName)
                    .whenScenarioStateIs(mockingState)
                    .willReturn(response);
            nextState.ifPresent(builder::willSetStateTo);
            stubFor(builder);
            return this;
        }

        public ScenarioBuilder put(String endpoint, ResponseDefinitionBuilder response) {
            ScenarioMappingBuilder builder = _put(endpoint).inScenario(scenarioName)
                    .whenScenarioStateIs(mockingState)
                    .willReturn(response);
            nextState.ifPresent(builder::willSetStateTo);
            stubFor(builder);
            return this;
        }

        public ScenarioBuilder delete(String endpoint, ResponseDefinitionBuilder response) {
            ScenarioMappingBuilder builder = _delete(endpoint).inScenario(scenarioName)
                    .whenScenarioStateIs(mockingState)
                    .willReturn(response);
            nextState.ifPresent(builder::willSetStateTo);
            stubFor(builder);
            return this;
        }

        public ScenarioBuilder post(String endpoint) {
            return post(endpoint, aResponse().withStatus(204));
        }

        public ScenarioBuilder put(String endpoint) {
            return put(endpoint, aResponse().withStatus(204));
        }

        public ScenarioBuilder getEntity(String endpoint, DTOEntity response) throws JsonProcessingException {
            String id = Objects.requireNonNull(response.getId(), "DTO id must be set");
            return get(endpoint + "/" + id, _jsonResponse(ok(), response));
        }

    }
}
