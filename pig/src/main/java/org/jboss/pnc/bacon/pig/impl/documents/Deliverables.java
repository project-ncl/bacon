/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pig.impl.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/29/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Deliverables {
    private String repositoryZipName;
    private String sourceZipName;
    private String licenseZipName;
    private String javadocZipName;
    private String communityDependencies = "community-dependencies.csv";
    // todo smarter way of passing the final ones
    private final String repoCoordinatesName = "REPOSITORY_COORDINATES.properties";
    private final String artifactListName = "repository-artifact-list.txt";
    private final String duplicateArtifactListName = "repository-duplicate-artifact-list.txt";
    private String nvrListName;
    private String sharedContentReport = "shared-content-report.csv";
    private String offlinerFile;
}
