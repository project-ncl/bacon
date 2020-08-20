package org.jboss.pnc.bacon.pig.impl.utils.indy;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;

public class Indy {
    private static volatile String indyRepoUrl;
    private static volatile String indyTempRepoUrl;

    private Indy() {
    }

    public static String getIndyUrl() {
        if (indyRepoUrl == null) {
            indyRepoUrl = pigUrl() + "api/content/maven/group/static";
        }

        return indyRepoUrl;
    }

    public static String getIndyTempUrl() {
        if (indyTempRepoUrl == null) {
            indyTempRepoUrl = pigUrl() + "api/content/maven/group/temporary-builds";
        }

        return indyTempRepoUrl;
    }

    private static String pigUrl() {
        PigConfig pig = Config.instance().getActiveProfile().getPig();
        String indyUrl = pig.getIndyUrl();
        if (!indyUrl.endsWith("/")) {
            indyUrl = indyUrl + "/";
        }
        return indyUrl;
    }
}
