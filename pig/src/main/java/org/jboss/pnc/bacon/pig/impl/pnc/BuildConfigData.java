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

import org.jboss.pnc.bacon.pig.impl.config.BuildConfig;
import org.jboss.pnc.dto.BuildConfiguration;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/2/17
 */
@Getter
public class BuildConfigData {
    @Delegate
    private BuildConfig newConfig;
    @Setter
    private String id;
    @Setter
    private BuildConfiguration oldConfig;
    @Setter
    private boolean modified = false;

    @Deprecated // for jackson
    public BuildConfigData() {
    }

    public BuildConfigData(BuildConfig newConfig) {
        this.newConfig = newConfig;
    }

    public boolean shouldBeUpdated(boolean skipBranchCheck, boolean temporaryBuild) {
        return !newConfig.isTheSameAs(oldConfig, skipBranchCheck, temporaryBuild);
    }
}
