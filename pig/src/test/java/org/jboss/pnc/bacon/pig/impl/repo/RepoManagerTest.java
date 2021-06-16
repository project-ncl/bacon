package org.jboss.pnc.bacon.pig.impl.repo;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.Flow;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.ProductConfig;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.mockito.Mockito.doReturn;

public class RepoManagerTest {

    @Test
    void resolveAndRepackageShouldGenerateRepository() throws Exception {

        PigContext pigContext = Mockito.mock(PigContext.class);
        doReturn(false).when(pigContext).isTempBuild();
        MockedStatic<PigContext> pigContextMockedStatic = Mockito.mockStatic(PigContext.class);
        pigContextMockedStatic.when(PigContext::get).thenReturn(pigContext);

        String pathToTestSettingsFile = ResourceUtils.extractToTmpFile("/indy-settings.xml", "settings", ".xml")
                .getAbsolutePath();
        MockedStatic<Indy> indyMockedStatic = Mockito.mockStatic(Indy.class);
        indyMockedStatic.when(() -> Indy.getConfiguredIndySettingsXmlPath(false)).thenReturn(pathToTestSettingsFile);

        PigConfiguration pigConfiguration = Mockito.mock(PigConfiguration.class, Mockito.RETURNS_DEEP_STUBS);
        ProductConfig productConfig = Mockito.mock(ProductConfig.class);
        doReturn(productConfig).when(pigConfiguration).getProduct();
        doReturn("sample").when(productConfig).getName();

        RepoGenerationData generationData = new RepoGenerationData();
        RepoGenerationData generationDataSpy = Mockito.spy(generationData);

        generationDataSpy.setStrategy(RepoGenerationStrategy.RESOLVE_ONLY);

        Map<String, String> params = new HashMap<>();
        params.put("extensionsListUrl", "http://gitlab.cee.com");
        doReturn(params).when(generationDataSpy).getParameters();

        Flow mockFlow = Mockito.mock(Flow.class);
        doReturn(generationDataSpy).when(mockFlow).getRepositoryGeneration();
        doReturn(mockFlow).when(pigConfiguration).getFlow();

        Map<String, PncBuild> builds = new HashMap<>();
        Map<String, PncBuild> buildsSpy = Mockito.spy(builds);

        PncBuild pncBuild = Mockito.mock(PncBuild.class);
        buildsSpy.put(generationDataSpy.getSourceBuild(), pncBuild);

        Path configurationDirectory = Mockito.mock(Path.class);

        MockedStatic<ResourceUtils> resourceUtilsMockedStatic = Mockito.mockStatic(ResourceUtils.class);
        resourceUtilsMockedStatic.when(
                () -> ResourceUtils.getOverridableResource("/repository-example-settings.xml", configurationDirectory))
                .thenReturn("fake-resource-name");

        resourceUtilsMockedStatic
                .when(() -> ResourceUtils.getOverridableResource("/repository-README.md", configurationDirectory))
                .thenReturn("fake-resource-readme");

        Deliverables deliverables = Mockito.mock(Deliverables.class);
        doReturn("rh-sample-maven-repository.zip").when(deliverables).getRepositoryZipName();

        doReturn("rh-sample-").when(pigConfiguration).getTopLevelDirectoryPrefix();

        String releasePath = "/tmp/resolveRepoTest/";
        File targetRepoZipPath = new File(releasePath);
        targetRepoZipPath.mkdirs();

        BuildInfoCollector buildInfoCollectorMock = Mockito.mock(BuildInfoCollector.class);

        RepoManager repoManager = new RepoManager(
                pigConfiguration,
                releasePath,
                deliverables,
                buildsSpy,
                configurationDirectory,
                false,
                false,
                buildInfoCollectorMock,
                true);
        RepoManager repoManagerSpy = Mockito.spy(repoManager);

        Artifact vertxWeb = new DefaultArtifact("io.vertx", "vertx-bridge-common", "jar", "4.1.0");

        List<Artifact> extensionsArtifactList = new ArrayList<>();
        extensionsArtifactList.add(vertxWeb);

        doReturn(extensionsArtifactList).when(repoManagerSpy).parseExtensionsArtifactList("http://gitlab.cee.com");
        Map<Artifact, String> redhatVersionMap = new HashMap<>();
        redhatVersionMap.put(vertxWeb, "4.1.0");
        doReturn(redhatVersionMap).when(repoManagerSpy).collectRedhatVersions(extensionsArtifactList);
        RepositoryData repoData = repoManagerSpy.prepare();

        assert repoData.getRepositoryPath().toString().equals("/tmp/resolveRepoTest/rh-sample-maven-repository.zip");

        Set<String> repoZipContents = repoZipContentList();
        repoData.getFiles().forEach((file) -> {
            String filePath = file.getAbsolutePath().replaceAll("/tmp/deliverable-generation\\d+/", "");
            assert repoZipContents.contains(filePath);
        });

    }

    private Set<String> repoZipContentList() {
        ClassLoader classLoader = getClass().getClassLoader();
        File repoZipContentListFile = new File(
                Objects.requireNonNull(classLoader.getResource("resolve-and-repackage-repo-artifact-list.txt"))
                        .getFile());
        Set<String> repoZipContents = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(repoZipContentListFile))) {
            for (String line; (line = br.readLine()) != null;) {
                repoZipContents.add(line);
            }
            return repoZipContents;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.emptySet();
    }
}
