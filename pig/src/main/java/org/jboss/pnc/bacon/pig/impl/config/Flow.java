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

import org.jboss.pnc.bacon.pig.impl.sources.SourcesGenerationData;
import org.jboss.pnc.bacon.pig.impl.validation.GenerationDataCheck;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Flow {
    @GenerationDataCheck
    private LicenseGenerationData licensesGeneration;
    @GenerationDataCheck
    private RepoGenerationData repositoryGeneration;
    @GenerationDataCheck
    private JavadocGenerationData javadocGeneration;
    /**
     * Add defaults to avoid having existing configurations having to define a sourceGeneration object in the flow
     * section
     */
    private SourcesGenerationData sourcesGeneration = new SourcesGenerationData();

    @java.lang.SuppressWarnings("all")
    public Flow() {
    }

    @java.lang.SuppressWarnings("all")
    public LicenseGenerationData getLicensesGeneration() {
        return this.licensesGeneration;
    }

    @java.lang.SuppressWarnings("all")
    public RepoGenerationData getRepositoryGeneration() {
        return this.repositoryGeneration;
    }

    @java.lang.SuppressWarnings("all")
    public JavadocGenerationData getJavadocGeneration() {
        return this.javadocGeneration;
    }

    /**
     * Add defaults to avoid having existing configurations having to define a sourceGeneration object in the flow
     * section
     */
    @java.lang.SuppressWarnings("all")
    public SourcesGenerationData getSourcesGeneration() {
        return this.sourcesGeneration;
    }

    @java.lang.SuppressWarnings("all")
    public void setLicensesGeneration(final LicenseGenerationData licensesGeneration) {
        this.licensesGeneration = licensesGeneration;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepositoryGeneration(final RepoGenerationData repositoryGeneration) {
        this.repositoryGeneration = repositoryGeneration;
    }

    @java.lang.SuppressWarnings("all")
    public void setJavadocGeneration(final JavadocGenerationData javadocGeneration) {
        this.javadocGeneration = javadocGeneration;
    }

    /**
     * Add defaults to avoid having existing configurations having to define a sourceGeneration object in the flow
     * section
     */
    @java.lang.SuppressWarnings("all")
    public void setSourcesGeneration(final SourcesGenerationData sourcesGeneration) {
        this.sourcesGeneration = sourcesGeneration;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Flow))
            return false;
        final Flow other = (Flow) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$licensesGeneration = this.getLicensesGeneration();
        final java.lang.Object other$licensesGeneration = other.getLicensesGeneration();
        if (this$licensesGeneration == null ? other$licensesGeneration != null
                : !this$licensesGeneration.equals(other$licensesGeneration))
            return false;
        final java.lang.Object this$repositoryGeneration = this.getRepositoryGeneration();
        final java.lang.Object other$repositoryGeneration = other.getRepositoryGeneration();
        if (this$repositoryGeneration == null ? other$repositoryGeneration != null
                : !this$repositoryGeneration.equals(other$repositoryGeneration))
            return false;
        final java.lang.Object this$javadocGeneration = this.getJavadocGeneration();
        final java.lang.Object other$javadocGeneration = other.getJavadocGeneration();
        if (this$javadocGeneration == null ? other$javadocGeneration != null
                : !this$javadocGeneration.equals(other$javadocGeneration))
            return false;
        final java.lang.Object this$sourcesGeneration = this.getSourcesGeneration();
        final java.lang.Object other$sourcesGeneration = other.getSourcesGeneration();
        if (this$sourcesGeneration == null ? other$sourcesGeneration != null
                : !this$sourcesGeneration.equals(other$sourcesGeneration))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Flow;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $licensesGeneration = this.getLicensesGeneration();
        result = result * PRIME + ($licensesGeneration == null ? 43 : $licensesGeneration.hashCode());
        final java.lang.Object $repositoryGeneration = this.getRepositoryGeneration();
        result = result * PRIME + ($repositoryGeneration == null ? 43 : $repositoryGeneration.hashCode());
        final java.lang.Object $javadocGeneration = this.getJavadocGeneration();
        result = result * PRIME + ($javadocGeneration == null ? 43 : $javadocGeneration.hashCode());
        final java.lang.Object $sourcesGeneration = this.getSourcesGeneration();
        result = result * PRIME + ($sourcesGeneration == null ? 43 : $sourcesGeneration.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "Flow(licensesGeneration=" + this.getLicensesGeneration() + ", repositoryGeneration="
                + this.getRepositoryGeneration() + ", javadocGeneration=" + this.getJavadocGeneration()
                + ", sourcesGeneration=" + this.getSourcesGeneration() + ")";
    }
}
