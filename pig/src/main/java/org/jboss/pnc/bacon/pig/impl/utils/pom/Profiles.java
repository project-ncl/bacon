package org.jboss.pnc.bacon.pig.impl.utils.pom;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

/**
 * @author Paul Gallagher, pgallagh@redhat <br>
 *         Date: 12/10/2018
 */
public class Profiles {
    List<Profile> profile = new ArrayList<>();

    @XmlElement
    public List<Profile> getProfile() {
        return profile;
    }

    public void addProfile(Profile profile) {
        this.profile.add(profile);
    }
}
