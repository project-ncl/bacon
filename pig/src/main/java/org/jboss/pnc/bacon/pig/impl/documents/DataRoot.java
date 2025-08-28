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
package org.jboss.pnc.bacon.pig.impl.documents;

import java.util.Collection;

import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/5/17
 */
public class DataRoot {
    private PigConfiguration pigConfiguration;
    private Deliverables deliverables;
    private String duplicates;
    private String repositoryContents;
    private Collection<PncBuild> builds;
    private String pncUrl;

    @java.lang.SuppressWarnings("all")
    public PigConfiguration getPigConfiguration() {
        return this.pigConfiguration;
    }

    @java.lang.SuppressWarnings("all")
    public Deliverables getDeliverables() {
        return this.deliverables;
    }

    @java.lang.SuppressWarnings("all")
    public String getDuplicates() {
        return this.duplicates;
    }

    @java.lang.SuppressWarnings("all")
    public String getRepositoryContents() {
        return this.repositoryContents;
    }

    @java.lang.SuppressWarnings("all")
    public Collection<PncBuild> getBuilds() {
        return this.builds;
    }

    @java.lang.SuppressWarnings("all")
    public String getPncUrl() {
        return this.pncUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setPigConfiguration(final PigConfiguration pigConfiguration) {
        this.pigConfiguration = pigConfiguration;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeliverables(final Deliverables deliverables) {
        this.deliverables = deliverables;
    }

    @java.lang.SuppressWarnings("all")
    public void setDuplicates(final String duplicates) {
        this.duplicates = duplicates;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepositoryContents(final String repositoryContents) {
        this.repositoryContents = repositoryContents;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuilds(final Collection<PncBuild> builds) {
        this.builds = builds;
    }

    @java.lang.SuppressWarnings("all")
    public void setPncUrl(final String pncUrl) {
        this.pncUrl = pncUrl;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DataRoot))
            return false;
        final DataRoot other = (DataRoot) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$pigConfiguration = this.getPigConfiguration();
        final java.lang.Object other$pigConfiguration = other.getPigConfiguration();
        if (this$pigConfiguration == null ? other$pigConfiguration != null
                : !this$pigConfiguration.equals(other$pigConfiguration))
            return false;
        final java.lang.Object this$deliverables = this.getDeliverables();
        final java.lang.Object other$deliverables = other.getDeliverables();
        if (this$deliverables == null ? other$deliverables != null : !this$deliverables.equals(other$deliverables))
            return false;
        final java.lang.Object this$duplicates = this.getDuplicates();
        final java.lang.Object other$duplicates = other.getDuplicates();
        if (this$duplicates == null ? other$duplicates != null : !this$duplicates.equals(other$duplicates))
            return false;
        final java.lang.Object this$repositoryContents = this.getRepositoryContents();
        final java.lang.Object other$repositoryContents = other.getRepositoryContents();
        if (this$repositoryContents == null ? other$repositoryContents != null
                : !this$repositoryContents.equals(other$repositoryContents))
            return false;
        final java.lang.Object this$builds = this.getBuilds();
        final java.lang.Object other$builds = other.getBuilds();
        if (this$builds == null ? other$builds != null : !this$builds.equals(other$builds))
            return false;
        final java.lang.Object this$pncUrl = this.getPncUrl();
        final java.lang.Object other$pncUrl = other.getPncUrl();
        if (this$pncUrl == null ? other$pncUrl != null : !this$pncUrl.equals(other$pncUrl))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DataRoot;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $pigConfiguration = this.getPigConfiguration();
        result = result * PRIME + ($pigConfiguration == null ? 43 : $pigConfiguration.hashCode());
        final java.lang.Object $deliverables = this.getDeliverables();
        result = result * PRIME + ($deliverables == null ? 43 : $deliverables.hashCode());
        final java.lang.Object $duplicates = this.getDuplicates();
        result = result * PRIME + ($duplicates == null ? 43 : $duplicates.hashCode());
        final java.lang.Object $repositoryContents = this.getRepositoryContents();
        result = result * PRIME + ($repositoryContents == null ? 43 : $repositoryContents.hashCode());
        final java.lang.Object $builds = this.getBuilds();
        result = result * PRIME + ($builds == null ? 43 : $builds.hashCode());
        final java.lang.Object $pncUrl = this.getPncUrl();
        result = result * PRIME + ($pncUrl == null ? 43 : $pncUrl.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "DataRoot(pigConfiguration=" + this.getPigConfiguration() + ", deliverables=" + this.getDeliverables()
                + ", duplicates=" + this.getDuplicates() + ", repositoryContents=" + this.getRepositoryContents()
                + ", builds=" + this.getBuilds() + ", pncUrl=" + this.getPncUrl() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public DataRoot(
            final PigConfiguration pigConfiguration,
            final Deliverables deliverables,
            final String duplicates,
            final String repositoryContents,
            final Collection<PncBuild> builds,
            final String pncUrl) {
        this.pigConfiguration = pigConfiguration;
        this.deliverables = deliverables;
        this.duplicates = duplicates;
        this.repositoryContents = repositoryContents;
        this.builds = builds;
        this.pncUrl = pncUrl;
    }

    @java.lang.SuppressWarnings("all")
    public DataRoot() {
    }
}
