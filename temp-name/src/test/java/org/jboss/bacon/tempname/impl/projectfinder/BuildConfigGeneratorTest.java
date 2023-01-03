package org.jboss.bacon.tempname.impl.projectfinder;

import org.jboss.bacon.tempname.impl.dependencies.DependencyResult;
import org.jboss.bacon.tempname.impl.dependencies.Project;
import org.jboss.da.model.rest.GAV;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class BuildConfigGeneratorTest {
    @Test
    void testExistingVersionBC() {
        DependencyResult dependencyResult = generateDependencyResult();
        Project topLevelProject = dependencyResult.getTopLevelProjects().iterator().next();
        GAV gav = topLevelProject.getGavs().iterator().next();
    }

    private Project generateProject(String groupId) {
        Project p = new Project();
        p.setGavs(generateGavs(groupId));
        p.setSourceCodeURL("https://git.example.com/" + groupId + ".git");
        return p;
    }

    private static Set<GAV> generateGavs(String groupId) {
        return generateGavs(groupId, "2.3.4");
    }

    private static Set<GAV> generateGavs(String groupId, String version) {
        Set<GAV> gavs = new HashSet<>();
        gavs.add(new GAV(groupId, "foo", version));
        return gavs;
    }

    private DependencyResult generateDependencyResult() {
        Project p1 = generateProject("project-one");
        DependencyResult result = new DependencyResult();
        Set<Project> projects = new HashSet<>();
        projects.add(p1);
        result.setTopLevelProjects(projects);
        return result;
    }
}
