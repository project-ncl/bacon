package org.jboss.pnc.bacon.pig.impl.addons.scanservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.bacon.config.Validate;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.addons.scanservice.pssaas.ScanHelper;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class PostBuildScanService extends AddOn implements Validate {
    private static final Logger log = LoggerFactory.getLogger(PostBuildScanService.class);
    private static final String ADDON_SCAN_SERVICE_MAP_KEY = "postBuildScanService";

    private static final Set<String> mandatoryItems = new HashSet<>();
    static {
        mandatoryItems.add("productID");
        mandatoryItems.add("serviceUrl");
        mandatoryItems.add("serviceSecretKey");
        mandatoryItems.add("serviceSecretValue");
    }

    private static final Set<String> masterSet = new HashSet<>();
    static {
        masterSet.add("productID");
        masterSet.add("eventId");
        masterSet.add("isManagedService");
        masterSet.add("cpaasVersion");
        masterSet.add("jobUrl");
        masterSet.add("serviceUrl");
        masterSet.add("serviceSecretKey");
        masterSet.add("serviceSecretValue");
        masterSet.add("brewBuilds");
        masterSet.add("extraScmUrls");
        masterSet.add("extraBuilds");
        masterSet.add("scmUrl");
        masterSet.add("scmRevision");
        masterSet.add("name");
    }

    Map<String, ?> postBuildScanConfigMap;

    public PostBuildScanService(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    protected String getName() {
        return ADDON_SCAN_SERVICE_MAP_KEY;
    }

    @Override
    public void trigger() {
        Map<String, Map<String, ?>> addonsMap = pigConfiguration.getAddons();
        postBuildScanConfigMap = addonsMap.get(ADDON_SCAN_SERVICE_MAP_KEY);
        ScanServiceDTO scanServiceDTO = createScanServiceDTO();
        if (scanServiceDTO == null) {
            log.error("Unable to create DTO");
            throw new RuntimeException("Unable to create DTO");
        }
        this.validate();
        sendScanRequest(scanServiceDTO);
    }

    private void sendScanRequest(ScanServiceDTO scanServiceDTO) {
        try {
            ScanHelper pssaasService = new ScanHelper(
                    this.getServiceSecretKey(),
                    this.getServiceSecretValue(),
                    this.getServiceUrl());
            Response response = pssaasService.client.triggerScan(scanServiceDTO);
            response.close();
        } catch (URISyntaxException e) {
            log.error("Invalid URI: {}", e.getMessage());
            throw new RuntimeException("sendScanRequest call failed");
        }
    }

    void validateMapKeys(Map<String, ?> map) throws ConfigMissingException {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!masterSet.contains(key)) {
                Validate.fail("'" + key + "' is not a valid build-config.yaml parameter");
            }
            Object value = entry.getValue();
            if (value instanceof List) {
                List<?> lst = (List<?>) value;
                if (lst.get(0) instanceof Map) {
                    int i = 0;
                    while (i < lst.size()) {
                        Map<String, ?> subMap = (Map<String, ?>) lst.get(i);
                        validateMapKeys(subMap);
                        i++;
                    }
                }
            } else if (value instanceof Map) {
                Map<String, ?> subMap = (Map<String, ?>) value;
                validateMapKeys(subMap);
            }
        }
    }

    void checkPresenceOfMandatoryFields(Map<String, ?> map) throws ConfigMissingException {
        for (String item : mandatoryItems) {
            List<String> items = map.keySet()
                    .stream()
                    .filter(k -> k.trim().contains(item))
                    .collect(Collectors.toList());
            if (items.size() != 1) {
                Validate.fail(" mandatory field '" + item + "' not provided");
            }
        }
    }

    /* Validate our addon config */
    @Override
    public void validate() {
        validateMapKeys(postBuildScanConfigMap);
        checkPresenceOfMandatoryFields(postBuildScanConfigMap);
        Validate.validateUrl((String) postBuildScanConfigMap.get("serviceUrl"), "PSSaaS");
    }

    private ScanServiceDTO createScanServiceDTO() {
        String s = postBuildScanConfigMap.get("productID").toString();
        String productId = s;
        if (s == null || s.isEmpty()) {
            log.error("Missing mandatory parameter Product ID");
            return null;
        }
        s = (String) postBuildScanConfigMap.get("EventId");
        String eventId = (s != null && !s.isEmpty()) ? s : ScanServiceDTO.EVENT_ID_DFT_VALUE;

        s = (String) postBuildScanConfigMap.get("cpaasVersion");
        String cpaasVersion = (s != null && !s.isEmpty()) ? s : ScanServiceDTO.CPAAS_VERSION_DFT_VALUE;

        s = (String) postBuildScanConfigMap.get("jobUrl");
        String jobUrl = (s != null && !s.isEmpty()) ? s : ScanServiceDTO.JOB_URL_DFT_VALUE;

        Boolean b = (Boolean) postBuildScanConfigMap.get("isManagedService");
        Boolean isManagedService = (b != null) ? b : ScanServiceDTO.IS_MANAGED_SERVICE_DFT_VALUE;

        List<Integer> brewBuilds = (List<Integer>) postBuildScanConfigMap.get("brewBuilds");

        List<Map<String, String>> extraSCMURLs = (List<Map<String, String>>) postBuildScanConfigMap.get("extraScmUrls");

        return new ScanServiceDTO(
                productId,
                eventId,
                isManagedService,
                cpaasVersion,
                jobUrl,
                brewBuilds,
                extraSCMURLs,
                builds);
    }

    public static String showDTOfields(ScanServiceDTO ssDTO) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ssDTO);
    }

    private URI getServiceUrl() throws URISyntaxException {
        return new URI((String) postBuildScanConfigMap.get("serviceUrl"));
    }

    private String getServiceSecretKey() {
        return (String) postBuildScanConfigMap.get("serviceSecretKey");
    }

    private String getServiceSecretValue() {
        return (String) postBuildScanConfigMap.get("serviceSecretValue");
    }
}
