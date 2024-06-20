package org.jboss.pnc.bacon.pig.impl.repo;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
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
import org.jboss.pnc.bacon.pig.impl.pnc.BuildInfoCollector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.bacon.pig.impl.utils.ResourceUtils;
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

import static org.mockito.Mockito.doReturn;

public abstract class MultiStepBomBasedRepositoryTestBase {

    private static final String IO_QUARKUS_PLATFORM_TEST = "io.quarkus.platform.test";
    private static final String ORIGINAL_LOCAL_REPO = "original-local";
    private static final String TEST_PLATFORM_ARTIFACTS = "test-platform-artifacts";
    private static final String EXPECTED_ARTIFACT_LIST_TXT = "resolve-and-repackage-repo-artifact-list.txt";
    private static final String EXTENSIONS_LIST_URL = "http://gitlab.cee.com";

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
    void resolveAndRepackageShouldGenerateRepository() throws Exception {

        mockPigContextAndMethods();
        mockIndySettingsFile();

        // initialize a Maven artifact resolver with the local Maven repo pointing
        // to a location we want to install the platform related artifacts below
        final MavenArtifactResolver resolver = MavenArtifactResolver.builder()
                .setLocalRepository(getPlatformTestArtifacts().toString())
                .setUserSettings(testMavenSettings)
                .build();

        // Compose a Quarkus platform model and install the artifacts into a Maven repo at a temporary location.
        // This temporary repo will be activated as a remote Maven repo when running the Maven repo generator
        TestPlatformBuilder.newInstance(workDir)
                // quarkus-bom
                .newBom(IO_QUARKUS_PLATFORM_TEST, "quarkus-bom", "1.1.1.redhat-00001")
                .addConstraint(
                        IO_QUARKUS_PLATFORM_TEST,
                        "quarkus-bom-quarkus-platform-descriptor",
                        "1.1.1.redhat-00001",
                        "json",
                        "1.1.1.redhat-00001")
                .addConstraint(
                        IO_QUARKUS_PLATFORM_TEST,
                        "quarkus-bom-quarkus-platform-properties",
                        "",
                        "properties",
                        "1.1.1.redhat-00001")
                .addConstraint("io.quarkus", "quarkus-core", "1.1.1.redhat-00001")
                // shaded jar should result in inclusion of the default jar in the Maven repo ZIP
                .addConstraint("org.acme", "acme", "shaded", "jar", "1.2.3.redhat-30303")
                // an annotation processor, as an example of an artifact that should be resolved with BOMs enforced and
                // without
                .addConstraint("org.acme", "acme-annotation-processor", "1.2.3.redhat-30303")
                .addConstraint("org.acme", "acme-annotation-processor-dep", "1.2.3.redhat-00002")
                .platform()
                // install core artifacts
                .installArtifact(
                        IO_QUARKUS_PLATFORM_TEST,
                        "quarkus-bom-quarkus-platform-descriptor",
                        "1.1.1.redhat-00001",
                        "json",
                        "1.1.1.redhat-00001")
                .installArtifact(
                        IO_QUARKUS_PLATFORM_TEST,
                        "quarkus-bom-quarkus-platform-properties",
                        "",
                        "properties",
                        "1.1.1.redhat-00001")
                .installArtifactWithDependencies("io.quarkus", "quarkus-core", "1.1.1.redhat-00001")
                // this common-lib productized version is not in the quarkus-bom
                .addDependency("org.thirdparty", "common-lib", "1.0.0.redhat-00005")
                .platform()
                .installArtifact("org.thirdparty", "common-lib", "1.0.0.redhat-00005")
                .installArtifact("org.thirdparty", "common-lib", "1.0.0")
                .installArtifactWithDependencies("org.acme", "acme", "1.2.3.redhat-30303")
                // a POM in the repo should result in the JAR associated with it also pulled in
                .addDependency("org.acme", "acme-library", "", "pom", "2.2.4.redhat-00005")
                .platform()
                .installArtifact("org.acme", "acme", "shaded", "jar", "1.2.3.redhat-30303")
                .installArtifactWithDependencies("org.acme", "acme-library", "2.2.4.redhat-00005")
                .addDependency("org.acme", "acme-excluded", "2.2.4.redhat-00005")
                .addDependency("org.acme", "acme-non-existent-excluded", "2.2.4.redhat-00005")
                .addDependency("org.acme", "acme-other-non-existent-excluded", "other", "jar", "2.2.4")
                .platform()
                .installArtifact("org.acme", "acme-excluded", "2.2.4.redhat-00005")
                .installArtifactWithDependencies("org.acme", "acme-annotation-processor", "1.2.3.redhat-30303")
                .addDependency("org.acme", "acme-annotation-processor-dep", "1.2.3.redhat-00001")
                .platform()
                .installArtifact("org.acme", "acme-annotation-processor-dep", "1.2.3.redhat-00001")
                .installArtifact("org.acme", "acme-annotation-processor-dep", "1.2.3.redhat-00002")
                // quarkus-camel-bom
                .newBom(IO_QUARKUS_PLATFORM_TEST, "quarkus-camel-bom", "1.1.1.redhat-00001")
                .addConstraint(
                        IO_QUARKUS_PLATFORM_TEST,
                        "quarkus-camel-bom-quarkus-platform-descriptor",
                        "1.1.1.redhat-00001",
                        "json",
                        "1.1.1.redhat-00001")
                .addConstraint("org.apache.camel.quarkus", "camel-quarkus-core", "2.2.2.redhat-00002")
                // this common-lib upstream version is managed by the quarkus-camel-bom
                .addConstraint("org.thirdparty", "common-lib", "1.0.0")
                .platform()
                // install Camel artifacts
                .installArtifact(
                        IO_QUARKUS_PLATFORM_TEST,
                        "quarkus-camel-bom-quarkus-platform-descriptor",
                        "1.1.1.redhat-00001",
                        "json",
                        "1.1.1.redhat-00001")
                .installArtifactWithDependencies("org.apache.camel.quarkus", "camel-quarkus-core", "2.2.2.redhat-00002")
                .addDependency("io.quarkus", "quarkus-core", "1.1.1.redhat-00001")
                .platform()
                // install the Maven plugin
                .installArtifact(IO_QUARKUS_PLATFORM_TEST, "quarkus-maven-plugin", "1.1.1.redhat-00001")
                .setMavenResolver(resolver)
                .build();

        PigConfiguration pigConfiguration = mockPigConfigurationAndMethods();

        RepoGenerationData generationDataSpy = mockRepoGenerationDataAndMethods(pigConfiguration);

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
                false);

        RepoManager repoManagerSpy = Mockito.spy(repoManager);

        prepareFakeExtensionArtifactList(repoManagerSpy);

        RepositoryData repoData = repoManagerSpy.prepare();

        Assertions.assertThat(repoData.getRepositoryPath())
                .isEqualTo(workDir.resolve("rh-sample-maven-repository.zip"));

        final Set<String> expectedFiles = repoZipContentList();

        final Set<String> actualFiles = repoData.getFiles()
                .stream()
                .map(file -> file.getAbsolutePath().replaceAll(".+/deliverable-generation\\d+/", "").replace('\\', '/'))
                .collect(Collectors.toCollection(TreeSet::new));

        final Path actualArtifactList = workDir.resolve("resolve-and-repackage-repo-artifact-list-actual.txt");
        try {
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

        Settings settings;
        try {
            settings = new DefaultSettingsReader().read(testMavenSettings, Map.of());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Maven settings from " + testMavenSettings, e);
        }

        String originalLocalRepo = settings.getLocalRepository();
        if (originalLocalRepo == null || originalLocalRepo.isBlank()) {
            originalLocalRepo = System.getProperty("user.home") + "/.m2/repository";
        } else {
            originalLocalRepo = originalLocalRepo.replace("${user.home}", System.getProperty("user.home"));
        }

        settings.setLocalRepository(getTestLocalRepo().toString());

        // original local repo
        addLocalRepo(settings, ORIGINAL_LOCAL_REPO, Paths.get(originalLocalRepo));
        // test platform artifacts
        addLocalRepo(settings, TEST_PLATFORM_ARTIFACTS, getPlatformTestArtifacts());

        try (BufferedWriter writer = Files.newBufferedWriter(testMavenSettings.toPath())) {
            new DefaultSettingsWriter().write(writer, Map.of(), settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist Maven settings to " + testMavenSettings, e);
        }

        String pathToTestSettingsFile = testMavenSettings.getAbsolutePath();
        MockedStatic<Indy> indyMockedStatic = Mockito.mockStatic(Indy.class);
        indyMockedStatic.when(() -> Indy.getConfiguredIndySettingsXmlPath(false)).thenReturn(pathToTestSettingsFile);
    }

    private Repository addLocalRepo(Settings settings, String id, Path localPath) {
        Profile profile = new Profile();
        profile.setId(id);
        settings.addActiveProfile(id);
        settings.addProfile(profile);

        Repository repo;
        try {
            repo = configureRepo(id, localPath.toUri().toURL().toExternalForm());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure repository", e);
        }
        profile.addRepository(repo);
        profile.addPluginRepository(repo);
        return repo;
    }

    private Path getTestLocalRepo() {
        return workDir.resolve("test-local-repo");
    }

    private Path getPlatformTestArtifacts() {
        return workDir.resolve(TEST_PLATFORM_ARTIFACTS);
    }

    private Repository configureRepo(String id, String url) {
        final Repository repo = new Repository();
        repo.setId(id);
        repo.setLayout("default");
        repo.setUrl(url);
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(true);
        repo.setReleases(policy);
        repo.setSnapshots(policy);
        return repo;
    }

    private PigConfiguration mockPigConfigurationAndMethods() {
        PigConfiguration pigConfiguration = Mockito.mock(PigConfiguration.class, Mockito.RETURNS_DEEP_STUBS);
        ProductConfig productConfig = Mockito.mock(ProductConfig.class);
        doReturn(productConfig).when(pigConfiguration).getProduct();
        doReturn("sample").when(productConfig).getName();
        return pigConfiguration;
    }

    protected abstract RepoGenerationStrategy getRepoGenerationStrategy();

    private RepoGenerationData mockRepoGenerationDataAndMethods(PigConfiguration pigConfiguration) {
        RepoGenerationData generationData = new RepoGenerationData();
        generationData.setParameters(
                Map.of(
                        "extensionsListUrl",
                        EXTENSIONS_LIST_URL,
                        "resolveIncludes",
                        "*:*:*redhat-*",
                        "resolveExcludes",
                        "*:*-excluded:*",
                        "excludeTransitive",
                        "org.acme:acme-non-existent-excluded, org.acme:acme-other-non-existent-excluded:other:jar",
                        // we add the quarkus-bom to the default params, since it will have to be enabled for everyone
                        "bomGavs",
                        IO_QUARKUS_PLATFORM_TEST + ":quarkus-bom:1.1.1.redhat-00001",
                        // this will resolve dependencies of the annotation processor w/o enforcing platform BOMs
                        "nonManagedDependencies",
                        "org.acme:acme-annotation-processor:1.2.3.redhat-30303"));

        // this quarkus-bom step is simply to generate the repo for the quarkus-bom
        final RepoGenerationData quarkusBomStep = new RepoGenerationData();

        // the Camel step includes only the quarkus-camel-bom, because the quarkus-bom will be inherited from the
        // default config
        final RepoGenerationData quarkusCamelBomStep = new RepoGenerationData();
        quarkusCamelBomStep
                .setParameters(Map.of("bomGavs", IO_QUARKUS_PLATFORM_TEST + ":quarkus-camel-bom:1.1.1.redhat-00001"));

        generationData.setSteps(List.of(quarkusBomStep, quarkusCamelBomStep));

        RepoGenerationData generationDataSpy = Mockito.spy(generationData);
        generationDataSpy.setStrategy(getRepoGenerationStrategy());
        generationDataSpy.setSourceBuild("test-build");

        mockFlowAndMethods(pigConfiguration, generationDataSpy);
        return generationDataSpy;
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
        Artifact mavenPlugin = new DefaultArtifact(
                IO_QUARKUS_PLATFORM_TEST,
                "quarkus-maven-plugin",
                "jar",
                "1.1.1.redhat-00001");

        List<Artifact> extensionsArtifactList = new ArrayList<>();
        extensionsArtifactList.add(mavenPlugin);
        doReturn(extensionsArtifactList).when(repoManager).parseExtensionsArtifactList(EXTENSIONS_LIST_URL);
        doReturn(extensionsArtifactList).when(repoManager).ensureVersionsSet(extensionsArtifactList);
    }

    private Set<String> repoZipContentList() {
        ClassLoader classLoader = getClass().getClassLoader();
        File repoZipContentListFile = new File(
                Objects.requireNonNull(
                        classLoader.getResource(
                                MultiStepBomBasedRepositoryTestBase.class.getSimpleName() + "/"
                                        + EXPECTED_ARTIFACT_LIST_TXT))
                        .getFile());
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
