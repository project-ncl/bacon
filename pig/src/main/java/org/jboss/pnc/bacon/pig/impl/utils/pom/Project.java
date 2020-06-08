package org.jboss.pnc.bacon.pig.impl.utils.pom;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Paul Gallagher, pgallagh@redhat <br>
 *         Date: 12/10/2018
 */
@XmlRootElement
public class Project {
    @XmlElement
    String modelVersion = "4.0.0";
    @XmlElement
    String packaging = "pom";

    String groupId;
    String artifactId;
    String version;

    @XmlElement
    Profiles profiles = new Profiles();

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

    public Profiles getProfiles() {
        return profiles;
    }
}
