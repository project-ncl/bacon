package org.jboss.bacon.tempname.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;

import java.util.LinkedHashMap;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BuildConfigGeneratorConfig {
    private PigConfiguration pigTemplate;
    private Set<String> scmReplaceWithPlaceholder = Set.of();
    private LinkedHashMap<String, String> scmPattern = new LinkedHashMap<>();
    private LinkedHashMap<String, String> scmMapping = new LinkedHashMap<>();
    private String placeholderSCMUrl = "https://github.com/michalszynkiewicz/empty.git";
    private String placeholderSCMRevision = "master";
    private boolean failGeneratedBuildScript = true;
}
