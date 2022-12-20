package org.jboss.bacon.tempname.impl.projectfinder;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.tempname.impl.dependencies.DependencyResult;
import org.jboss.bacon.tempname.impl.dependencies.Project;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.bacon.common.Constant;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.BuildConfigurationRevision;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ProjectFinderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(45656).usingFilesUnderDirectory("src/test/resources/wiremock"))
            .build();

    private static WireMockServer mockServer;
    private ProjectFinder finder = new ProjectFinder();
    public static final Path CONFIG_LOCATION = Paths.get("target", "test-config");

    @BeforeAll
    public static void initBaconConfig() {
        Config.configure(CONFIG_LOCATION.toString(), Constant.CONFIG_FILE_NAME, "default");
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
        assertThat(toplevelOne.getBuildConfig()).isNotEmpty();
        BuildConfigurationRevision revision = toplevelOne.getBuildConfig().get();
        assertThat(revision.getId()).isEqualTo("300");
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
