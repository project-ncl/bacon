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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 5/25/18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JavadocGenerationData extends GenerationData<JavadocGenerationStrategy> {
    private static final Logger log = LoggerFactory.getLogger(JavadocGenerationData.class);
    private List<String> sourceBuilds = new ArrayList<>();
    private String scmRevision;
    private String generationProject;
    private String buildScript;
    // customPmeParameters Deprecated - alignmentParameters should be used
    private Set<String> customPmeParameters = new TreeSet<>();
    private Set<String> alignmentParameters = new TreeSet<>();
    private String importBom;

    public Set<String> getAlignmentParameters() {
        if (!customPmeParameters.isEmpty() && alignmentParameters.isEmpty()) {
            log.warn("[Deprecated] Please rename \'customPmeParameters\' section to \'alignmentParameters\'");
            alignmentParameters = customPmeParameters;
        }
        return alignmentParameters;
    }

    @java.lang.SuppressWarnings("all")
    public JavadocGenerationData() {
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getSourceBuilds() {
        return this.sourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public String getScmRevision() {
        return this.scmRevision;
    }

    @java.lang.SuppressWarnings("all")
    public String getGenerationProject() {
        return this.generationProject;
    }

    @java.lang.SuppressWarnings("all")
    public String getBuildScript() {
        return this.buildScript;
    }

    @java.lang.SuppressWarnings("all")
    public Set<String> getCustomPmeParameters() {
        return this.customPmeParameters;
    }

    @java.lang.SuppressWarnings("all")
    public String getImportBom() {
        return this.importBom;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceBuilds(final List<String> sourceBuilds) {
        this.sourceBuilds = sourceBuilds;
    }

    @java.lang.SuppressWarnings("all")
    public void setScmRevision(final String scmRevision) {
        this.scmRevision = scmRevision;
    }

    @java.lang.SuppressWarnings("all")
    public void setGenerationProject(final String generationProject) {
        this.generationProject = generationProject;
    }

    @java.lang.SuppressWarnings("all")
    public void setBuildScript(final String buildScript) {
        this.buildScript = buildScript;
    }

    @java.lang.SuppressWarnings("all")
    public void setCustomPmeParameters(final Set<String> customPmeParameters) {
        this.customPmeParameters = customPmeParameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setAlignmentParameters(final Set<String> alignmentParameters) {
        this.alignmentParameters = alignmentParameters;
    }

    @java.lang.SuppressWarnings("all")
    public void setImportBom(final String importBom) {
        this.importBom = importBom;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JavadocGenerationData))
            return false;
        final JavadocGenerationData other = (JavadocGenerationData) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$sourceBuilds = this.getSourceBuilds();
        final java.lang.Object other$sourceBuilds = other.getSourceBuilds();
        if (this$sourceBuilds == null ? other$sourceBuilds != null : !this$sourceBuilds.equals(other$sourceBuilds))
            return false;
        final java.lang.Object this$scmRevision = this.getScmRevision();
        final java.lang.Object other$scmRevision = other.getScmRevision();
        if (this$scmRevision == null ? other$scmRevision != null : !this$scmRevision.equals(other$scmRevision))
            return false;
        final java.lang.Object this$generationProject = this.getGenerationProject();
        final java.lang.Object other$generationProject = other.getGenerationProject();
        if (this$generationProject == null ? other$generationProject != null
                : !this$generationProject.equals(other$generationProject))
            return false;
        final java.lang.Object this$buildScript = this.getBuildScript();
        final java.lang.Object other$buildScript = other.getBuildScript();
        if (this$buildScript == null ? other$buildScript != null : !this$buildScript.equals(other$buildScript))
            return false;
        final java.lang.Object this$customPmeParameters = this.getCustomPmeParameters();
        final java.lang.Object other$customPmeParameters = other.getCustomPmeParameters();
        if (this$customPmeParameters == null ? other$customPmeParameters != null
                : !this$customPmeParameters.equals(other$customPmeParameters))
            return false;
        final java.lang.Object this$alignmentParameters = this.getAlignmentParameters();
        final java.lang.Object other$alignmentParameters = other.getAlignmentParameters();
        if (this$alignmentParameters == null ? other$alignmentParameters != null
                : !this$alignmentParameters.equals(other$alignmentParameters))
            return false;
        final java.lang.Object this$importBom = this.getImportBom();
        final java.lang.Object other$importBom = other.getImportBom();
        if (this$importBom == null ? other$importBom != null : !this$importBom.equals(other$importBom))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JavadocGenerationData;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $sourceBuilds = this.getSourceBuilds();
        result = result * PRIME + ($sourceBuilds == null ? 43 : $sourceBuilds.hashCode());
        final java.lang.Object $scmRevision = this.getScmRevision();
        result = result * PRIME + ($scmRevision == null ? 43 : $scmRevision.hashCode());
        final java.lang.Object $generationProject = this.getGenerationProject();
        result = result * PRIME + ($generationProject == null ? 43 : $generationProject.hashCode());
        final java.lang.Object $buildScript = this.getBuildScript();
        result = result * PRIME + ($buildScript == null ? 43 : $buildScript.hashCode());
        final java.lang.Object $customPmeParameters = this.getCustomPmeParameters();
        result = result * PRIME + ($customPmeParameters == null ? 43 : $customPmeParameters.hashCode());
        final java.lang.Object $alignmentParameters = this.getAlignmentParameters();
        result = result * PRIME + ($alignmentParameters == null ? 43 : $alignmentParameters.hashCode());
        final java.lang.Object $importBom = this.getImportBom();
        result = result * PRIME + ($importBom == null ? 43 : $importBom.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "JavadocGenerationData(sourceBuilds=" + this.getSourceBuilds() + ", scmRevision=" + this.getScmRevision()
                + ", generationProject=" + this.getGenerationProject() + ", buildScript=" + this.getBuildScript()
                + ", customPmeParameters=" + this.getCustomPmeParameters() + ", alignmentParameters="
                + this.getAlignmentParameters() + ", importBom=" + this.getImportBom() + ")";
    }
}
