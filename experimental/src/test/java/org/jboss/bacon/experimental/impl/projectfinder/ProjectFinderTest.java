package org.jboss.bacon.experimental.impl.projectfinder;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.bacon.experimental.impl.config.BuildConfigGeneratorConfig;
import org.jboss.bacon.experimental.impl.dependencies.DependencyResult;
import org.jboss.bacon.experimental.impl.dependencies.Project;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectFinderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(
                    wireMockConfig().port(45656)
                            .usingFilesUnderDirectory("src/test/resources/wiremock")
                            .notifier(new ConsoleNotifier(false)))
            .build();

    private static WireMockServer mockServer;

    private BuildConfigGeneratorConfig config;
    private ProjectFinder finder;
    public static final Path CONFIG_LOCATION = Paths.get("target", "test-config");

    @BeforeAll
    public static void initBaconConfig() {
        Config.configure(CONFIG_LOCATION.toString(), Constant.CONFIG_FILE_NAME, "default");
    }

    @BeforeEach
    public void initProjectFinder() {
        config = new BuildConfigGeneratorConfig();
        finder = new ProjectFinder(config);
    }

    @Test
    public void testFindProjects() throws RemoteResourceException {
        DependencyResult dependencyResult = generateDependencyResult();

        FoundProjects projects = finder.findProjects(dependencyResult);

        assertThat(projects).isNotNull();
        assertThat(projects.getFoundProjects()).hasSize(5);
        FoundProject toplevelOne = null;
        for (FoundProject project : projects.getFoundProjects()) {
            if (project.getGavs().stream().map(GAV::getGroupId).anyMatch("toplevel-one"::equals)) {
                toplevelOne = project;
                break;
            }
        }
        assertThat(toplevelOne).isNotNull();
        assertThat(toplevelOne.isFound()).isTrue();
        BuildConfigurationRevision revision = toplevelOne.getBuildConfigRevision();
        assertThat(revision.getId()).isEqualTo("300");
    }

    @Test
    public void testExactMatch() throws RemoteResourceException {
        GAV gav = new GAV("com.sun.xml.bind", "jaxb-bom-ext", "2.3.3-b02");
        Project project = new Project();
        project.setGavs(Set.of(gav));
        project.setName("com.sun.xml.bind-jaxb-bom-ext-2.3.3-b02-AUTOBUILDER");
        project.setDependencies(Set.of());
        project.setSourceCodeURL("https://github.com/eclipse-ee4j/jaxb-ri.git");
        project.setSourceCodeRevision("2.3.3-b02-RI");
        DependencyResult dependencyResult = new DependencyResult();
        dependencyResult.setTopLevelProjects(Set.of(project));

        FoundProjects projects = finder.findProjects(dependencyResult);

        assertThat(projects).isNotNull();
        assertThat(projects.getFoundProjects()).hasSize(1);
        FoundProject found = projects.getFoundProjects().iterator().next();
        assertThat(found).isNotNull();
        assertThat(found.isFound()).isTrue();
        assertThat(found.isExactMatch()).isTrue();
    }

    @Test
    public void shouldReuseAutobuilderConfig() throws RemoteResourceException {
        GAV gav = new GAV("foo.bar", "managed", "1.2.3");
        Project project = new Project();
        project.setGavs(Set.of(gav));
        project.setName("foo.bar-managed-1.2.3-AUTOBUILDER");
        project.setDependencies(Set.of());
        project.setSourceCodeURL("https://github.com/eclipse-ee4j/jaxb-ri.git");
        project.setSourceCodeRevision("2.3.3-b02-RI");

        DependencyResult dependencyResult = new DependencyResult();
        dependencyResult.setTopLevelProjects(Set.of(project));

        FoundProjects projects = finder.findProjects(dependencyResult);

        assertThat(projects).isNotNull();
        assertThat(projects.getFoundProjects()).hasSize(1);
        FoundProject found = projects.getFoundProjects().iterator().next();
        assertThat(found).isNotNull();
        assertThat(found.isFound()).isTrue();
        assertThat(found.isManaged()).isTrue();
        assertThat(found.getBuildConfig().getBuildScript()).contains("# Created by Autobuilder-afs231");
    }

    @Test
    public void shouldFindPersistentBuild() throws RemoteResourceException {
        GAV gav = new GAV("foo.bar", "built", "1.2.3");
        Project project = new Project();
        project.setGavs(Set.of(gav));
        project.setName("foo.bar-built-1.2.3-AUTOBUILDER");
        project.setDependencies(Set.of());
        project.setSourceCodeURL("https://github.com/eclipse-ee4j/jaxb-ri.git");
        project.setSourceCodeRevision("2.3.3-b02-RI");

        DependencyResult dependencyResult = new DependencyResult();
        dependencyResult.setTopLevelProjects(Set.of(project));

        FoundProjects projects = finder.findProjects(dependencyResult);

        assertThat(projects).isNotNull();
        assertThat(projects.getFoundProjects()).hasSize(1);
        FoundProject found = projects.getFoundProjects().iterator().next();
        assertThat(found).isNotNull();
        assertThat(found.isFound()).isTrue();
        assertThat(found.isExactMatch()).isTrue();
    }

    @Test
    public void shouldFindTempBuildWhenPersistentDoesntExist() throws RemoteResourceException {
        GAV gav = new GAV("foo.bar", "temporary", "1.2.3");
        Project project = new Project();
        project.setGavs(Set.of(gav));
        project.setName("foo.bar-temporary-1.2.3-AUTOBUILDER");
        project.setDependencies(Set.of());
        project.setSourceCodeURL("https://github.com/eclipse-ee4j/jaxb-ri.git");
        project.setSourceCodeRevision("2.3.3-b02-RI");

        DependencyResult dependencyResult = new DependencyResult();
        dependencyResult.setTopLevelProjects(Set.of(project));

        FoundProjects projects = finder.findProjects(dependencyResult);

        assertThat(projects).isNotNull();
        assertThat(projects.getFoundProjects()).hasSize(1);
        FoundProject found = projects.getFoundProjects().iterator().next();
        assertThat(found).isNotNull();
        assertThat(found.isFound()).isTrue();
        assertThat(found.isExactMatch()).isTrue();
    }

    @Test
    public void shouldNotFindBuild() throws RemoteResourceException {
        GAV gav = new GAV("foo.bar", "notbuilt", "1.2.3");
        Project project = new Project();
        project.setGavs(Set.of(gav));
        project.setName("foo.bar-notbuilt-1.2.3-AUTOBUILDER");
        project.setDependencies(Set.of());
        project.setSourceCodeURL("https://github.com/eclipse-ee4j/jaxb-ri.git");
        project.setSourceCodeRevision("2.3.3-b02-RI");

        DependencyResult dependencyResult = new DependencyResult();
        dependencyResult.setTopLevelProjects(Set.of(project));

        FoundProjects projects = finder.findProjects(dependencyResult);

        assertThat(projects).isNotNull();
        assertThat(projects.getFoundProjects()).hasSize(1);
        FoundProject found = projects.getFoundProjects().iterator().next();
        assertThat(found).isNotNull();
        assertThat(found.isFound()).isFalse();
    }

    private DependencyResult generateDependencyResult() {
        Project toplevel1 = generateProject("toplevel-one");
        Project toplevel2 = generateProject("toplevel-two");
        Project dep1 = generateProject("dependency-one");
        Project dep2 = generateProject("dependency-two");
        Project depCommon = generateProject("dependency-common");

        dep1.setDependencies(Collections.singleton(depCommon));
        dep2.setDependencies(Collections.singleton(depCommon));
        toplevel1.setDependencies(Collections.singleton(dep1));
        toplevel2.setDependencies(Collections.singleton(dep2));

        DependencyResult result = new DependencyResult();
        Set<Project> toplevels = new HashSet<>();
        toplevels.add(toplevel1);
        toplevels.add(toplevel2);
        result.setTopLevelProjects(toplevels);
        return result;
    }

    private Project generateProject(String groupId) {
        Project p = new Project();
        p.setGavs(generateGavs(groupId));
        p.setName(groupId + "-bar-2.3.4-AUTOBUILDER");
        p.setDependencies(new HashSet<>());
        p.setSourceCodeURL("https://git.example.com/" + groupId + ".git");
        return p;
    }

    private static Set<GAV> generateGavs(String groupId) {
        return generateGavs(groupId, "2.3.4");
    }

    private static Set<GAV> generateGavs(String groupId, String version) {
        Set<GAV> gavs = new HashSet<>();
        gavs.add(new GAV(groupId, "foo", version));
        gavs.add(new GAV(groupId, "bar", version));
        gavs.add(new GAV(groupId, "baz", version));
        return gavs;
    }

}
