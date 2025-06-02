package org.jboss.pnc.bacon.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class RexConfig implements Validate {

    // Url of Rex server
    private String url;

    @Override
    public void validate() {
        Validate.validateUrl(url, "Rex");
    }
}
