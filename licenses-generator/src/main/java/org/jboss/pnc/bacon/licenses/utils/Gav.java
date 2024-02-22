/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
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

package org.jboss.pnc.bacon.licenses.utils;

import java.util.Objects;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class Gav {

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String type;

    public Gav(String groupId, String artifactId, String version, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version + ":" + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Gav))
            return false;
        Gav gav = (Gav) o;
        return Objects.equals(groupId, gav.groupId) && Objects.equals(artifactId, gav.artifactId)
                && Objects.equals(version, gav.version) && Objects.equals(type, gav.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, type);
    }
}
