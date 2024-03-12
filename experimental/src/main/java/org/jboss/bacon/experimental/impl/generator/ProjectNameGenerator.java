/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bacon.experimental.impl.generator;

import lombok.extern.slf4j.Slf4j;
import org.jboss.bacon.experimental.impl.dependencies.Project;
import org.jboss.da.model.rest.GA;
import org.jboss.da.model.rest.GAV;
import org.jboss.pnc.common.version.SuffixedVersion;
import org.jboss.pnc.common.version.VersionParser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ProjectNameGenerator {
    private final VersionParser versionParser = new VersionParser("redhat", "temporary-redhat");

    private Set<Project> allProjects;
    private Map<GA, Set<Project>> projectsByGA;

    public void nameProjects(Set<Project> projects) {
        allProjects = new HashSet<>();
        projectsByGA = new HashMap<>();
        addProjects(projects);
        for (Project project : allProjects) {
            nameProject(project);
        }
    }

    private void addProjects(Set<Project> projects) {
        for (Project project : projects) {
            allProjects.add(project);
            GA ga = project.getFirstGAV().getGA();
            projectsByGA.computeIfAbsent(ga, g -> new HashSet<>()).add(project);
            addProjects(project.getDependencies());
        }
    }

    private void nameProject(Project project) {
        GAV gav = project.getFirstGAV();
        String version = resolveVersionForName(project, gav);
        String name = gav.getGroupId() + "-" + gav.getArtifactId() + "-" + version + "-AUTOBUILD";

        project.setName(name);
    }

    private String resolveVersionForName(Project project, GAV gav) {
        SuffixedVersion version = versionParser.parse(gav.getVersion());
        if (isThereConflictingProject(project)) {
            log.error("Project " + gav + " has a duplicate project without the redhat suffix.");
            project.setConflictingName(true);
            return gav.getVersion();
        }
        if (!version.isSuffixed()) {
            return gav.getVersion();
        }
        return version.unsuffixedVersion();
    }

    private boolean isThereConflictingProject(Project project) {
        GAV gav = project.getFirstGAV();
        GA ga = gav.getGA();
        SuffixedVersion version = versionParser.parse(gav.getVersion());
        Set<Project> projects = projectsByGA.get(ga);
        for (Project other : projects) {
            if (other.equals(project)) {
                continue;
            }
            SuffixedVersion otherVersion = versionParser.parse(other.getFirstGAV().getVersion());
            if (otherVersion.unsuffixedVersion().equals(version.unsuffixedVersion())) {
                return true;
            }
        }
        return false;
    }

}
