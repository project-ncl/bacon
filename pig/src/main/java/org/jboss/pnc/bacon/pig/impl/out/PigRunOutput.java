/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.out;

import lombok.Data;
import org.jboss.pnc.bacon.pig.impl.config.GroupBuildInfo;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;
import org.jboss.pnc.dto.GroupBuild;

import java.util.Collection;

@Data
public class PigRunOutput {
    private String fullVersion;
    private GroupBuild groupBuild;
    private Collection<PncBuild> builds;
    private String releaseDirName;
    private String releasePath;

    public PigRunOutput(String fullVersion, GroupBuildInfo groupBuildInfo, String releaseDirName, String releasePath) {
        this.fullVersion = fullVersion;
        this.groupBuild = groupBuildInfo.getGroupBuild();
        this.builds = groupBuildInfo.getBuilds().values();
        this.releaseDirName = releaseDirName;
        this.releasePath = releasePath;
    }
}
