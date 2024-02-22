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

package org.jboss.pnc.bacon.licenses.xml;

import org.apache.maven.artifact.Artifact;

import javax.json.JsonObject;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@XmlRootElement(name = "dependency")
@XmlType(propOrder = { "groupId", "artifactId", "version", "licenses" })
public class DependencyElement {

    private String groupId;

    private String artifactId;

    private String version;

    private Set<LicenseElement> licenses = new HashSet<>();

    private Artifact artifact;

    public DependencyElement() {
    }

    public DependencyElement(Artifact artifact) {
        this.groupId = artifact.getGroupId();
        this.artifactId = artifact.getArtifactId();
        this.version = artifact.getVersion();
        this.artifact = artifact;
    }

    public DependencyElement(DependencyElement dependencyElement) {
        this(
                dependencyElement.getGroupId(),
                dependencyElement.getArtifactId(),
                dependencyElement.getVersion(),
                dependencyElement.getLicenses());
    }

    public DependencyElement(JsonObject dependencyElementJson) {
        this.groupId = dependencyElementJson.getString("groupId");
        Objects.requireNonNull(this.groupId, "groupId cannot be null");

        this.artifactId = dependencyElementJson.getString("artifactId");
        Objects.requireNonNull(this.artifactId, "artifactId cannot be null");

        this.version = dependencyElementJson.getString("version");
        Objects.requireNonNull(this.version, "version cannot be null");

        this.licenses = dependencyElementJson.getJsonArray("licenses")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(LicenseElement::new)
                .collect(Collectors.toSet());
    }

    public DependencyElement(String groupId, String artifactId, String version, Set<LicenseElement> licenses) {
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(artifactId, "artifactId cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        Objects.requireNonNull(licenses, "licenses set cannot be null");
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.licenses = new HashSet<>(licenses.size());
        licenses.parallelStream().map(LicenseElement::new).forEach(this.licenses::add);
    }

    public DependencyElement(DependencyElement dependencyElement, Set<LicenseElement> licenses) {
        this(dependencyElement.groupId, dependencyElement.artifactId, dependencyElement.version, licenses);
    }

    public String getGroupId() {
        return groupId;
    }

    @XmlElement
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @XmlElement
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    @XmlElement
    public void setVersion(String version) {
        this.version = version;
    }

    public Set<LicenseElement> getLicenses() {
        return licenses;
    }

    @XmlElement(name = "license")
    @XmlElementWrapper
    public void setLicenses(Set<LicenseElement> licenses) {
        this.licenses = Collections.unmodifiableSet(licenses);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{groupId='%s', artifactId='%s', version='%s', licenses=%s}",
                DependencyElement.class.getSimpleName(),
                groupId,
                artifactId,
                version,
                licenses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependencyElement that = (DependencyElement) o;

        if (!groupId.equals(that.groupId)) {
            return false;
        }
        if (!artifactId.equals(that.artifactId)) {
            return false;
        }
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    public String toGavString() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }

    @XmlTransient
    public Artifact getArtifact() {
        return artifact;
    }
}
