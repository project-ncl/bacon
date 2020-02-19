package org.jboss.pnc.bacon.config;

import utils.Validator;

import java.net.URI;
import java.net.URISyntaxException;

public interface Validate {

    /**
     * WARNING: If validation fails, the method may stop the application
     */
    void validate();

    /**
     * WARNING: If validation fails, the method will stop the application
     *
     * Checks if url is null or empty and has the proper format
     */
    static void validateUrl(String url, String kind) {

        if (url == null || url.isEmpty()) {
            Validator.fail(kind + " Url is not specified in the config file!");
        }

        try {
            URI uri = new URI(url);

            Validator.checkIfNull(uri.getScheme(),
                    "You need to specify the protocol of the " + kind + " URL in the config file");
            Validator.checkIfNull(uri.getHost(), "You need to specify the host of the " + kind + " URL in the config file");

        } catch (URISyntaxException e) {
            Validator.fail("Could not parse the " + kind + " Url at all! " + e.getMessage());
        }
    }

}
