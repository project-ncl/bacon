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

import java.util.List;

import org.jboss.pnc.dto.GroupConfigurationRef;
import org.jboss.pnc.dto.ProductMilestoneRef;
import org.jboss.pnc.dto.ProductVersionRef;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/4/17
 */
public class ImportResult {
    private ProductMilestoneRef milestone;
    private GroupConfigurationRef buildGroup;
    private ProductVersionRef version;
    private List<BuildConfigData> buildConfigs;

    // for jackson only
    @Deprecated
    public ImportResult() {
    }

    @java.lang.SuppressWarnings("all")
    public ProductMilestoneRef getMilestone() {
        return this.milestone;
    }

    @java.lang.SuppressWarnings("all")
    public GroupConfigurationRef getBuildGroup() {
        return this.buildGroup;
    }

    @java.lang.SuppressWarnings("all")
    public ProductVersionRef getVersion() {
        return this.version;
    }

    @java.lang.SuppressWarnings("all")
    public List<BuildConfigData> getBuildConfigs() {
        return this.buildConfigs;
    }

    @java.lang.SuppressWarnings("all")
    public void setMilestone(final ProductMilestoneRef milestone) {
        this.milestone = milestone;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildGroup(final GroupConfigurationRef buildGroup) {
        this.buildGroup = buildGroup;
    }

    @java.lang.SuppressWarnings("all")
    public void setVersion(final ProductVersionRef version) {
        this.version = version;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildConfigs(final List<BuildConfigData> buildConfigs) {
        this.buildConfigs = buildConfigs;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ImportResult))
            return false;
        final ImportResult other = (ImportResult) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$milestone = this.getMilestone();
        final java.lang.Object other$milestone = other.getMilestone();
        if (this$milestone == null ? other$milestone != null : !this$milestone.equals(other$milestone))
            return false;
        final java.lang.Object this$buildGroup = this.getBuildGroup();
        final java.lang.Object other$buildGroup = other.getBuildGroup();
        if (this$buildGroup == null ? other$buildGroup != null : !this$buildGroup.equals(other$buildGroup))
            return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version))
            return false;
        final java.lang.Object this$buildConfigs = this.getBuildConfigs();
        final java.lang.Object other$buildConfigs = other.getBuildConfigs();
        if (this$buildConfigs == null ? other$buildConfigs != null : !this$buildConfigs.equals(other$buildConfigs))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ImportResult;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $milestone = this.getMilestone();
        result = result * PRIME + ($milestone == null ? 43 : $milestone.hashCode());
        final java.lang.Object $buildGroup = this.getBuildGroup();
        result = result * PRIME + ($buildGroup == null ? 43 : $buildGroup.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $buildConfigs = this.getBuildConfigs();
        result = result * PRIME + ($buildConfigs == null ? 43 : $buildConfigs.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ImportResult(milestone=" + this.getMilestone() + ", buildGroup=" + this.getBuildGroup() + ", version="
                + this.getVersion() + ", buildConfigs=" + this.getBuildConfigs() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public ImportResult(
            final ProductMilestoneRef milestone,
            final GroupConfigurationRef buildGroup,
            final ProductVersionRef version,
            final List<BuildConfigData> buildConfigs) {
        this.milestone = milestone;
        this.buildGroup = buildGroup;
        this.version = version;
        this.buildConfigs = buildConfigs;
    }
}
