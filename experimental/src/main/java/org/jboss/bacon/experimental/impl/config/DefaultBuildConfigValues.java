package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.jboss.pnc.dto.validation.constraints.SCMUrl;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DefaultBuildConfigValues {
    @NotNull
    private String environmentName;
    @NotEmpty
    private String buildScript = "mvn deploy";
    @SCMUrl(message = "must be valid git url")
    private String scmUrl = "https://github.com/michalszynkiewicz/empty.git";
    @NotEmpty
    private String scmRevision = "master";
}
