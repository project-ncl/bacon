package org.jboss.pnc.bacon.pig.impl.repo;

import static org.mockito.Mockito.doReturn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.config.Flow;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.config.ProductConfig;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationData;
import org.jboss.pnc.bacon.pig.impl.config.RepoGenerationStrategy;
import org.jboss.pnc.bacon.pig.impl.documents.Deliverables;
import org.jboss.pnc.bacon.pig.impl.pnc.ArtifactWrapper;
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ResolveOnlyRepositoryTest {

    private static final String EXPECTED_ARTIFACT_LIST_TXT = "resolve-and-repackage-repo-artifact-list.txt";
    private static final String EXTENSIONS_LIST_URL = "http://gitlab.cee.com";
    private static final String BOM_PATTERN = "vertx-dependencies-[\\d]+.*.pom";

    private Path workDir;
    private File testMavenSettings;

    @BeforeEach
    void beforeEach() throws Exception {
        workDir = Paths.get("target")
                .resolve("test-output")
                .resolve(getClass().getSimpleName())
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(workDir);
    }

    @AfterEach
    void afterEach() {
        if (testMavenSettings != null && testMavenSettings.exists()) {
            testMavenSettings.deleteOnExit();
        }
    }

    @AfterAll
    static void after() {
        Mockito.clearAllCaches();
    }

    @Test
    void resolveAndRepackageShouldGenerateRepository() {

        mockPigContextAndMethods();
        mockIndySettingsFile();

        PigConfiguration pigConfiguration = mockPigConfigurationAndMethods();

        RepoGenerationData generationDataSpy = mockRepoGenerationDataAndMethods();

        mockParamsAndMethods(generationDataSpy);

        mockFlowAndMethods(pigConfiguration, generationDataSpy);

        Map<String, PncBuild> buildsSpy = mockBuildsAndMethods(generationDataSpy);

        Path configurationDirectory = Mockito.mock(Path.class);

        mockResourceUtilsMethods(configurationDirectory);

        Deliverables deliverables = mockDeliverables(pigConfiguration);

        BuildInfoCollector buildInfoCollectorMock = Mockito.mock(BuildInfoCollector.class);

        RepoManager repoManager = new RepoManager(
                pigConfiguration,
                workDir.toString(),
                deliverables,
                buildsSpy,
                configurationDirectory,
                false,
                false,
                false,
                buildInfoCollectorMock,
                true);

        RepoManager repoManagerSpy = Mockito.spy(repoManager);

        prepareFakeExtensionArtifactList(repoManagerSpy);

        RepositoryData repoData = repoManagerSpy.prepare();

        Assertions.assertThat(repoData.getRepositoryPath())
                .isEqualTo(workDir.resolve("rh-sample-maven-repository.zip"));

        final Set<String> expectedFiles = repoZipContentList();

        final Set<String> actualFiles = repoData.getFiles()
                .stream()
                .map(
                        file -> file.getAbsolutePath()
                                .replaceAll(".+/deliverable-generation-\\d+/", "")
                                .replace('\\', '/'))
                .collect(Collectors.toCollection(TreeSet::new));

        final Path actualArtifactList = workDir.resolve("resolve-and-repackage-repo-artifact-list-actual.txt");
        try {
            Files.createDirectories(actualArtifactList.getParent());
            Files.write(
                    actualArtifactList,
                    (actualFiles.stream().collect(Collectors.joining("\n")) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + actualArtifactList, e);
        }

        if (!actualFiles.equals(expectedFiles)) {
            System.out.printf(
                    "\nThe zipped repository has unexpected content. You may want to compare src/test/resources/%s with %s\n\n",
                    EXPECTED_ARTIFACT_LIST_TXT,
                    actualArtifactList);
        }
        Assertions.assertThat(actualFiles).containsExactlyElementsOf(expectedFiles);
    }

    private void mockPigContextAndMethods() {
        PigContext pigContext = Mockito.mock(PigContext.class);
        doReturn(false).when(pigContext).isTempBuild();
        MockedStatic<PigContext> pigContextMockedStatic = Mockito.mockStatic(PigContext.class);
        pigContextMockedStatic.when(PigContext::get).thenReturn(pigContext);
    }

    private void mockIndySettingsFile() {
        testMavenSettings = ResourceUtils.extractToTmpFile("/indy-settings.xml", "settings", ".xml");
        String pathToTestSettingsFile = testMavenSettings.getAbsolutePath();
        MockedStatic<Indy> indyMockedStatic = Mockito.mockStatic(Indy.class);
        indyMockedStatic.when(() -> Indy.getConfiguredIndySettingsXmlPath(false)).thenReturn(pathToTestSettingsFile);
        indyMockedStatic.when(() -> Indy.getConfiguredIndySettingsXmlPath(false, true))
                .thenReturn(pathToTestSettingsFile);
        indyMockedStatic.when(() -> Indy.getConfiguredIndySettingsXmlPath(false, false))
                .thenReturn(pathToTestSettingsFile);
    }

    private PigConfiguration mockPigConfigurationAndMethods() {
        PigConfiguration pigConfiguration = Mockito.mock(PigConfiguration.class, Mockito.RETURNS_DEEP_STUBS);
        ProductConfig productConfig = Mockito.mock(ProductConfig.class);
        doReturn(productConfig).when(pigConfiguration).getProduct();
        doReturn("sample").when(productConfig).getName();
        return pigConfiguration;
    }

    private RepoGenerationData mockRepoGenerationDataAndMethods() {
        RepoGenerationData generationData = new RepoGenerationData();
        RepoGenerationData generationDataSpy = Mockito.spy(generationData);
        generationDataSpy.setStrategy(RepoGenerationStrategy.RESOLVE_ONLY);
        generationDataSpy.setSourceBuild("test-build");
        generationDataSpy.setSourceArtifact(BOM_PATTERN);

        return generationDataSpy;
    }

    private void mockParamsAndMethods(RepoGenerationData generationData) {
        Map<String, String> params = new HashMap<>();
        params.put("extensionsListUrl", EXTENSIONS_LIST_URL);
        doReturn(params).when(generationData).getParameters();
    }

    private void mockFlowAndMethods(PigConfiguration pigConfiguration, RepoGenerationData generationData) {
        Flow mockFlow = Mockito.mock(Flow.class);
        doReturn(generationData).when(mockFlow).getRepositoryGeneration();
        doReturn(mockFlow).when(pigConfiguration).getFlow();
    }

    private Map<String, PncBuild> mockBuildsAndMethods(RepoGenerationData generationData) {
        Map<String, PncBuild> builds = new HashMap<>();
        Map<String, PncBuild> buildsSpy = Mockito.spy(builds);

        PncBuild pncBuild = Mockito.mock(PncBuild.class);
        buildsSpy.put(generationData.getSourceBuild(), pncBuild);

        ArtifactWrapper artifactWrapper = Mockito.spy(ArtifactWrapper.class);
        GAV gav = new GAV("io.vertx", "vertx-dependencies", "4.1.0", "pom");
        doReturn(gav).when(artifactWrapper).toGAV();
        doReturn(artifactWrapper).when(pncBuild).findArtifactByFileName(BOM_PATTERN);

        return buildsSpy;
    }

    private void mockResourceUtilsMethods(Path configurationDirectory) {
        MockedStatic<ResourceUtils> resourceUtilsMockedStatic = Mockito.mockStatic(ResourceUtils.class);
        resourceUtilsMockedStatic.when(
                () -> ResourceUtils.getOverridableResource("/repository-example-settings.xml", configurationDirectory))
                .thenReturn("fake-resource-name");

        resourceUtilsMockedStatic
                .when(() -> ResourceUtils.getOverridableResource("/repository-README.md", configurationDirectory))
                .thenReturn("fake-resource-readme");
    }

    private Deliverables mockDeliverables(PigConfiguration pigConfiguration) {
        Deliverables deliverables = Mockito.mock(Deliverables.class);
        doReturn("rh-sample-maven-repository.zip").when(deliverables).getRepositoryZipName();
        doReturn("rh-sample-").when(pigConfiguration).getTopLevelDirectoryPrefix();
        return deliverables;
    }

    private void prepareFakeExtensionArtifactList(RepoManager repoManager) {
        Artifact vertxWeb = new DefaultArtifact("io.vertx", "vertx-bridge-common", "jar", "4.1.0");

        List<Artifact> extensionsArtifactList = new ArrayList<>();
        extensionsArtifactList.add(vertxWeb);
        doReturn(extensionsArtifactList).when(repoManager).parseExtensionsArtifactList(EXTENSIONS_LIST_URL);
        doReturn(extensionsArtifactList).when(repoManager).ensureVersionsSet(extensionsArtifactList);
    }

    private Set<String> repoZipContentList() {
        ClassLoader classLoader = getClass().getClassLoader();
        File repoZipContentListFile = new File(
                Objects.requireNonNull(classLoader.getResource(EXPECTED_ARTIFACT_LIST_TXT)).getFile());
        Set<String> repoZipContents = new TreeSet<>();

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
