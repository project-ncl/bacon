package org.jboss.pnc.bacon.pig.impl.utils.pom;

import javax.xml.bind.annotation.XmlElement;

/**
 * @author Paul Gallagher, pgallagh@redhat <br>
 *         Date: 12/10/2018
 */
public class Dependency {
    String groupId;
    String artifactId;
    String version;
    String classifier;
    String type;
    String scope;

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

    public String getClassifier() {
        return classifier;
    }

    @XmlElement
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getType() {
        return type;
    }

    @XmlElement
    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    @XmlElement
    public void setScope(String scope) {
        this.scope = scope;
    }
}
