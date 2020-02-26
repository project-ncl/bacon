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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/1/17
 */
@Getter
@Setter
@ToString
public class CommunityDependency {
    @Delegate
    private final GAV gav;
    private String recommendation;
    private String availableVersions;
    private DependencyState state;
    private List<String> usedForSwarm;

    /* org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar */
    public CommunityDependency(String logLine) {
        logLine = logLine.trim();
        gav = new GAV(logLine);
    }

    public CommunityDependency(String groupId, String artifactId, String communityVersion, String packaging) {
        gav = new GAV(groupId, artifactId, communityVersion, packaging);
    }

    public String toPathSubstring() {
        return String.format("%s/%s/", getGroupId().replace('.', '/'), getArtifactId());
    }

    public void appendToCsv(FileWriter writer) {
        try {
            writer.append(toCsvLine()).append("\n");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write to the output file", e);
        }
    }

    /**
     * @return g:a:v; state; recommendation; availableVersions; usedForSwarm
     */
    private String toCsvLine() {
        return String.format(
                "%s:%s:%s; %s; %s; %s; %s",
                getGroupId(),
                getArtifactId(),
                getVersion(),
                state,
                recommendation,
                availableVersions,
                usedForSwarm);
    }

    public org.jboss.da.model.rest.GAV toDaGav() {
        return new org.jboss.da.model.rest.GAV(getGroupId(), getArtifactId(), getVersion());
    }
}
