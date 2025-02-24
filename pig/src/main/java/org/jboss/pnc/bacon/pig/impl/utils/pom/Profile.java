package org.jboss.pnc.bacon.pig.impl.utils.pom;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * @author Paul Gallagher, pgallagh@redhat <br>
 *         Date: 12/10/2018
 */
public class Profile {
    List<Dependency> dependencies = new ArrayList<>();
    DependencyManagement dependencyManagement = new DependencyManagement();
    String id;

    @XmlElementWrapper
    @XmlElement(name = "dependency")
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(Dependency dep) {
        dependencies.add(dep);
    }

    public String getId() {
        return id;
    }

    @XmlElement
    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public DependencyManagement getDependencyManagement() {
        return dependencyManagement;
    }
}
