package org.jboss.pnc.bacon.pig.impl.utils.indy;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;

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
            indyTempRepoUrl = pigUrl() + "api/content/maven/hosted/temporary-builds";
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

    public static String getConfiguredIndySettingsXmlPath(boolean tempBuild) {
        return getConfiguredIndySettingsXmlPath(tempBuild, false);
    }

    public static String getConfiguredIndySettingsXmlPath(boolean tempBuild, boolean useLocalM2Cache) {
        String settingsXml;

        String filename;
        if (useLocalM2Cache) {
            filename = "/indy-cache";
        } else {
            filename = "/indy";
        }
        if (tempBuild) {
            settingsXml = ResourceUtils.extractToTmpFile(filename + "-temp-settings.xml", "settings", ".xml")
                    .getAbsolutePath();
        } else {
            settingsXml = ResourceUtils.extractToTmpFile(filename + "-settings.xml", "settings", ".xml")
                    .getAbsolutePath();
        }
        FileUtils.replaceFileString("\\$\\{INDY_TMP_URL}", Indy.getIndyTempUrl(), settingsXml);
        FileUtils.replaceFileString("\\$\\{INDY_URL}", Indy.getIndyUrl(), settingsXml);
        return settingsXml;
    }
}
