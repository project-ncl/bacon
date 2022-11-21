package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DefaultBuildConfigValues {
    @NotNull
    private String environmentName;
    private String buildScript = "mvn deploy";
    private String scmUrl = "https://github.com/michalszynkiewicz/empty.git";
    private String scmRevision = "master";
}
