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

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 1/17/18
 */
public abstract class GenerationData<GenerationStrategyType> {
    private GenerationStrategyType strategy;
    private String sourceBuild;
    private String sourceArtifact;

    @java.lang.SuppressWarnings("all")
    public GenerationData() {
    }

    @java.lang.SuppressWarnings("all")
    public GenerationStrategyType getStrategy() {
        return this.strategy;
    }

    @java.lang.SuppressWarnings("all")
    public String getSourceBuild() {
        return this.sourceBuild;
    }

    @java.lang.SuppressWarnings("all")
    public String getSourceArtifact() {
        return this.sourceArtifact;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrategy(final GenerationStrategyType strategy) {
        this.strategy = strategy;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceBuild(final String sourceBuild) {
        this.sourceBuild = sourceBuild;
    }

    @java.lang.SuppressWarnings("all")
    public void setSourceArtifact(final String sourceArtifact) {
        this.sourceArtifact = sourceArtifact;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GenerationData))
            return false;
        final GenerationData<?> other = (GenerationData<?>) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$strategy = this.getStrategy();
        final java.lang.Object other$strategy = other.getStrategy();
        if (this$strategy == null ? other$strategy != null : !this$strategy.equals(other$strategy))
            return false;
        final java.lang.Object this$sourceBuild = this.getSourceBuild();
        final java.lang.Object other$sourceBuild = other.getSourceBuild();
        if (this$sourceBuild == null ? other$sourceBuild != null : !this$sourceBuild.equals(other$sourceBuild))
            return false;
        final java.lang.Object this$sourceArtifact = this.getSourceArtifact();
        final java.lang.Object other$sourceArtifact = other.getSourceArtifact();
        if (this$sourceArtifact == null ? other$sourceArtifact != null
                : !this$sourceArtifact.equals(other$sourceArtifact))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof GenerationData;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $strategy = this.getStrategy();
        result = result * PRIME + ($strategy == null ? 43 : $strategy.hashCode());
        final java.lang.Object $sourceBuild = this.getSourceBuild();
        result = result * PRIME + ($sourceBuild == null ? 43 : $sourceBuild.hashCode());
        final java.lang.Object $sourceArtifact = this.getSourceArtifact();
        result = result * PRIME + ($sourceArtifact == null ? 43 : $sourceArtifact.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "GenerationData(strategy=" + this.getStrategy() + ", sourceBuild=" + this.getSourceBuild()
                + ", sourceArtifact=" + this.getSourceArtifact() + ")";
    }
}
