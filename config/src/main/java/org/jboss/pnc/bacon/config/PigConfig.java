package org.jboss.pnc.bacon.config;

import lombok.Data;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com 2020-06-05
 */
@Data
public class PigConfig implements Validate {

    private String kojiHubUrl;
    private String licenseServiceUrl;
    private String indyUrl;

    @Override
    public void validate() {
        Validate.validateUrl(kojiHubUrl, "KojiHub URL");
        Validate.validateUrl(licenseServiceUrl, "License Service URL");
        Validate.validateUrl(indyUrl, "Indy URL");
    }

}
