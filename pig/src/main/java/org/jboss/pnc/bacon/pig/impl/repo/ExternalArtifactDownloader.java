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
import org.jboss.pnc.bacon.pig.impl.utils.indy.Indy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 5/10/18
 */
public class ExternalArtifactDownloader {

    private static final Logger log = LoggerFactory.getLogger(ExternalArtifactDownloader.class);

    private ExternalArtifactDownloader() {
    }

    public static File downloadExternalArtifact(GAV gav, Path targetRepoContents, boolean sourcesOptional) {
        File targetPath = targetPath(gav, targetRepoContents);

        return downloadExternalArtifact(gav, targetPath, sourcesOptional);
    }

    public static File downloadExternalArtifact(GAV gav, File targetPath, boolean sourcesOptional) {
        targetPath.toPath().getParent().toFile().mkdirs();

        String indyUrl = gav.isTemporary() ? Indy.getIndyTempUrl() : Indy.getIndyUrl();

        URI downloadUrl = URI.create(String.format("%s/%s", indyUrl, gav.toUri()));
        try {
            FileDownloadUtils.downloadTo(downloadUrl, targetPath);
        } catch (RuntimeException any) {
            if (sourcesOptional && "sources".equals(gav.getClassifier()) || "javadoc".equals(gav.getClassifier())) {
                log.warn("Unable to download sources for " + gav, any);
            } else {
                throw any;
            }
        }

        return targetPath;
    }

    public static File targetPath(GAV gav, Path targetRepoContents) {
        Path versionPath = targetRepoContents.resolve(gav.toVersionPath());
        return versionPath.resolve(gav.toFileName()).toFile();
    }
}
