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
package org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da;

import org.jboss.pnc.bacon.pig.impl.utils.GAV;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/1/17
 */
public class CommunityDependency implements CsvExportable {
    final GAV gav;
    String recommendation;
    String availableVersions;
    DependencyState state;

    /* org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar */
    public CommunityDependency(String logLine) {
        logLine = logLine.trim();
        gav = new GAV(logLine);
    }

    public CommunityDependency(String groupId, String artifactId, String communityVersion, String packaging) {
        gav = new GAV(groupId, artifactId, communityVersion, packaging);
    }

    public CommunityDependency(GAV gav) {
        this.gav = gav;
    }

    public String toPathSubstring() {
        return String.format("%s/%s/", getGroupId().replace('.', '/'), getArtifactId());
    }

    /**
     * @return g:a:v; state; recommendation; availableVersions; usedForSwarm
     */
    public String toCsvLine() {
        return String.format(
                "%s:%s:%s; %s; %s; %s",
                getGroupId(),
                getArtifactId(),
                getVersion(),
                state,
                recommendation,
                availableVersions);
    }

    public org.jboss.da.model.rest.GAV toDaGav() {
        return new org.jboss.da.model.rest.GAV(getGroupId(), getArtifactId(), getVersion());
    }

    @java.lang.SuppressWarnings("all")
    public GAV getGav() {
        return this.gav;
    }

    @java.lang.SuppressWarnings("all")
    public String getRecommendation() {
        return this.recommendation;
    }

    @java.lang.SuppressWarnings("all")
    public String getAvailableVersions() {
        return this.availableVersions;
    }

    @java.lang.SuppressWarnings("all")
    public DependencyState getState() {
        return this.state;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecommendation(final String recommendation) {
        this.recommendation = recommendation;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvailableVersions(final String availableVersions) {
        this.availableVersions = availableVersions;
    }

    @java.lang.SuppressWarnings("all")
    public void setState(final DependencyState state) {
        this.state = state;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "CommunityDependency(gav=" + this.getGav() + ", recommendation=" + this.getRecommendation()
                + ", availableVersions=" + this.getAvailableVersions() + ", state=" + this.getState() + ")";
    }

    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public String getVersion() {
        return gav.getVersion();
    }

    public String getClassifier() {
        return gav.getClassifier();
    }
}
