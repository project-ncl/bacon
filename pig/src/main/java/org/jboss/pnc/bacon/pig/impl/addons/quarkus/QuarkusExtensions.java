package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import lombok.Data;

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/23/20
 */
@Data
public class QuarkusExtensions {
    private List<QuarkusExtension> extensions;
}
