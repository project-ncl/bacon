package org.jboss.pnc.bacon.pig.impl.addons.scanservice;

import static java.lang.String.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * PSSC scanning container interface
 * <p>
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "product-id", "event-id", "is-managed-service", "cpaas-version", "job-url", "component-list" })

public class ScanServiceDTO {
    private static final Logger log = LoggerFactory.getLogger(ScanServiceDTO.class);

    public static final String EVENT_ID_DFT_VALUE = "dft-event-id";
    public static final Boolean IS_MANAGED_SERVICE_DFT_VALUE = false;
    public static final String CPAAS_VERSION_DFT_VALUE = "latest";
    public static final String JOB_URL_DFT_VALUE = "dft-job-url";

    /* Default Constructor */
    ScanServiceDTO() {
        eventId = EVENT_ID_DFT_VALUE;
        isManagedService = IS_MANAGED_SERVICE_DFT_VALUE;
        cpaasVersion = CPAAS_VERSION_DFT_VALUE;
        jobUrl = JOB_URL_DFT_VALUE;
    }

    /**
     * For each build the type (brew, pnc or git [scmurl])
     */
    public enum buildType {
        brew, pnc, git
    }

    /**
     * The product ID associated with the scan. (Required)
     *
     */
    @JsonProperty("product-id")
    @JsonPropertyDescription("The product ID associated with the scan")
    @Getter
    @Setter
    private String productId;

    /**
     * The submission event ID associated with the scan.
     *
     */
    @JsonProperty("event-id")
    @JsonPropertyDescription("The submission event ID associated with the scan")
    @Getter
    @Setter
    private String eventId;

    /**
     * Indicates whether or not the product is a managed service. (Required)
     *
     */
    @JsonProperty("is-managed-service")
    @JsonPropertyDescription("Indicates whether or not the product is a managed service")
    @Getter
    @Setter
    private Boolean isManagedService;

    /**
     * The version of CPaaS that submitted the scan.
     *
     */
    @JsonProperty("cpaas-version")
    @JsonPropertyDescription("The version of CPaaS that submitted the scan")
    @Getter
    @Setter
    private String cpaasVersion;

    /**
     * URL of Jenkins job that submitted the scan
     *
     */
    @JsonProperty("job-url")
    @JsonPropertyDescription("URL of Jenkins job that submitted the scan")
    @Getter
    @Setter
    private String jobUrl;

    /**
     * The Additional Builds associated with the scan
     *
     */
    @JsonProperty("component-list")
    @JsonPropertyDescription("The builds associated with the scan")
    private List<Object> componentList = new ArrayList<>();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     *
     * @param productId (mandatory) The product ID as defined in product pages, this is used for sorting generated scan
     *        reports
     * @param eventId (optional) CPaaS eventId
     * @param isManagedService (optional) If the build is from a Managed Service build
     * @param cpaasVersion (optional) The version of the scan container image to use, this should normally be "latest"
     * @param jobUrl (optional) The pipeline job submitting the scan request
     * @param brewBuilds (optional) Extra list of brew builds (by Integer)
     * @param extraSCMURLs (optional) Extra list of git repos and revisions
     * @param pncBuilds (optional) Extra list of PncBuilds (stub)
     *
     *        For non-provided optional parameters in the build-config.yaml file default values will be passed-in by the
     *        caller of this constructor.
     *
     */

    public ScanServiceDTO(
            String productId,
            String eventId,
            Boolean isManagedService,
            String cpaasVersion,
            String jobUrl,
            List<Integer> brewBuilds,
            List<Map<String, String>> extraSCMURLs,
            Map<String, PncBuild> pncBuilds) {
        super();
        this.productId = productId;
        this.eventId = eventId;
        this.isManagedService = isManagedService;
        this.cpaasVersion = cpaasVersion;
        this.jobUrl = jobUrl;
        this.componentList = fromPncBuilds(pncBuilds);
        if (brewBuilds != null) {
            brewBuilds.stream().forEach(b -> this.addBrewBuild(b));
        }
        if (extraSCMURLs != null) {
            extraSCMURLs.stream().forEach(scm -> this.addSCMURLs(scm));
        }
    }

    private static String safeURL(String repo, String ref) throws UnsupportedEncodingException {
        String url = URLDecoder
                .decode(URLEncoder.encode(repo, StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
        String fragment = URLEncoder.encode(ref, StandardCharsets.UTF_8.toString());
        return format("%s#%s", url, fragment);
    }

    private List<Object> fromPncBuilds(Map<String, PncBuild> builds) {
        return builds.entrySet().stream().map(b -> new ScanServiceDTO.build(b.getValue())).collect(Collectors.toList());
    }

    public void addBrewBuilds(List<Integer> builds) {
        builds.stream().forEach(b -> this.addBrewBuild(b));
    }

    public void addBrewBuild(Integer build) {
        this.componentList.add(new ScanServiceDTO.build(build));
    }

    public void addPncBuilds(Map<String, PncBuild> builds) {
        this.componentList.addAll(fromPncBuilds(builds));
    }

    public void addPncBuild(PncBuild build) {
        this.componentList.add(new ScanServiceDTO.build(build));
    }

    public void addSCMUrl(String scmURL) {
        this.componentList.add(new ScanServiceDTO.git(scmURL));
    }

    public void addSCMUrl(String repo, String ref) {
        try {
            this.componentList.add(new git(repo, ref));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void addSCMURLs(Map<String, String> scmUrls) {
        this.addSCMUrl(scmUrls.get("scmUrl"), scmUrls.get("scmRevision"));
    }

    /**
     * The product ID associated with the scan. (Required)
     *
     */
    public ScanServiceDTO withProductId(String productId) {
        this.productId = productId;
        return this;
    }

    /**
     * The submission event ID associated with the scan
     *
     */
    public ScanServiceDTO withEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    /**
     * Indicates whether or not the product is a managed service
     *
     */
    public ScanServiceDTO withIsManagedService(Boolean isManagedService) {
        this.isManagedService = isManagedService;
        return this;
    }

    /**
     * The version of CPaaS that submitted the scan
     *
     */
    public ScanServiceDTO withCpaasVersion(String cpaasVersion) {
        this.cpaasVersion = cpaasVersion;
        return this;
    }

    /**
     * URL of Jenkins job that submitted the scan
     *
     */
    public ScanServiceDTO withJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public ScanServiceDTO withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    private final class build {
        private build(PncBuild build) {
            this.type = buildType.pnc;
            this.buildId = build.getId();
        }

        private build(Integer brewBuild) {
            this.type = buildType.brew;
            this.buildId = brewBuild.toString();
        }

        @JsonProperty("type")
        private buildType type;

        @JsonProperty("build-id")
        private String buildId;
    }

    private final class git {
        @JsonProperty("type")
        private buildType type;

        @JsonProperty("repo")
        private String repo;

        @JsonProperty("ref")
        private String ref;

        private git(String repo, String ref) throws UnsupportedEncodingException {
            this(ScanServiceDTO.safeURL(repo, ref));
        }

        /*
         * Constructor for git://github.com/foo/bar.git#2.0.0 style SCMURLs and some kind of validation
         */
        private git(String scmUrl) {
            this.type = buildType.git;
            try {
                URI uri = new URI(scmUrl);
                this.ref = uri.getFragment();
                // Strip fragment and use original string rather than building a new one from URI
                int index = scmUrl.indexOf(URLEncoder.encode(this.ref, StandardCharsets.UTF_8.toString()));
                this.repo = scmUrl.substring(0, index - 1);
            } catch (URISyntaxException e) {
                log.error("Invalid scmUrl");
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding in SCMURL");
                throw new RuntimeException(e);
            }
        }
    }
}
