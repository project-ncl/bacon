package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.apache.commons.collections4.CollectionUtils;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.CSVUtils;
import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Harsh Madhani<harshmadhani@gmail.com> Date: 06-August-2020
 */
public class QuarkusPostBuildAnalyzer extends AddOn {

    public static final String NAME = "quarkusPostBuildAnalyzer";

    private static final Logger log = LoggerFactory.getLogger(QuarkusPostBuildAnalyzer.class);

    public QuarkusPostBuildAnalyzer(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    private static void postBuildCheck(String stagingPath, String productName) {
        String stagingPathToProduct = stagingPath + productName + "/";
        String communityDependenciesPath = "/extras/community-dependencies.csv";
        Set<String> oldDependencies;
        Set<String> newDependencies;
        try {
            Document document = Jsoup.connect(stagingPathToProduct + "?C=M;O=D").get();
            Element quarkusLatestBuild = document.select("a[href~=" + productName + "*]").first().parent().parent();

            String latest_build = quarkusLatestBuild.select("a[href]").text().replace("/", "");
            String old_build = quarkusLatestBuild.nextElementSibling().select("a[href]").text().replace("/", "");

            log.info("Latest build is {}", latest_build);
            log.info("Old build is {}", old_build);

            FileDownloadUtils.downloadTo(
                    new URI(stagingPathToProduct + latest_build + communityDependenciesPath),
                    new File("new_dependencies.csv"));
            FileDownloadUtils.downloadTo(
                    new URI(stagingPathToProduct + old_build + communityDependenciesPath),
                    new File("old_dependencies.csv"));

            oldDependencies = CSVUtils.columnValues("Community dependencies", "new_dependencies.csv", ';');
            newDependencies = CSVUtils.columnValues("Community dependencies", "old_dependencies.csv", ';');

            String newBuildInfo = "Community Dependencies present in new build which were not present in old build are "
                    + CollectionUtils.subtract(newDependencies, oldDependencies);
            String oldBuildInfo = "Community Dependencies present in old build which are not present in new build are "
                    + CollectionUtils.subtract(oldDependencies, newDependencies);
            log.info("Build info for new build is {}", newBuildInfo);
            log.info("Build info for old build is {}", oldBuildInfo);
            List<String> fileContent = Arrays.asList(newBuildInfo, oldBuildInfo);
            Files.write(Paths.get("post-build-info.txt"), fileContent, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            log.error("Error during post build check", e);
        }
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    public void trigger() {
        log.info("releasePath: {}, extrasPath: {}, config: {}", releasePath, extrasPath, pigConfiguration);
        String stagingPath = (String) getAddOnConfiguration().get("stagingPath");
        String productName = (String) getAddOnConfiguration().get("productName");
        postBuildCheck(stagingPath, productName);
    }
}
