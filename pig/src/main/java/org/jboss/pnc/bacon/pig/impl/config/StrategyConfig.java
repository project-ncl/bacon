package org.jboss.pnc.bacon.pig.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StrategyConfig {
    private String dependencyOverride;
    private List<String> ranks = new ArrayList<>();
    private String denyList;
    private String allowList;
}
