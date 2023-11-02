package org.jboss.pnc.bacon.pig.impl.sources;

import com.google.common.collect.Maps;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import org.apache.commons.io.FilenameUtils;
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
import org.jboss.pnc.dto.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jboss.pnc.bacon.pig.impl.utils.GerritUtils.gerritSnapshotDownloadUrl;

public class SourcesGenerator {
    private static final Logger log = LoggerFactory.getLogger(SourcesGenerator.class);

    private static final String KOJI_TOP_URL = "http://download.eng.bos.redhat.com/brewroot";

    private static final ClientCreator<BuildClient> CREATOR = new ClientCreator<>(BuildClient::new);

    private final BuildInfoCollector buildInfoCollector;

    public static final MRRCSearcher mrrcSearcher = MRRCSearcher.getInstance();

    private final SourcesGenerationData sourcesGenerationData;

    private final String topLevelDirectoryName;

    private final String targetZipFileName;

    public SourcesGenerator(
            SourcesGenerationData sourcesGenerationData,
            String topLevelDirectoryName,
            String targetZipFileName) {
        this.sourcesGenerationData = sourcesGenerationData;
        this.topLevelDirectoryName = topLevelDirectoryName;
        this.targetZipFileName = targetZipFileName;
        buildInfoCollector = new BuildInfoCollector();
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
                        pncBuild.setName(pncBuild.getName().replaceAll("-AUTOBUILD", ""));
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
            URI url = gerritSnapshotDownloadUrl(build.getInternalScmUrl(), build.getScmRevision());

            File targetPath = new File(workDir, build.getName() + "-" + build.getId() + ".tar.gz");
            try (BuildClient client = CREATOR.newClient();
                    Response response = client.getInternalScmArchiveLink(build.getId())) {
                InputStream in = (InputStream) response.getEntity();
                Files.copy(in, targetPath.toPath());
            } catch (IOException | RemoteResourceException e) {
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
            File properTopLevelDirectory = new File(contentsDir, build.getName());
            topLevelDirectory.renameTo(properTopLevelDirectory);
        });
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
