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
package org.jboss.pnc.bacon.pig.impl.config;

import javax.validation.constraints.NotBlank;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
public class Output {
    @NotBlank
    private String releaseFile;
    @NotBlank
    private String releaseDir;

    @java.lang.SuppressWarnings("all")
    public Output() {
    }

    @java.lang.SuppressWarnings("all")
    public String getReleaseFile() {
        return this.releaseFile;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleaseDir() {
        return this.releaseDir;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleaseFile(final String releaseFile) {
        this.releaseFile = releaseFile;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleaseDir(final String releaseDir) {
        this.releaseDir = releaseDir;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Output))
            return false;
        final Output other = (Output) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$releaseFile = this.getReleaseFile();
        final java.lang.Object other$releaseFile = other.getReleaseFile();
        if (this$releaseFile == null ? other$releaseFile != null : !this$releaseFile.equals(other$releaseFile))
            return false;
        final java.lang.Object this$releaseDir = this.getReleaseDir();
        final java.lang.Object other$releaseDir = other.getReleaseDir();
        if (this$releaseDir == null ? other$releaseDir != null : !this$releaseDir.equals(other$releaseDir))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Output;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $releaseFile = this.getReleaseFile();
        result = result * PRIME + ($releaseFile == null ? 43 : $releaseFile.hashCode());
        final java.lang.Object $releaseDir = this.getReleaseDir();
        result = result * PRIME + ($releaseDir == null ? 43 : $releaseDir.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Output(releaseFile=" + this.getReleaseFile() + ", releaseDir=" + this.getReleaseDir() + ")";
    }
}
