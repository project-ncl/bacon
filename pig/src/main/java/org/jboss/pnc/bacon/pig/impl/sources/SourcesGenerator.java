package org.jboss.pnc.bacon.pig.impl.sources;

import static org.jboss.pnc.common.scm.ScmUrlGeneratorProvider.determineScmProvider;
import static org.jboss.pnc.common.scm.ScmUrlGeneratorProvider.getScmUrlGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.BrewSearcher;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.MRRCSearcher;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.repo.RepositoryData;
import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jboss.pnc.bacon.pig.impl.utils.FileUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pnc.common.ClientCreator;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.common.scm.ScmException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

public class SourcesGenerator {
    private static final Logger log = LoggerFactory.getLogger(SourcesGenerator.class);

    private static final String KOJI_TOP_URL = "https://download.devel.redhat.com/brewroot";

    private static final ClientCreator<BuildClient> CREATOR = new ClientCreator<>(BuildClient::new);

    private final BuildInfoCollector buildInfoCollector;

    public static final MRRCSearcher mrrcSearcher = MRRCSearcher.getInstance();

    private final SourcesGenerationData sourcesGenerationData;

    private final String topLevelDirectoryName;

    private final String targetZipFileName;

    public SourcesGenerator(
            boolean oldBCNaming,
            SourcesGenerationData sourcesGenerationData,
            String topLevelDirectoryName,
            String targetZipFileName) {
        this.sourcesGenerationData = sourcesGenerationData;
        this.topLevelDirectoryName = topLevelDirectoryName;
        this.targetZipFileName = targetZipFileName;
        buildInfoCollector = new BuildInfoCollector();
        if (oldBCNaming) {
            this.sourcesGenerationData.setOldBCNaming(true);
        }
    }

    public void generateSources(Map<String, PncBuild> builds, RepositoryData repo) {
        // Do not modify the original builds map. Rather, create a shallow copy and modify it.
        // The builds map could be a 'global' object and we don't want to modify that since it'll wrongly add
        // additional builds to the list of builds done as part of PiG run
        Map<String, PncBuild> additionalBuildsForSources = new HashMap<>(builds);
        if (sourcesGenerationData.getStrategy() == SourcesGenerationStrategy.IGNORE) {
            log.info("Ignoring source zip generation");
            return;
        }
        log.info("Generating sources");

        File workDir = FileUtils.mkTempDir("sources");

        File contentsDir = new File(workDir, topLevelDirectoryName);

        contentsDir.mkdirs();

        if (sourcesGenerationData.getStrategy() == SourcesGenerationStrategy.GENERATE_SELECTED) {
            PncBuild sourceBuild = additionalBuildsForSources.get(sourcesGenerationData.getSourceBuild());
            additionalBuildsForSources = Maps
                    .filterEntries(additionalBuildsForSources, entry -> entry.getKey().equals(sourceBuild.getName()));
        }

        if (sourcesGenerationData.getStrategy() == SourcesGenerationStrategy.GENERATE_REDHAT_DEPENDENCIES
                || sourcesGenerationData
                        .getStrategy() == SourcesGenerationStrategy.GENERATE_REDHAT_DEPENDENCIES_EXTENDED) {
            additionalBuildsForSources = addRedhatDependencyBuilds(additionalBuildsForSources);
        }
        if (sourcesGenerationData.getStrategy() == SourcesGenerationStrategy.GENERATE_ADDITIONAL_SELECTED) {
            for (String buildConfigName : sourcesGenerationData.getAdditionalExternalSources()) {
                BuildInfoCollector.BuildSearchType type = PigContext.get().isTempBuild()
                        ? BuildInfoCollector.BuildSearchType.TEMPORARY
                        : BuildInfoCollector.BuildSearchType.PERMANENT;

                additionalBuildsForSources.put(
                        buildConfigName,
                        buildInfoCollector.getLatestBuild(buildInfoCollector.ConfigNametoId(buildConfigName), type));

            }
        }

        if (!sourcesGenerationData.getExcludeSourceBuilds().isEmpty()) {
            log.info("Removing builds from sources generation " + sourcesGenerationData.getExcludeSourceBuilds());
            additionalBuildsForSources.keySet().removeAll(sourcesGenerationData.getExcludeSourceBuilds());
        }

        // Set the name of the build to be more consistent, relying on user specified BC names yields some odd names
        // in source directories. These attributes should be set but are BEST EFFORT and not guaranteed to be set
        // for non-maven builds.
        if (!sourcesGenerationData.isOldBCNaming()) {
            for (PncBuild build : additionalBuildsForSources.values()) {
                String brewBuildName = build.getAttributes().get("BREW_BUILD_NAME");
                String brewBuildVersion = build.getAttributes().get("BREW_BUILD_VERSION");
                if (brewBuildName != null && brewBuildVersion != null) {
                    build.setName(brewBuildName.replaceAll(":", "-") + "-" + brewBuildVersion);
                }
            }
        }

        downloadSourcesFromBuilds(additionalBuildsForSources, workDir, contentsDir);

        if (sourcesGenerationData.getStrategy() == SourcesGenerationStrategy.GENERATE_EXTENDED || sourcesGenerationData
                .getStrategy() == SourcesGenerationStrategy.GENERATE_REDHAT_DEPENDENCIES_EXTENDED) {
            addSourcesOfUnreleasedDependencies(repo, workDir, contentsDir);
        }

        File zipFile = new File(targetZipFileName);

        FileUtils.zip(zipFile, workDir, contentsDir);
    }

    private Map<String, PncBuild> addRedhatDependencyBuilds(Map<String, PncBuild> parentBuilds) {
        Map<String, PncBuild> completeBuilds = parentBuilds;
        List<PncBuild> mapBuilds = parentBuilds.values().stream().collect(Collectors.toCollection(ArrayList::new));
        try (BuildClient client = CREATOR.newClient()) {
            for (PncBuild parentBuild : mapBuilds) {
                List<Artifact> redhatArtifacts = client.getDependencyArtifacts(parentBuild.getId())
                        .getAll()
                        .stream()
                        .filter(artifact -> artifact.getIdentifier().matches(".*redhat-\\d{1,5}"))
                        .collect(Collectors.toList());
                for (Artifact a : redhatArtifacts) {
                    try {
                        String buildName = a.getBuild().getBuildConfigRevision().getName();
                        PncBuild pncBuild = new PncBuild(a.getBuild());
                        if (sourcesGenerationData.isOldBCNaming()) {
                            pncBuild.setName(pncBuild.getName().replaceAll("-AUTOBUILD", ""));
                        } else {
                            buildName = pncBuild.getName();
                        }
                        completeBuilds.put(buildName, pncBuild);
                    } catch (NullPointerException e) {
                        log.warn("Artifact " + a.getIdentifier() + " does not have build assigned! No sources added.");
                    }
                }
            }
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
        return completeBuilds;
    }

    private void downloadSourcesFromBuilds(Map<String, PncBuild> builds, File workDir, File contentsDir) {
        builds.values().forEach(build -> {
            File targetPath = new File(workDir, build.getName() + "-" + build.getId() + ".tar.gz");

            try {
                var provider = determineScmProvider(build.getScmRepository(), build.getInternalScmUrl());
                URI uri = new URI(
                        getScmUrlGenerator(provider)
                                .generateTarballDownloadUrl(build.getScmRepository(), build.getScmRevision()));

                HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", "Bearer " + Config.instance().getActiveProfile().getGithubToken())
                        // specify that we only want a tar.gz reply, otherwise it returns an html file if there is a
                        // login failure with http status 200, which makes the http client wrongly believe the download
                        // was successful
                        .header("Accept", "application/x-tar, application/gzip")
                        .GET()
                        .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == HttpResponseCodes.SC_NOT_ACCEPTABLE) {
                    // Github throws this error when the Github Token is not specified, or wrong
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("Failed to download sources for build: ").append(build.getId()).append("\n");
                    errorMessage.append("URL: ").append(uri).append("\n");
                    errorMessage.append("Please specify a valid 'githubToken' in your 'config.yaml'").append("\n");
                    throw new RuntimeException(errorMessage.toString());

                } else if (response.statusCode() != HttpResponseCodes.SC_OK) {
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("Failed to download sources for build: ").append(build.getId()).append("\n");
                    errorMessage.append("HTTP Status: ").append(response.statusCode()).append("\n");
                    throw new RuntimeException(errorMessage.toString());
                }
                try (InputStream responseStream = response.body()) {
                    Files.copy(responseStream, targetPath.toPath());
                }

            } catch (ScmException | URISyntaxException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            Collection<String> untaredFiles = FileUtils.untar(targetPath, contentsDir);
            List<String> topLevelDirectories = untaredFiles.stream()
                    .filter(this::isNotANestedFile)
                    .collect(Collectors.toList());

            if (topLevelDirectories.size() != 1) {
                throw new RuntimeException(
                        "Found more than one top-level directory (" + topLevelDirectories.size()
                                + ") untared for build " + build + ", the untared archive: "
                                + targetPath.getAbsolutePath() + ", the top level directories:" + topLevelDirectories);
            }

            String topLevelDirectoryName = untaredFiles.iterator().next();
            File topLevelDirectory = new File(contentsDir, topLevelDirectoryName);
            cleanupSources(topLevelDirectory);
            File properTopLevelDirectory = new File(contentsDir, build.getName());
            topLevelDirectory.renameTo(properTopLevelDirectory);
        });
    }

    /**
     * <p>
     * Deletes unwanted files found on the sources folder.
     * </p>
     *
     * @param topLevelDirectory
     *        <p>
     *        Root folder for searching the unwanted files.
     *        </p>
     */
    private void cleanupSources(final File topLevelDirectory) {
        final String cleanupRegex = "^repositories-backup.xml$";
        try (Stream<Path> stream = Files.walk(topLevelDirectory.toPath())) {
            stream.filter(path -> path.getFileName().toString().matches(cleanupRegex)).forEach(path -> {
                log.debug("Deleting file: {}", path);
                path.toFile().delete();
            });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private boolean isNotANestedFile(String name) {
        // either "some-directory" or "some-directory/"
        // paths inside tar files only ever contain forward slashes
        int i = name.indexOf("/");
        return i == -1 || i + 1 == name.length();
    }

    private void addSourcesOfUnreleasedDependencies(RepositoryData repo, File workDir, File contentsDir) {
        File unreleasedWorkDir = new File(workDir, topLevelDirectoryName);
        unreleasedWorkDir.mkdirs();
        addUnreleasedSources(repo, unreleasedWorkDir);
        Stream.of(unreleasedWorkDir.listFiles())
                .filter(f -> f.getName().endsWith("tar.gz"))
                .forEach(f -> FileUtils.untar(f, contentsDir));
    }

    private void addUnreleasedSources(RepositoryData repo, File contentsDir) {
        // TODO: handle projects without the project sources tgz here
        Predicate<File> isWhitelisted = sourcesGenerationData.getWhitelistedArtifacts().isEmpty() ? f -> true
                : f -> sourcesGenerationData.getWhitelistedArtifacts().stream().anyMatch(a -> f.getName().contains(a));

        repo.getFiles()
                .stream()
                .filter(f -> f.getName().endsWith(".jar"))
                .filter(SourcesGenerator::isUnreleased)
                .filter(isWhitelisted)
                .map(SourcesGenerator::getSingleBuild)
                .distinct()
                .forEach(build -> downloadSourcesTo(build, contentsDir));
    }

    private static boolean isUnreleased(File file) {
        final String fileAbsolutePath = file.getAbsolutePath();
        final String lastPartOfPath = "maven-repository";
        final String repoDirName = fileAbsolutePath
                .substring(0, fileAbsolutePath.indexOf(lastPartOfPath) + lastPartOfPath.length() + 1);
        GAV gav = GAV.fromFileName(fileAbsolutePath, repoDirName);

        return !mrrcSearcher.isReleased(gav);
    }

    protected static File downloadSourcesTo(KojiBuild build, File directory) {
        URI downloadUrl = getSourcesArtifactURL(build);
        String filename = FilenameUtils.getName(downloadUrl.getPath());

        log.info(
                "Downloading sources artifact url {} for build id {} to {}",
                downloadUrl,
                build.getBuildInfo().getId(),
                directory);

        File destination = new File(directory, filename);

        FileDownloadUtils.downloadTo(downloadUrl, destination);

        return destination;
    }

    private static URI getDownloadURL(KojiBuildInfo buildInfo, KojiArchiveInfo archiveInfo) {
        final StringBuilder sb = new StringBuilder(KOJI_TOP_URL);

        if (buildInfo.getVolumeName() != null && !buildInfo.getVolumeName().equals("DEFAULT")) {
            sb.append(String.format("/vol/%s", buildInfo.getVolumeName()));
        }

        sb.append(
                String.format(
                        "/packages/%s/%s/%s",
                        buildInfo.getName(),
                        buildInfo.getVersion(),
                        buildInfo.getRelease()));
        sb.append("/maven");
        sb.append(
                "/" + String.format(
                        "%s/%s/%s",
                        archiveInfo.getGroupId().replace('.', '/'),
                        archiveInfo.getArtifactId(),
                        archiveInfo.getVersion()));
        sb.append("/" + archiveInfo.getFilename());

        final String str = sb.toString();

        return URI.create(str);
    }

    protected static URI getSourcesArtifactURL(KojiBuild build) {
        Optional<KojiArchiveInfo> archiveInfo = build.getProjectSourcesTgz();
        KojiBuildInfo buildInfo = build.getBuildInfo();

        if (!archiveInfo.isPresent()) {
            throw new RuntimeException("Could not find project-sources.tar.gz for build id " + buildInfo.getId());
        }

        log.info("Got project sources tgz artifact {}", archiveInfo.get().getFilename());

        return getDownloadURL(buildInfo, archiveInfo.get());
    }

    private static KojiBuild getSingleBuild(File file) {
        List<KojiBuild> builds = BrewSearcher.getBuilds(file.toPath());

        if (builds.size() != 1) {
            throw new RuntimeException(
                    "Number of builds " + builds.size() + " does not equal one for artifact " + file.getAbsolutePath());
        }

        return builds.get(0);
    }
}
