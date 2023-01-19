package org.jboss.bacon.tempname.impl.generator;

import org.jboss.bacon.tempname.impl.config.BuildConfigGeneratorConfig;
import org.jboss.bacon.tempname.impl.dependencies.DependencyResult;
import org.jboss.bacon.tempname.impl.dependencies.Project;
import org.jboss.bacon.tempname.impl.projectfinder.FoundProject;
import org.jboss.bacon.tempname.impl.projectfinder.FoundProjects;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BuildConfigGeneratorTest {

    static BuildConfigGenerator generator;

    @BeforeAll
    public static void initGenerator() {
        BuildConfigGeneratorConfig config = new BuildConfigGeneratorConfig();
        generator = new BuildConfigGenerator(config);
    }

    @Test
    public void testShouldSetAutoBuildCategory() {
        GAV gav = new GAV("gid", "aid", "1.0.0");

        Project project = new Project();
        project.setGavs(Set.of(gav));
        project.setDependencies(Collections.emptySet());

        FoundProject foundProject = new FoundProject();
        foundProject.setGavs(Set.of(gav));

        FoundProjects foundProjects = new FoundProjects();
        foundProjects.setFoundProjects(Set.of(foundProject));

        DependencyResult result = new DependencyResult();
        result.setTopLevelProjects(Set.of(project));

        List<BuildConfig> buildConfigs = generator.generateConfigs(result, foundProjects);

        assertEquals(1, buildConfigs.size());

        BuildConfig buildConfig = buildConfigs.get(0);

        assertEquals("AUTO", buildConfig.getParameters().get("BUILD_CATEGORY"));
    }

}
