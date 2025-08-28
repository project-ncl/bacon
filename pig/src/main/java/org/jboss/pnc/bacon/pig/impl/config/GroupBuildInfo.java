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
package org.jboss.pnc.bacon.pig.impl.config;

import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.dto.GroupBuild;

public class GroupBuildInfo {
    private GroupBuild groupBuild;
    private Map<String, PncBuild> builds;

    @java.lang.SuppressWarnings("all")
    public GroupBuild getGroupBuild() {
        return this.groupBuild;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, PncBuild> getBuilds() {
        return this.builds;
    }

    @java.lang.SuppressWarnings("all")
    public void setGroupBuild(final GroupBuild groupBuild) {
        this.groupBuild = groupBuild;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuilds(final Map<String, PncBuild> builds) {
        this.builds = builds;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GroupBuildInfo))
            return false;
        final GroupBuildInfo other = (GroupBuildInfo) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$groupBuild = this.getGroupBuild();
        final java.lang.Object other$groupBuild = other.getGroupBuild();
        if (this$groupBuild == null ? other$groupBuild != null : !this$groupBuild.equals(other$groupBuild))
            return false;
        final java.lang.Object this$builds = this.getBuilds();
        final java.lang.Object other$builds = other.getBuilds();
        if (this$builds == null ? other$builds != null : !this$builds.equals(other$builds))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof GroupBuildInfo;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $groupBuild = this.getGroupBuild();
        result = result * PRIME + ($groupBuild == null ? 43 : $groupBuild.hashCode());
        final java.lang.Object $builds = this.getBuilds();
        result = result * PRIME + ($builds == null ? 43 : $builds.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "GroupBuildInfo(groupBuild=" + this.getGroupBuild() + ", builds=" + this.getBuilds() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public GroupBuildInfo(final GroupBuild groupBuild, final Map<String, PncBuild> builds) {
        this.groupBuild = groupBuild;
        this.builds = builds;
    }
}
