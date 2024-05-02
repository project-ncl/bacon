package org.jboss.bacon.experimental.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.jboss.pnc.dto.validation.constraints.SCMUrl;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Default values to be used when generating build config.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DefaultBuildConfigValues {
    /**
     * Name of the environment to be used when generating new build config. Required.
     */
    @NotNull
    private String environmentName;

    /**
     * Build script to be used when generating new build config.
     */
    @NotEmpty
    private String buildScript = "mvn deploy";

    /**
     * Placeholder SCM URL.
     *
     * To be used when no SCM url was found for the project. We need it to generate valid build-config.
     */
    @SCMUrl(message = "must be valid git url")
    private String scmUrl = "https://github.com/michalszynkiewicz/empty.git";

    /**
     * Placeholder SCM revision to be used when using placeholder URL or when SCM revision was not resolved.
     */
    @NotEmpty
    private String scmRevision = "master";
}
