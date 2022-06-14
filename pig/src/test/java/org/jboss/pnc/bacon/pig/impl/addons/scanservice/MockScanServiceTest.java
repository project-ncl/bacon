package org.jboss.pnc.bacon.pig.impl.addons.scanservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.jboss.pnc.bacon.pig.impl.addons.scanservice.pssaas.ScanHelper;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.*;

import javax.ws.rs.core.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MockScanServiceTest {

    private WireMockServer mockServer;
    private WireMockServer redirectServer;

    // Mock Server information
    private static final String WIREMOCK_HOST = "http://localhost";
    private static int WIREMOCK_PORT;
    private static int REDIRECT_PORT;

    // Secret headers PSSaaS takes, and their values
    private static final String HEADER_SECRET_KEY = "PSSC-Secret-Key";
    private static final String HEADER_SECRET_VALUE = "PSSC-Secret-Value";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    // Scan Invoke Request Information
    private static final String SCAN_INVOKE_URL = "/";
    private static final String SCAN_INVOKE_REQ_BODY_JSON = "{    \"product-id\": \"jochrist-dev-test-hawtio\",    \"is-managed-service\": false,    \"cpaas-version\": \"latest\",    \"component-list\": [        {            \"type\": \"pnc\",            \"build-id\": \"101305\"        }    ]}";
    private static final String SCAN_INVOKE_RESPONSE_BODY = "{\"eventListener\":\"pssc-scan-listener\",\"namespace\":\"pssaas-service\",\"eventListenerUID\":\"d67eafe1-d0dc-4b37-9395-a574afbae1f7\",\"eventID\":\"6a730728-8226-4c02-bc04-bb4c66b9d9c4\"}";
    private static final int SCAN_INVOKE_RESPONSE_STATUS = 202;

    // For PNC Build dummy data
    static EasyRandom easyRandom = new EasyRandom();

    @BeforeAll
    private void startMockService() {
        mockServer = new WireMockServer(options().dynamicPort());
        mockServer.start();
        WIREMOCK_PORT = mockServer.port();
        redirectServer = new WireMockServer(options().dynamicPort());
        redirectServer.start();
        REDIRECT_PORT = redirectServer.port();
        System.out.println("Wiremock ports: Main->" + mockServer.port() + " Redirect->" + redirectServer.port());
    }

    @BeforeEach
    private void clearStubs() {
        // remove stubs before each test
        mockServer.resetAll();
        redirectServer.resetAll();
    }

    private void stubForScanInvocation(WireMockServer mockServer) {
        // Stub for invoking a scan with PSSaaS
        StubMapping scanInvokeStubMapping = mockServer.stubFor(
                post(urlEqualTo(SCAN_INVOKE_URL)).withHeader(HEADER_SECRET_KEY, matching(".*?"))
                        .withHeader(HEADER_SECRET_VALUE, matching(".*?"))
                        .withRequestBody(equalToJson(SCAN_INVOKE_REQ_BODY_JSON, true, true)) // checks if valid json and
                        // contains all expected
                        // elements, extra elements
                        // can exist, order can
                        // differ
                        .withRequestBody(matchingJsonPath("$.product-id")) // does product-id property exist in the
                        // request body?
                        .withRequestBody(matchingJsonPath("$.component-list[?(@.type)]")) // Do type and build-id exist
                        // in the request body?
                        .withRequestBody(matchingJsonPath("$.component-list[?(@.build-id)]"))
                        .willReturn(
                                aResponse().withBody(SCAN_INVOKE_RESPONSE_BODY)
                                        .withStatus(SCAN_INVOKE_RESPONSE_STATUS)
                                        .withHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)));
        // System.out.println(scanInvokeStubMapping.toString()); // To check if stub correctly mapped
        System.out.println("Scanner invoke stub has been set up");
    }

    private void stubUnauthorized() {
        mockServer.stubFor(
                post(urlMatching(SCAN_INVOKE_URL)).willReturn(aResponse().withStatus(401).withBody("Unauthorized")));
        System.out.println("Unauthorized stub has been set up");
    }

    private void stubTemporaryRedirect() {
        stubForScanInvocation(redirectServer);
        mockServer.stubFor(
                post(urlMatching(SCAN_INVOKE_URL)).willReturn(temporaryRedirect(WIREMOCK_HOST + ":" + REDIRECT_PORT)));
        System.out.println("Temporary redirect stub has been set up");
    }

    private void stubServiceUnavailable() {
        mockServer.stubFor(
                post(urlMatching(SCAN_INVOKE_URL))
                        .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));
        System.out.println("Service unavailable stub has been set up");
    }

    @AfterAll
    private void shutDownMockService() {
        System.out.println("Unmatched requests for main Wiremock:");
        logAllUnmatchedRequests(mockServer); // log unmatched requests made to mock service if any before closing up
        System.out.println("Unmatched requests for redirect Wiremock:");
        logAllUnmatchedRequests(redirectServer);
        mockServer.stop();
        redirectServer.stop();
    }

    private Response makeRequestWithRESTClient(String url) {
        PncBuild b = easyRandom.nextObject(PncBuild.class);
        b.setId("101305");
        b.setInternalScmUrl("https://github.com/michalszynkiewicz/demo-bar");
        b.setScmRevision("1.0.1");

        ScanServiceDTO ss = ScanServiceDTOTest.dummyScanService();
        ss.setProductId("jochrist-dev-test-hawtio");
        ss.addPncBuild(b);

        ScanHelper sh = null;
        Response response = null;
        try {
            sh = new ScanHelper("PSSC-Secret-Key", "PSSC-Secret-Value", new URI(url));
            response = sh.client.triggerScan(ss);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Test
    void testScanningServiceInvocation() throws IOException {

        stubForScanInvocation(mockServer);

        Response response = makeRequestWithRESTClient(WIREMOCK_HOST + ":" + WIREMOCK_PORT);

        verifyCorrectRequestMadeForScanInvocation(mockServer);

        String responseBody = response.readEntity(String.class);
        assertEquals(SCAN_INVOKE_RESPONSE_STATUS, response.getStatus());
        assertEquals(CONTENT_TYPE_JSON, response.getHeaderString(HEADER_CONTENT_TYPE));
        try {
            Assertions.assertTrue(
                    twoJsonStringsAreEqual(SCAN_INVOKE_RESPONSE_BODY, responseBody),
                    "Response body is not the expected one");
        } catch (JsonProcessingException e) {
            assertTrue(false, "Json Parsing Error: Please check the format of the response body");
        }
    }

    @Test
    void testTemporaryRedirect() {
        stubTemporaryRedirect();
        Response response = makeRequestWithRESTClient(WIREMOCK_HOST + ":" + WIREMOCK_PORT);
        assertEquals(302, response.getStatus());
        // verify that a request was sent to the redirect server after getting a 302 from the main one
        verifyCorrectRequestMadeForScanInvocation(redirectServer);
    }

    @Test
    void testUnauthorized() {
        stubUnauthorized();
        Response response = makeRequestWithRESTClient(WIREMOCK_HOST + ":" + WIREMOCK_PORT);
        assertEquals(401, response.getStatus());
    }

    @Test
    void testServiceUnavailable() {
        stubServiceUnavailable();
        Response response = makeRequestWithRESTClient(WIREMOCK_HOST + ":" + WIREMOCK_PORT);
        assertEquals(503, response.getStatus());
    }

    // For feedback in case something goes wrong, we can verify here if the request made was matching
    // the requirements of the mock service. Though this causes almost duplicate code with the stubs
    private void verifyCorrectRequestMadeForScanInvocation(WireMockServer mockServer) {
        mockServer.verify(
                postRequestedFor(urlEqualTo(SCAN_INVOKE_URL)).withHeader(HEADER_SECRET_KEY, matching(".*?"))
                        .withHeader(HEADER_SECRET_VALUE, matching(".*?"))
                        .withRequestBody(matchingJsonPath("$.product-id"))
                        .withRequestBody(equalToJson(SCAN_INVOKE_REQ_BODY_JSON, true, true))
                        .withRequestBody(matchingJsonPath("$.component-list[?(@.type)]"))
                        .withRequestBody(matchingJsonPath("$.component-list[?(@.build-id)]")));
    }

    // gives an opportunity to see all unmatched requests made to the mock service for debugging
    private void logAllUnmatchedRequests(WireMockServer wireMockServer) {
        List<LoggedRequest> unmatchedRequests = wireMockServer.findAllUnmatchedRequests();
        if (unmatchedRequests.isEmpty())
            System.out.println("No unmatched requests found");
        else
            System.out.println("Unmatched requests made to mock service: ");
        for (int i = 0; i < unmatchedRequests.size(); i++)
            System.out.println(unmatchedRequests.get(i).toString());
    }

    private boolean twoJsonStringsAreEqual(String json1, String json2) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json1).equals(mapper.readTree(json2));
    }

}
