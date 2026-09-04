package org.jboss.pnc.bacon.pig.impl.utils.repository;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;

public class RepositoryProvider {

    enum PROVIDER_TYPE {
        INDY, ARTIFACTORY
    };

    private static volatile String repoProvUrl;
    private static volatile String tempRepoProvUrl;

    private RepositoryProvider() {
    }

    public static String getRepoProviderUrl() {
        if (repoProvUrl == null) {
            if (repoProvider() == PROVIDER_TYPE.INDY) {
                repoProvUrl = pigUrl() + "api/content/maven/group/static";
            } else {
                repoProvUrl = pigUrl() + "artifactory/pnc-" + pncInstance() + "mvn-builds-imports";
            }
        }
        return repoProvUrl;
    }

    public static String getTempRepoProviderUrl() {
        if (tempRepoProvUrl == null) {
            if (repoProvider() == PROVIDER_TYPE.INDY) {
                repoProvUrl = pigUrl() + "api/content/maven/hosted/temporary-builds";
            } else {
                tempRepoProvUrl = pigUrl() + "artifactory/pnc-" + pncInstance() + "mvn-ibm-temp-builds";
            }
        }

        return tempRepoProvUrl;
    }

    private static String pigUrl() {
        PigConfig pig = Config.instance().getActiveProfile().getPig();
        String repoUrl = pig.getRepoProviderUrl();
        if (!repoUrl.endsWith("/")) {
            repoUrl = repoUrl + "/";
        }
        return repoUrl;
    }

    private static String pncInstance() {
        String pncurl = Config.instance().getActiveProfile().getPnc().getUrl();
        if (pncurl.contains("orch-devel")) {
            return "devel-";
        }
        if (pncurl.contains("orch-stage")) {
            return "stage-";
        }
        return ""; //prod
    }

    private static PROVIDER_TYPE repoProvider() {
        String repoUrl = Config.instance().getActiveProfile().getPig().getRepoProviderUrl();
        if (repoUrl.contains("artifactory")) {
            return PROVIDER_TYPE.ARTIFACTORY;
        }
        if (repoUrl.contains("indy")) {
            return PROVIDER_TYPE.INDY;
        }
        return PROVIDER_TYPE.ARTIFACTORY; //defaults to artifactory
    }

    public static String getConfiguredRepoSettingsXmlPath(boolean tempBuild) {
        return getConfiguredRepoSettingsXmlPath(tempBuild, false);
    }

    public static String getConfiguredRepoSettingsXmlPath(boolean tempBuild, boolean useLocalM2Cache) {
        String settingsXml;

        String filename;
        if (useLocalM2Cache) {
            filename = "/repo-cache";
        } else {
            filename = "/repo";
        }
        if (tempBuild) {
            settingsXml = ResourceUtils.extractToTmpFile(filename + "-temp-settings.xml", "settings", ".xml")
                    .getAbsolutePath();
        } else {
            settingsXml = ResourceUtils.extractToTmpFile(filename + "-settings.xml", "settings", ".xml")
                    .getAbsolutePath();
        }
        FileUtils.replaceFileString("\\$\\{REPO_TMP_URL}", RepositoryProvider.getTempRepoProviderUrl(), settingsXml);
        FileUtils.replaceFileString("\\$\\{REPO_URL}", RepositoryProvider.getRepoProviderUrl(), settingsXml);
        return settingsXml;
    }
}
