package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.CSVUtils;
import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
        String communityDependenciesPath = "extras/community-dependencies.csv";
        String artifactFilesPath = "extras/repository-artifact-list.txt";
        Set<String> oldDependencies;
        Set<String> newDependencies;
        try {
            Document document = Jsoup.connect(stagingPathToProduct + "?C=M;O=D").get();
            Elements quarkusBuilds = document.select("a[href~=" + productName + "*]");

            String latest_build_path = stagingPathToProduct + quarkusBuilds.first().select("a[href]").text();
            String old_build_path = stagingPathToProduct + quarkusBuilds.get(1).select("a[href]").text();

            log.info("Latest build path is {}", latest_build_path);
            log.info("Old build path is {}", old_build_path);

            FileDownloadUtils.downloadTo(
                    new URI(latest_build_path + communityDependenciesPath),
                    new File("new_dependencies.csv"));
            FileDownloadUtils
                    .downloadTo(new URI(old_build_path + communityDependenciesPath), new File("old_dependencies.csv"));

            oldDependencies = CSVUtils.columnValues("Community dependencies", "new_dependencies.csv", ';');
            newDependencies = CSVUtils.columnValues("Community dependencies", "old_dependencies.csv", ';');

            String newBuildInfo = "Community Dependencies present in new build which were not present in old build are "
                    + CollectionUtils.subtract(newDependencies, oldDependencies);
            String oldBuildInfo = "Community Dependencies present in old build which are not present in new build are "
                    + CollectionUtils.subtract(oldDependencies, newDependencies);
            log.info("Build info for new build is {}", newBuildInfo);
            log.info("Build info for old build is {}", oldBuildInfo);

            String artifactDiff = "Artifacts present in new build and not present in old build are "
                    + diffArtifactsList(latest_build_path + artifactFilesPath, old_build_path + artifactFilesPath);
            List<String> fileContent = Arrays.asList(newBuildInfo, oldBuildInfo, artifactDiff);

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

    private static List<String> diffArtifactsList(String latestFilePath, String oldFilePath)
            throws IOException, URISyntaxException {
        File latestBuildFile = new File("latest_artifacts.txt");
        File oldBuildFile = new File("old_artifacts.txt");

        FileDownloadUtils.downloadTo(new URI(latestFilePath), latestBuildFile);
        FileDownloadUtils.downloadTo(new URI(oldFilePath), oldBuildFile);

        List<String> diff = Collections.emptyList();
        boolean areFilesEqual = FileUtils.contentEquals(latestBuildFile, oldBuildFile);
        log.info("Files are {}", (areFilesEqual == true ? "equal" : "not equal"));
        if (!(areFilesEqual)) {
            Set<String> latestFileContents = new HashSet<>(
                    FileUtils.readLines(latestBuildFile, StandardCharsets.UTF_8));
            Set<String> oldFileContents = new HashSet<>(FileUtils.readLines(oldBuildFile, StandardCharsets.UTF_8));
            diff = new ArrayList<>(CollectionUtils.subtract(latestFileContents, oldFileContents));
        }
        return diff;
    }
}
