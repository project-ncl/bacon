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

import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.dto.BuildConfiguration;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/2/17
 */
public class BuildConfigData {
    private BuildConfig newConfig;
    private String id;
    private BuildConfiguration oldConfig;
    private boolean modified = false;

    // for jackson
    @Deprecated
    public BuildConfigData() {
    }

    public BuildConfigData(BuildConfig newConfig) {
        this.newConfig = newConfig;
    }

    public boolean shouldBeUpdated(boolean skipBranchCheck, boolean temporaryBuild) {
        return !newConfig.isTheSameAs(oldConfig, skipBranchCheck, temporaryBuild);
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfig getNewConfig() {
        return this.newConfig;
    }

    @java.lang.SuppressWarnings("all")
    public String getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public BuildConfiguration getOldConfig() {
        return this.oldConfig;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isModified() {
        return this.modified;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final String id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setOldConfig(final BuildConfiguration oldConfig) {
        this.oldConfig = oldConfig;
    }

    @java.lang.SuppressWarnings("all")
    public void setModified(final boolean modified) {
        this.modified = modified;
    }

    public String getEnvironmentId() {
        return newConfig.getEnvironmentId();
    }

    public String getName() {
        return newConfig.getName();
    }

    public List<String> getDependencies() {
        return newConfig.getDependencies();
    }

}
