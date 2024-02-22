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

package org.jboss.pnc.bacon.licenses.sanitiser.exceptions;

import org.jboss.pnc.bacon.licenses.xml.DependencyElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;

import javax.json.JsonObject;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LicenseException {
    private String groupId;
    private String artifactId;
    private VersionMatcher versionMatcher;
    private Set<LicenseElement> licenses;

    public LicenseException(JsonObject jsonObject) {
        this.groupId = jsonObject.getString("groupId");
        Objects.requireNonNull(this.groupId, "groupId cannot be null");

        this.artifactId = jsonObject.getString("artifactId");
        Objects.requireNonNull(this.artifactId, "artifactId cannot be null");

        if (jsonObject.containsKey("version")) {
            this.versionMatcher = new ExactVersionMatcher(jsonObject.getString("version"));
        } else if (jsonObject.containsKey("version-range")) {
            this.versionMatcher = new RangeVersionMatcher(jsonObject.getString("version-range"));
        } else if (jsonObject.containsKey("version-regexp")) {
            this.versionMatcher = new RegexpVersionMatcher(jsonObject.getString("version-regexp"));
        } else {
            throw new IllegalArgumentException(
                    "License exception for " + groupId + ":" + artifactId
                            + " must contain 'version' or 'version-range' or 'version-regexp'");
        }

        this.licenses = jsonObject.getJsonArray("licenses")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(LicenseElement::new)
                .collect(Collectors.toSet());
    }

    public boolean matches(DependencyElement dependency) {
        return groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())
                && versionMatcher.matches(dependency.getVersion());
    }

    public Set<LicenseElement> getLicenses() {
        return licenses;
    }
}
