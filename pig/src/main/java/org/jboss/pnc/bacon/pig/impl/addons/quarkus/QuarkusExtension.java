package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 19/08/2019
 */
@Data
public class QuarkusExtension {
    @JsonProperty("artifact")
    private String artifact;
}
