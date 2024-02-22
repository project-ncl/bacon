/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
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
package org.jboss.pnc.bacon.licenses.sanitiser;

import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.jboss.pnc.bacon.licenses.maven.MavenProjectFactory;
import org.jboss.pnc.bacon.licenses.xml.DependencyElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A pass-through sanitiser. If the dependency element does not have a license, the sanitiser will try to get it from a
 * maven project. Then, the dependency element will be given to the next sanitiser.
 *
 * @author <a href="mailto:michal.l.szynkiewicz@gmail.com">Michal Szynkiewicz</a>
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MavenSanitiser implements LicenseSanitiser {

    private final Logger logger = LoggerFactory.getLogger(MavenSanitiser.class);

    private final MavenProjectFactory mavenProjectFactory;

    private final LicenseSanitiser next;

    public MavenSanitiser(MavenProjectFactory mavenProjectFactory, LicenseSanitiser next) {
        this.mavenProjectFactory = mavenProjectFactory;
        this.next = next;
    }

    @Override
    public DependencyElement fix(DependencyElement dependencyElement) {
        if (dependencyElement.getLicenses().size() > 0) {
            return next.fix(dependencyElement);
        }
        return next.fix(new DependencyElement(dependencyElement, getMavenProjectLicenses(dependencyElement)));
    }

    private Set<LicenseElement> getMavenProjectLicenses(DependencyElement dependencyElement) {
        Set<LicenseElement> licenses = new HashSet<>();
        Optional<MavenProject> mavenProject = mavenProjectFactory
                .getMavenProject(dependencyElement.getArtifact(), false);
        if (mavenProject.isPresent()) {
            for (License license : mavenProject.get().getLicenses()) {
                licenses.add(new LicenseElement(license));
            }
        } else {
            logger.warn("Could not get maven project for {}", dependencyElement);
        }
        return licenses;
    }
}
