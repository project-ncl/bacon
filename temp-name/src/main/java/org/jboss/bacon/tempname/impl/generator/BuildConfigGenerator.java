package org.jboss.bacon.tempname.impl.generator;

import org.jboss.bacon.tempname.impl.config.BuildConfigGeneratorConfig;
import org.jboss.bacon.tempname.impl.dependencies.DependencyResult;
import org.jboss.bacon.tempname.impl.dependencies.Project;
import org.jboss.bacon.tempname.impl.projectfinder.FoundProject;
import org.jboss.bacon.tempname.impl.projectfinder.FoundProjects;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.bacon.pig.impl.mapping.BuildConfigMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BuildConfigGenerator {
    public BuildConfigGenerator(BuildConfigGeneratorConfig buildConfigGeneratorConfig) {
        throw new UnsupportedOperationException();
    }

    public List<BuildConfig> generateConfigs(DependencyResult dependencies, FoundProjects foundProjects) {
        List<BuildConfig> generatedConfigs = new ArrayList<>();

        Set<Project> projects = dependencies.getTopLevelProjects();
        Set<GAV> gavsToSearch = new HashSet<>();
        Iterator<Project> setIterator = projects.iterator();
        while (!projects.isEmpty() && setIterator.hasNext()) {
            gavsToSearch.addAll(setIterator.next().getGavs());
        }

        Set<FoundProject> foundProjectSet = foundProjects.getFoundProjects();
        Iterator<FoundProject> foundProjectIterator = foundProjectSet.iterator();
        BuildConfig tempBC;
        while (!foundProjectSet.isEmpty() && foundProjectIterator.hasNext()) {
            tempBC = BuildConfigMapping.toBuildConfig(
                    foundProjectIterator.next().getBuildConfig().get(),
                    BuildConfigMapping.GeneratorOptions.builder().build());
            generatedConfigs.add(tempBC);
        }
        return generatedConfigs;
    }
}
