/*
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

package org.jboss.pnc.bacon.pig.impl.pnc;

import org.jboss.pnc.bacon.pnc.client.PncClientHelper;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.enums.BuildStatus;
import org.jboss.pnc.rest.api.parameters.BuildsFilterParameters;

import java.util.Iterator;
import java.util.List;

import static java.util.Optional.of;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.query;
import static org.jboss.pnc.bacon.pig.impl.utils.PncClientUtils.toList;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/3/17
 */
public class BuildInfoCollector {
    private final BuildClient buildClient;
    private final BuildConfigurationClient buildConfigClient;

    public void addDependencies(PncBuild bd) {
        List<Artifact> artifacts = null;
        try {
            artifacts = toList(buildClient.getDependencyArtifacts(bd.getId()));
        } catch (RemoteResourceException e) {
            throw new RuntimeException("Failed to get dependency artifacts for " + bd.getId(), e);
        }

        bd.setDependencyArtifacts(artifacts);
    }

    public PncBuild getLatestBuild(String configId) {
        try {
            BuildsFilterParameters filter = new BuildsFilterParameters();
            filter.setLatest(true);
            Iterator<Build> buildIterator = buildConfigClient.getBuilds(
                  configId,
                  filter,
                  of("=desc=id"),
                  query("status==", BuildStatus.SUCCESS)
            ).iterator();

            if (!buildIterator.hasNext()) {
                throw new NoSuccessfulBuildException(configId);
            }

            Build build = buildIterator.next();

            PncBuild result = new PncBuild(build);

            buildClient.getBuildLogs(build.getId()).ifPresent(result::setBuildLog);
            result.setBuiltArtifacts(toList(buildClient.getBuiltArtifacts(build.getId())));

            return result;
        } catch (ClientException e) {
            throw new RuntimeException("Failed to get latest successful build for " + configId, e);
        }
    }

    public BuildInfoCollector() {
        buildClient = new BuildClient(PncClientHelper.getPncConfiguration());
        buildConfigClient = new BuildConfigurationClient(PncClientHelper.getPncConfiguration());
    }
}
