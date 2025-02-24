package org.jboss.pnc.bacon.pig.impl.addons.scanservice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.jboss.pnc.bacon.config.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValidationTest {

    PostBuildScanService scanService = new PostBuildScanService(null, null, null, null);

    private static final Map<String, ?> benchmarkScanServiceConfigMap = new LinkedHashMap<>();
    static {
        benchmarkScanServiceConfigMap.put("productID", null);
        benchmarkScanServiceConfigMap.put("eventId", null);
        benchmarkScanServiceConfigMap.put("isManagedService", null);
        benchmarkScanServiceConfigMap.put("cpaasVersion", null);
        benchmarkScanServiceConfigMap.put("jobUrl", null);
        benchmarkScanServiceConfigMap.put("serviceUrl", null);
        benchmarkScanServiceConfigMap.put("serviceSecretKey", null);
        benchmarkScanServiceConfigMap.put("serviceSecretValue", null);
        benchmarkScanServiceConfigMap.put("brewBuilds", null);
        benchmarkScanServiceConfigMap.put("scmUrl", null);
        benchmarkScanServiceConfigMap.put("scmRevision", null);
        benchmarkScanServiceConfigMap.put("extraScmUrls", null);
        benchmarkScanServiceConfigMap.put("extraBuilds", null);
        benchmarkScanServiceConfigMap.put("name", null);
    }

    private static final Map<String, ?> minimalConfigMap = new LinkedHashMap<>();
    static {
        minimalConfigMap.put("productID", null);
        minimalConfigMap.put("serviceUrl", null);
        minimalConfigMap.put("serviceSecretKey", null);
        minimalConfigMap.put("serviceSecretValue", null);
    }

    @Test
    void positiveKeywordsTest() {
        showMap(benchmarkScanServiceConfigMap);
        scanService.validateMapKeys(benchmarkScanServiceConfigMap);
    }

    @ParameterizedTest(name = "{index} => validKey={0}, invalidKey={1}")
    @CsvSource({
            "productID, product-id",
            "eventId, eventID",
            "isManagedService, _isManagedService_",
            "cpaasVersion, CpaasVersion",
            "jobUrl, xyz",
            "serviceUrl, service-url",
            "serviceSecretKey, serviceSecretkey",
            "serviceSecretValue, servicesecretValue",
            "brewBuilds, brewbuilds",
            "scmUrl, SCMURL",
            "scmRevision, scm-revision",
            "extraScmUrls, _xtraScmUrls",
            "extraBuilds, extraBuilds_" })
    void negativeKeywordsTest(String validKey, String invalidKey) {
        HashMap<String, Object> testMap = new LinkedHashMap<>(benchmarkScanServiceConfigMap);
        testMap.remove(validKey);
        testMap.put(invalidKey, null);
        Assertions.assertThrows(Validate.ConfigMissingException.class, () -> scanService.validateMapKeys(testMap));
    }

    @Test
    void positiveMandatoryKeywordsTest() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("productID", 111);
        map.put("serviceUrl", "http://someurl.com:8080");
        map.put("serviceSecretKey", "pig");
        map.put("serviceSecretValue", "sometoken");
        scanService.checkPresenceOfMandatoryFields(map);
    }

    @Test
    void negativeMandatoryKeywordsTest() {

        for (String mk : minimalConfigMap.keySet()) {
            HashMap<String, Object> map = new HashMap<>(minimalConfigMap);
            map.remove(mk);
            Exception e = Assertions.assertThrows(
                    Validate.ConfigMissingException.class,
                    () -> scanService.checkPresenceOfMandatoryFields(map));
            assertTrue(e.getMessage().contains("mandatory field '" + mk));
        }
    }

    void showMap(Map<String, ?> mapToShow) {
        try {
            for (Map.Entry<String, ?> entry : mapToShow.entrySet()) {
                System.out.printf("%30s : %s%n", entry.getKey(), entry.getValue());
            }
            System.out.println(new String(new char[5]).replace("\0", "========="));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
