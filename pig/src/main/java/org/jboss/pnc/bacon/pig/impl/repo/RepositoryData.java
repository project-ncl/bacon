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
package org.jboss.pnc.bacon.pig.impl.repo;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 8/29/17
 */
public class RepositoryData {
    private Collection<GAV> gavs;
    private Collection<File> files;
    private Path repositoryPath;

    @java.lang.SuppressWarnings("all")
    public RepositoryData() {
    }

    @java.lang.SuppressWarnings("all")
    public Collection<GAV> getGavs() {
        return this.gavs;
    }

    @java.lang.SuppressWarnings("all")
    public Collection<File> getFiles() {
        return this.files;
    }

    @java.lang.SuppressWarnings("all")
    public Path getRepositoryPath() {
        return this.repositoryPath;
    }

    @java.lang.SuppressWarnings("all")
    public void setGavs(final Collection<GAV> gavs) {
        this.gavs = gavs;
    }

    @java.lang.SuppressWarnings("all")
    public void setFiles(final Collection<File> files) {
        this.files = files;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepositoryPath(final Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RepositoryData))
            return false;
        final RepositoryData other = (RepositoryData) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$gavs = this.getGavs();
        final java.lang.Object other$gavs = other.getGavs();
        if (this$gavs == null ? other$gavs != null : !this$gavs.equals(other$gavs))
            return false;
        final java.lang.Object this$files = this.getFiles();
        final java.lang.Object other$files = other.getFiles();
        if (this$files == null ? other$files != null : !this$files.equals(other$files))
            return false;
        final java.lang.Object this$repositoryPath = this.getRepositoryPath();
        final java.lang.Object other$repositoryPath = other.getRepositoryPath();
        if (this$repositoryPath == null ? other$repositoryPath != null
                : !this$repositoryPath.equals(other$repositoryPath))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RepositoryData;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $gavs = this.getGavs();
        result = result * PRIME + ($gavs == null ? 43 : $gavs.hashCode());
        final java.lang.Object $files = this.getFiles();
        result = result * PRIME + ($files == null ? 43 : $files.hashCode());
        final java.lang.Object $repositoryPath = this.getRepositoryPath();
        result = result * PRIME + ($repositoryPath == null ? 43 : $repositoryPath.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "RepositoryData(gavs=" + this.getGavs() + ", files=" + this.getFiles() + ", repositoryPath="
                + this.getRepositoryPath() + ")";
    }
}
