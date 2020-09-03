package org.jboss.pnc.bacon.pig.impl.utils.indy;

import lombok.experimental.UtilityClass;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;

@UtilityClass
public class Indy {
    private volatile String indyRepoUrl;

    private volatile String indyTempRepoUrl;

    public String getIndyUrl() {
        if (indyRepoUrl == null) {
            indyRepoUrl = pigUrl() + "api/content/maven/group/static";
        }

        return indyRepoUrl;
    }

    public String getIndyTempUrl() {
        if (indyTempRepoUrl == null) {
            indyTempRepoUrl = pigUrl() + "api/content/maven/group/temporary-builds";
        }

        return indyTempRepoUrl;
    }

    private String pigUrl() {
        PigConfig pig = Config.instance().getActiveProfile().getPig();
        String indyUrl = pig.getIndyUrl();
        if (!indyUrl.endsWith("/")) {
            indyUrl = indyUrl + "/";
        }
        return indyUrl;
    }
}
