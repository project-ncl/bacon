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

import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 2/5/18
 */
public class AdditionalArtifactsFromBuild {
    private String from;
    private List<String> download;

    @java.lang.SuppressWarnings("all")
    public AdditionalArtifactsFromBuild() {
    }

    @java.lang.SuppressWarnings("all")
    public String getFrom() {
        return this.from;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getDownload() {
        return this.download;
    }

    @java.lang.SuppressWarnings("all")
    public void setFrom(final String from) {
        this.from = from;
    }

    @java.lang.SuppressWarnings("all")
    public void setDownload(final List<String> download) {
        this.download = download;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AdditionalArtifactsFromBuild))
            return false;
        final AdditionalArtifactsFromBuild other = (AdditionalArtifactsFromBuild) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$from = this.getFrom();
        final java.lang.Object other$from = other.getFrom();
        if (this$from == null ? other$from != null : !this$from.equals(other$from))
            return false;
        final java.lang.Object this$download = this.getDownload();
        final java.lang.Object other$download = other.getDownload();
        if (this$download == null ? other$download != null : !this$download.equals(other$download))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdditionalArtifactsFromBuild;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $from = this.getFrom();
        result = result * PRIME + ($from == null ? 43 : $from.hashCode());
        final java.lang.Object $download = this.getDownload();
        result = result * PRIME + ($download == null ? 43 : $download.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdditionalArtifactsFromBuild(from=" + this.getFrom() + ", download=" + this.getDownload() + ")";
    }
}
