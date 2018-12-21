/**
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
package org.jboss.pnc.bacon.pig.pnc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jboss.prod.generator.config.build.BuildConfig;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 12/2/17
 */
@Getter
public class BuildConfigData {
    @Delegate
    private final BuildConfig newConfig;
    @Setter
    private Integer id;
    private PncBuildConfig oldConfig;
    @Setter
    private boolean modified = false;

    public BuildConfigData(BuildConfig newConfig) {
        this.newConfig = newConfig;
    }

    public boolean shouldBeUpdated() {
        return !newConfig.isTheSameAs(oldConfig);
    }

    public void setOldConfig(PncBuildConfig currentConfig) {
        oldConfig = currentConfig;
        id = oldConfig.getId();
    }
}
