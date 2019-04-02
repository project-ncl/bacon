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
package org.jboss.pnc.bacon.pig.impl.repo;

import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 5/10/18
 */
public class ExternalArtifactDownloader {

    private ExternalArtifactDownloader() {
    }

    private static final String TEMPORARY_ARTIFACTS_URL;
    private static final String ARTIFACTS_URL;

    static
    {
        String indyRepoUrl = System.getenv("INDY_REPO_URL");
        if (indyRepoUrl == null) {
            indyRepoUrl = "http://indy.psi.redhat.com/";
        }
        if (!indyRepoUrl.endsWith("/")) {
            indyRepoUrl = indyRepoUrl + "/";
        }
        TEMPORARY_ARTIFACTS_URL = indyRepoUrl + "api/content/maven/group/temporary-builds";
        ARTIFACTS_URL = indyRepoUrl + "api/group/builds-untested+shared-imports+public";
    }

    public static File downloadExternalArtifact(GAV gav, Path targetRepoContents) {
        Path versionPath = targetRepoContents.resolve(gav.toVersionPath());
        versionPath.toFile().mkdirs();

        File targetPath = versionPath.resolve(gav.toFileName()).toFile();

        String indyUrl = gav.isTemporary() ? TEMPORARY_ARTIFACTS_URL : ARTIFACTS_URL;

        URI downloadUrl = URI.create(String.format("%s/%s", indyUrl, gav.toUri()));

        FileDownloadUtils.downloadTo(downloadUrl, targetPath);


        return targetPath;
    }
}
