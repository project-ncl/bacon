/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/3/17
 */
@Getter
@ToString(exclude = "buildLog")
@AllArgsConstructor
public class PncBuild {
    private static final Logger log = LoggerFactory.getLogger(PncBuild.class);

    public static final String SUCCESSFUL_STATUS = "DONE";
    public static Comparator<PncBuild> compareById = Comparator.comparingInt(PncBuild::getId);

    private Integer id;
    private Integer configId;
    private String name;
    private String scmUrl;
    private String scmRevision;
    private Integer milestoneId;
    private String status;
    @Setter
    private List<String> buildLog;
    private List<Artifact> builtArtifacts;
    private List<Artifact> dependencyArtifacts;

    public PncBuild(Map<?, ?> brAsMap) {
        id = (Integer) brAsMap.get("id");
        configId = (Integer) brAsMap.get("build_configuration_id");
        name = (String) ((Map<?, ?>) brAsMap.get("build_configuration_audited")).get("name");
        scmUrl = (String) brAsMap.get("scm_repo_url");
        scmRevision = (String) brAsMap.get("scm_revision");
        milestoneId = (Integer) brAsMap.get("product_milestone_id");
        status = (String) brAsMap.get("status");
    }

    public void setBuiltArtifacts(List<Map<String, ?>> artifacts) {
        builtArtifacts = artifacts.stream().map(Artifact::new).collect(Collectors.toList());
    }

    public void setDependencyArtifacts(List<Map<String, ?>> artifacts) {
        dependencyArtifacts = artifacts.stream().map(Artifact::new).collect(Collectors.toList());
    }

    public boolean isSuccessful() {
        return SUCCESSFUL_STATUS.equals(status);
    }

    public Artifact findArtifact(String regex) {
        List<Artifact> matches = findArtifactsMatching(a -> a.getGapv().matches(regex));
        if (matches.size() != 1) {
            throw new RuntimeException(
                    "Expecting exactly one artifact matching " + regex + ", found " + matches.size()
            );
        }
        return matches.get(0);
    }

    public Artifact findArtifactByFileName(String regex) {
        List<Artifact> matches = findArtifactsMatching(a -> a.getFileName().matches(regex));
        if (matches.size() != 1) {
            throw new RuntimeException(
                    "Expecting exactly one artifact matching " + regex + ", found " + matches.size()
            );
        }
        return matches.get(0);
    }

    public void downloadArtifact(String pattern, File downloadedZip) {
        Predicate<Artifact> query = a -> a.getFileName().matches(pattern);
        List<Artifact> artifacts = findArtifactsMatching(query);

        if (artifacts.size() != 1) {
            throw new RuntimeException("Unexpected number of artifacts to download found.\n" +
                    "Expecting one artifact matching " + pattern + " in build " + id + " , found: " + artifacts);
        }

        Artifact artifact = artifacts.get(0);
        artifact.downloadTo(downloadedZip);
    }

    private List<Artifact> findArtifactsMatching(Predicate<Artifact> query) {
        return getBuiltArtifacts()
                .stream()
                .filter(query)
                .collect(Collectors.toList());
    }
}
