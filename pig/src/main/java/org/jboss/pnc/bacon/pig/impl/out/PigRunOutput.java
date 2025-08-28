/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pig.impl.out;

import java.util.Collection;

import org.jboss.pnc.bacon.pig.impl.config.GroupBuildInfo;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.dto.GroupBuild;

public class PigRunOutput {
    private String fullVersion;
    private GroupBuild groupBuild;
    private Collection<PncBuild> builds;
    private String releaseDirName;
    private String releasePath;

    public PigRunOutput(String fullVersion, GroupBuildInfo groupBuildInfo, String releaseDirName, String releasePath) {
        this.fullVersion = fullVersion;
        this.groupBuild = groupBuildInfo.getGroupBuild();
        this.builds = groupBuildInfo.getBuilds().values();
        this.releaseDirName = releaseDirName;
        this.releasePath = releasePath;
    }

    @java.lang.SuppressWarnings("all")
    public String getFullVersion() {
        return this.fullVersion;
    }

    @java.lang.SuppressWarnings("all")
    public GroupBuild getGroupBuild() {
        return this.groupBuild;
    }

    @java.lang.SuppressWarnings("all")
    public Collection<PncBuild> getBuilds() {
        return this.builds;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleaseDirName() {
        return this.releaseDirName;
    }

    @java.lang.SuppressWarnings("all")
    public String getReleasePath() {
        return this.releasePath;
    }

    @java.lang.SuppressWarnings("all")
    public void setFullVersion(final String fullVersion) {
        this.fullVersion = fullVersion;
    }

    @java.lang.SuppressWarnings("all")
    public void setGroupBuild(final GroupBuild groupBuild) {
        this.groupBuild = groupBuild;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuilds(final Collection<PncBuild> builds) {
        this.builds = builds;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleaseDirName(final String releaseDirName) {
        this.releaseDirName = releaseDirName;
    }

    @java.lang.SuppressWarnings("all")
    public void setReleasePath(final String releasePath) {
        this.releasePath = releasePath;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PigRunOutput))
            return false;
        final PigRunOutput other = (PigRunOutput) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$fullVersion = this.getFullVersion();
        final java.lang.Object other$fullVersion = other.getFullVersion();
        if (this$fullVersion == null ? other$fullVersion != null : !this$fullVersion.equals(other$fullVersion))
            return false;
        final java.lang.Object this$groupBuild = this.getGroupBuild();
        final java.lang.Object other$groupBuild = other.getGroupBuild();
        if (this$groupBuild == null ? other$groupBuild != null : !this$groupBuild.equals(other$groupBuild))
            return false;
        final java.lang.Object this$builds = this.getBuilds();
        final java.lang.Object other$builds = other.getBuilds();
        if (this$builds == null ? other$builds != null : !this$builds.equals(other$builds))
            return false;
        final java.lang.Object this$releaseDirName = this.getReleaseDirName();
        final java.lang.Object other$releaseDirName = other.getReleaseDirName();
        if (this$releaseDirName == null ? other$releaseDirName != null
                : !this$releaseDirName.equals(other$releaseDirName))
            return false;
        final java.lang.Object this$releasePath = this.getReleasePath();
        final java.lang.Object other$releasePath = other.getReleasePath();
        if (this$releasePath == null ? other$releasePath != null : !this$releasePath.equals(other$releasePath))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PigRunOutput;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $fullVersion = this.getFullVersion();
        result = result * PRIME + ($fullVersion == null ? 43 : $fullVersion.hashCode());
        final java.lang.Object $groupBuild = this.getGroupBuild();
        result = result * PRIME + ($groupBuild == null ? 43 : $groupBuild.hashCode());
        final java.lang.Object $builds = this.getBuilds();
        result = result * PRIME + ($builds == null ? 43 : $builds.hashCode());
        final java.lang.Object $releaseDirName = this.getReleaseDirName();
        result = result * PRIME + ($releaseDirName == null ? 43 : $releaseDirName.hashCode());
        final java.lang.Object $releasePath = this.getReleasePath();
        result = result * PRIME + ($releasePath == null ? 43 : $releasePath.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PigRunOutput(fullVersion=" + this.getFullVersion() + ", groupBuild=" + this.getGroupBuild()
                + ", builds=" + this.getBuilds() + ", releaseDirName=" + this.getReleaseDirName() + ", releasePath="
                + this.getReleasePath() + ")";
    }
}
