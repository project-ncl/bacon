package org.jboss.pnc.bacon.pig.impl.addons.quarkus;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.CSVUtils;
import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jboss.pnc.bacon.pig.impl.utils.XmlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
        List<String> fileContent = new ArrayList<>();
        try {
            Document document = Jsoup.connect(stagingPathToProduct + "?C=M;O=D").get();
            Elements quarkusBuilds = document.select("a[href~=" + productName + "*]");

            String latest_build_path = stagingPathToProduct + quarkusBuilds.first().select("a[href]").text();
            String old_build_path = stagingPathToProduct + quarkusBuilds.get(1).select("a[href]").text();

            log.info("Latest build path is {}", latest_build_path);
            log.info("Old build path is {}", old_build_path);

            fileContent.addAll(diffDepsCsv(latest_build_path, old_build_path));
            fileContent.add(
                    diffTextFiles(
                            latest_build_path,
                            old_build_path,
                            "extras/repository-artifact-list.txt",
                            "artifacts"));
            fileContent.add(
                    diffTextFiles(
                            latest_build_path,
                            old_build_path,
                            "extras/nonexistent-redhat-deps.txt",
                            "nonexistent-redhat-deps"));
            fileContent.add(diffLicenseXml(latest_build_path, old_build_path));
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

    private static String diffTextFiles(
            String latestBuildPath,
            String oldBuildPath,
            String filePath,
            String deliverableType) throws IOException, URISyntaxException {
        String latestFilePath = latestBuildPath + filePath;
        String oldFilePath = oldBuildPath + filePath;
        File latestBuildFile = new File("latest_" + deliverableType + ".txt");
        File oldBuildFile = new File("old_" + deliverableType + ".txt");

        FileDownloadUtils.downloadTo(new URI(latestFilePath), latestBuildFile);
        FileDownloadUtils.downloadTo(new URI(oldFilePath), oldBuildFile);

        boolean areFilesEqual = FileUtils.contentEquals(latestBuildFile, oldBuildFile);
        log.info("Files are {}", (areFilesEqual == true ? "equal" : "not equal"));
        String diff = "The " + deliverableType + " files are equal";
        if (!(areFilesEqual)) {
            Set<String> latestFileContents = new HashSet<>(
                    FileUtils.readLines(latestBuildFile, StandardCharsets.UTF_8));
            Set<String> oldFileContents = new HashSet<>(FileUtils.readLines(oldBuildFile, StandardCharsets.UTF_8));
            diff = deliverableType + " present in new build and not present in old build are "
                    + CollectionUtils.subtract(latestFileContents, oldFileContents);
        }
        return diff;
    }

    private static List<String> diffDepsCsv(String latest_build_path, String old_build_path)
            throws URISyntaxException, IOException {
        List<String> diff = new ArrayList<>();
        String communityDependenciesPath = "extras/community-dependencies.csv";
        FileDownloadUtils
                .downloadTo(new URI(latest_build_path + communityDependenciesPath), new File("new_dependencies.csv"));
        FileDownloadUtils
                .downloadTo(new URI(old_build_path + communityDependenciesPath), new File("old_dependencies.csv"));

        Set<String> oldDependencies = CSVUtils.columnValues("Community dependencies", "new_dependencies.csv", ';');
        Set<String> newDependencies = CSVUtils.columnValues("Community dependencies", "old_dependencies.csv", ';');

        String newBuildInfo = "Community Dependencies present in new build which were not present in old build are "
                + CollectionUtils.subtract(newDependencies, oldDependencies);
        String oldBuildInfo = "Community Dependencies present in old build which are not present in new build are "
                + CollectionUtils.subtract(oldDependencies, newDependencies);
        log.info("Build info for new build is {}", newBuildInfo);
        log.info("Build info for old build is {}", oldBuildInfo);
        diff.add(newBuildInfo);
        diff.add(oldBuildInfo);
        return diff;
    }

    private static String diffLicenseXml(String latest_build_path, String old_build_path)
            throws IOException, URISyntaxException {
        File latestzip = new File("latest.zip");
        File oldZip = new File("old.zip");
        Document document = Jsoup.connect(latest_build_path).get();
        String latestlicensezip = latest_build_path
                + document.select("a[href~=license]").first().select("a[href]").text();
        document = Jsoup.connect(old_build_path).get();
        String oldlicensezip = old_build_path + document.select("a[href~=license]").first().select("a[href]").text();
        FileDownloadUtils.downloadTo(new URI(latestlicensezip), latestzip);
        FileDownloadUtils.downloadTo(new URI(oldlicensezip), oldZip);
        org.jboss.pnc.bacon.pig.impl.utils.FileUtils.getFileFromZip("latest.zip", "licenses.xml", "new_license.xml");
        org.jboss.pnc.bacon.pig.impl.utils.FileUtils.getFileFromZip("old.zip", "licenses.xml", "old_license.xml");

        Set<String> newXml = XmlUtils.listNodes(new File("new_license.xml"), "//license/name")
                .stream()
                .map(dep -> dep.getTextContent())
                .collect(Collectors.toSet());
        Set<String> oldXml = XmlUtils.listNodes(new File("old_license.xml"), "//license/name")
                .stream()
                .map(dep -> dep.getTextContent())
                .collect(Collectors.toSet());
        List<String> licenseDiff = CollectionUtils.subtract(newXml, oldXml).stream().collect(Collectors.toList());
        String diffLicense = "There is no new licenses added in the build compared to the previous build";
        if (!(licenseDiff.isEmpty())) {
            diffLicense = "The new licenses added to this build are " + CollectionUtils.subtract(newXml, oldXml);
        }
        return diffLicense;
    }
}
