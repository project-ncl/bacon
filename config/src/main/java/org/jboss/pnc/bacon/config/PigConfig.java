package org.jboss.pnc.bacon.config;

import lombok.Data;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com 2020-06-05
 */
@Data
public class PigConfig {
    private String kojiHubUrl;
    private String licenseServiceUrl;
    private String indyUrl;
}
