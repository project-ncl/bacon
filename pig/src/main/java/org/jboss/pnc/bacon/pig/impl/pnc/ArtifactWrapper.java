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

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import org.jboss.pnc.bacon.pig.impl.utils.FileDownloadUtils;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.enums.RepositoryType;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/13/17
 */
public class ArtifactWrapper {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactWrapper.class);
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

    @java.lang.SuppressWarnings("all")
    public String getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getGapv() {
        return this.gapv;
    }

    @java.lang.SuppressWarnings("all")
    public String getFileName() {
        return this.fileName;
    }

    @java.lang.SuppressWarnings("all")
    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getMd5() {
        return this.md5;
    }

    @java.lang.SuppressWarnings("all")
    public String getSha1() {
        return this.sha1;
    }

    @java.lang.SuppressWarnings("all")
    public String getSha256() {
        return this.sha256;
    }

    @java.lang.SuppressWarnings("all")
    public RepositoryType getRepositoryType() {
        return this.repositoryType;
    }

    @java.lang.SuppressWarnings("all")
    public ArtifactWrapper() {
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ArtifactWrapper))
            return false;
        final ArtifactWrapper other = (ArtifactWrapper) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$gapv = this.getGapv();
        final java.lang.Object other$gapv = other.getGapv();
        if (this$gapv == null ? other$gapv != null : !this$gapv.equals(other$gapv))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ArtifactWrapper;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $gapv = this.getGapv();
        result = result * PRIME + ($gapv == null ? 43 : $gapv.hashCode());
        return result;
    }
}
