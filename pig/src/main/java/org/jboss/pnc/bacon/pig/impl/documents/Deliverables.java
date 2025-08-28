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

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/29/17
 */
public class Deliverables {
    private String repositoryZipName;
    private String sourceZipName;
    private String licenseZipName;
    private String javadocZipName;
    private String communityDependencies = "community-dependencies.csv";
    // todo smarter way of passing the final ones
    private final String repoCoordinatesName = "REPOSITORY_COORDINATES.properties";
    private final String artifactListName = "repository-artifact-list.txt";
    private final String duplicateArtifactListName = "repository-duplicate-artifact-list.txt";
    private String nvrListName;
    private String sharedContentReport = "shared-content-report.csv";

    @java.lang.SuppressWarnings("all")
    public String getRepositoryZipName() {
        return this.repositoryZipName;
    }

    @java.lang.SuppressWarnings("all")
    public String getSourceZipName() {
        return this.sourceZipName;
    }

    @java.lang.SuppressWarnings("all")
    public String getLicenseZipName() {
        return this.licenseZipName;
    }

    @java.lang.SuppressWarnings("all")
    public String getJavadocZipName() {
        return this.javadocZipName;
    }

    @java.lang.SuppressWarnings("all")
    public String getCommunityDependencies() {
        return this.communityDependencies;
    }

    @java.lang.SuppressWarnings("all")
    public String getRepoCoordinatesName() {
        return this.repoCoordinatesName;
    }

    @java.lang.SuppressWarnings("all")
    public String getArtifactListName() {
        return this.artifactListName;
    }

    @java.lang.SuppressWarnings("all")
    public String getDuplicateArtifactListName() {
        return this.duplicateArtifactListName;
    }

    @java.lang.SuppressWarnings("all")
    public String getNvrListName() {
        return this.nvrListName;
    }

    @java.lang.SuppressWarnings("all")
    public String getSharedContentReport() {
        return this.sharedContentReport;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepositoryZipName(final String repositoryZipName) {
        this.repositoryZipName = repositoryZipName;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceZipName(final String sourceZipName) {
        this.sourceZipName = sourceZipName;
    }

    @java.lang.SuppressWarnings("all")
    public void setLicenseZipName(final String licenseZipName) {
        this.licenseZipName = licenseZipName;
    }

    @java.lang.SuppressWarnings("all")
    public void setJavadocZipName(final String javadocZipName) {
        this.javadocZipName = javadocZipName;
    }

    @java.lang.SuppressWarnings("all")
    public void setCommunityDependencies(final String communityDependencies) {
        this.communityDependencies = communityDependencies;
    }

    @java.lang.SuppressWarnings("all")
    public void setNvrListName(final String nvrListName) {
        this.nvrListName = nvrListName;
    }

    @java.lang.SuppressWarnings("all")
    public void setSharedContentReport(final String sharedContentReport) {
        this.sharedContentReport = sharedContentReport;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Deliverables))
            return false;
        final Deliverables other = (Deliverables) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$repositoryZipName = this.getRepositoryZipName();
        final java.lang.Object other$repositoryZipName = other.getRepositoryZipName();
        if (this$repositoryZipName == null ? other$repositoryZipName != null
                : !this$repositoryZipName.equals(other$repositoryZipName))
            return false;
        final java.lang.Object this$sourceZipName = this.getSourceZipName();
        final java.lang.Object other$sourceZipName = other.getSourceZipName();
        if (this$sourceZipName == null ? other$sourceZipName != null : !this$sourceZipName.equals(other$sourceZipName))
            return false;
        final java.lang.Object this$licenseZipName = this.getLicenseZipName();
        final java.lang.Object other$licenseZipName = other.getLicenseZipName();
        if (this$licenseZipName == null ? other$licenseZipName != null
                : !this$licenseZipName.equals(other$licenseZipName))
            return false;
        final java.lang.Object this$javadocZipName = this.getJavadocZipName();
        final java.lang.Object other$javadocZipName = other.getJavadocZipName();
        if (this$javadocZipName == null ? other$javadocZipName != null
                : !this$javadocZipName.equals(other$javadocZipName))
            return false;
        final java.lang.Object this$communityDependencies = this.getCommunityDependencies();
        final java.lang.Object other$communityDependencies = other.getCommunityDependencies();
        if (this$communityDependencies == null ? other$communityDependencies != null
                : !this$communityDependencies.equals(other$communityDependencies))
            return false;
        final java.lang.Object this$repoCoordinatesName = this.getRepoCoordinatesName();
        final java.lang.Object other$repoCoordinatesName = other.getRepoCoordinatesName();
        if (this$repoCoordinatesName == null ? other$repoCoordinatesName != null
                : !this$repoCoordinatesName.equals(other$repoCoordinatesName))
            return false;
        final java.lang.Object this$artifactListName = this.getArtifactListName();
        final java.lang.Object other$artifactListName = other.getArtifactListName();
        if (this$artifactListName == null ? other$artifactListName != null
                : !this$artifactListName.equals(other$artifactListName))
            return false;
        final java.lang.Object this$duplicateArtifactListName = this.getDuplicateArtifactListName();
        final java.lang.Object other$duplicateArtifactListName = other.getDuplicateArtifactListName();
        if (this$duplicateArtifactListName == null ? other$duplicateArtifactListName != null
                : !this$duplicateArtifactListName.equals(other$duplicateArtifactListName))
            return false;
        final java.lang.Object this$nvrListName = this.getNvrListName();
        final java.lang.Object other$nvrListName = other.getNvrListName();
        if (this$nvrListName == null ? other$nvrListName != null : !this$nvrListName.equals(other$nvrListName))
            return false;
        final java.lang.Object this$sharedContentReport = this.getSharedContentReport();
        final java.lang.Object other$sharedContentReport = other.getSharedContentReport();
        if (this$sharedContentReport == null ? other$sharedContentReport != null
                : !this$sharedContentReport.equals(other$sharedContentReport))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Deliverables;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $repositoryZipName = this.getRepositoryZipName();
        result = result * PRIME + ($repositoryZipName == null ? 43 : $repositoryZipName.hashCode());
        final java.lang.Object $sourceZipName = this.getSourceZipName();
        result = result * PRIME + ($sourceZipName == null ? 43 : $sourceZipName.hashCode());
        final java.lang.Object $licenseZipName = this.getLicenseZipName();
        result = result * PRIME + ($licenseZipName == null ? 43 : $licenseZipName.hashCode());
        final java.lang.Object $javadocZipName = this.getJavadocZipName();
        result = result * PRIME + ($javadocZipName == null ? 43 : $javadocZipName.hashCode());
        final java.lang.Object $communityDependencies = this.getCommunityDependencies();
        result = result * PRIME + ($communityDependencies == null ? 43 : $communityDependencies.hashCode());
        final java.lang.Object $repoCoordinatesName = this.getRepoCoordinatesName();
        result = result * PRIME + ($repoCoordinatesName == null ? 43 : $repoCoordinatesName.hashCode());
        final java.lang.Object $artifactListName = this.getArtifactListName();
        result = result * PRIME + ($artifactListName == null ? 43 : $artifactListName.hashCode());
        final java.lang.Object $duplicateArtifactListName = this.getDuplicateArtifactListName();
        result = result * PRIME + ($duplicateArtifactListName == null ? 43 : $duplicateArtifactListName.hashCode());
        final java.lang.Object $nvrListName = this.getNvrListName();
        result = result * PRIME + ($nvrListName == null ? 43 : $nvrListName.hashCode());
        final java.lang.Object $sharedContentReport = this.getSharedContentReport();
        result = result * PRIME + ($sharedContentReport == null ? 43 : $sharedContentReport.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Deliverables(repositoryZipName=" + this.getRepositoryZipName() + ", sourceZipName="
                + this.getSourceZipName() + ", licenseZipName=" + this.getLicenseZipName() + ", javadocZipName="
                + this.getJavadocZipName() + ", communityDependencies=" + this.getCommunityDependencies()
                + ", repoCoordinatesName=" + this.getRepoCoordinatesName() + ", artifactListName="
                + this.getArtifactListName() + ", duplicateArtifactListName=" + this.getDuplicateArtifactListName()
                + ", nvrListName=" + this.getNvrListName() + ", sharedContentReport=" + this.getSharedContentReport()
                + ")";
    }

    @java.lang.SuppressWarnings("all")
    public Deliverables() {
    }

    @java.lang.SuppressWarnings("all")
    public Deliverables(
            final String repositoryZipName,
            final String sourceZipName,
            final String licenseZipName,
            final String javadocZipName,
            final String communityDependencies,
            final String nvrListName,
            final String sharedContentReport) {
        this.repositoryZipName = repositoryZipName;
        this.sourceZipName = sourceZipName;
        this.licenseZipName = licenseZipName;
        this.javadocZipName = javadocZipName;
        this.communityDependencies = communityDependencies;
        this.nvrListName = nvrListName;
        this.sharedContentReport = sharedContentReport;
    }
}
