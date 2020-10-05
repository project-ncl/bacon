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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.enums.RepositoryType;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/13/17
 */
@Getter
@Slf4j
@NoArgsConstructor
public class ArtifactWrapper {

    private String id;
    private String gapv;
    private String fileName;
    private String downloadUrl;
    private String md5;
    private String sha1;
    private String sha256;
    // TODO: Remove when NCL-6079 is done.
    private RepositoryType repositoryType;

    public ArtifactWrapper(Artifact artifact) {
        id = artifact.getId();
        fileName = artifact.getFilename();
        downloadUrl = artifact.getPublicUrl();
        gapv = artifact.getIdentifier();
        md5 = artifact.getMd5();
        sha1 = artifact.getSha1();
        sha256 = artifact.getSha256();
        repositoryType = artifact.getTargetRepository().getRepositoryType();
    }

    public GAV toGAV() {
        return GAV.fromColonSeparatedGAPV(gapv);
    }

    @Override
    public String toString() {
        return gapv;
    }

    public void downloadTo(File downloadedZip) {
        FileDownloadUtils.downloadTo(URI.create(downloadUrl), downloadedZip);
    }

    public void downloadToDirectory(Path parentDirPath) {
        File targetPath = parentDirPath.resolve(fileName).toFile();

        downloadTo(targetPath);
    }
}
