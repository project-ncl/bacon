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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@XmlRootElement
public class LicenseSummary {

    private List<DependencyElement> dependencies;

    public LicenseSummary() {
        dependencies = Collections.emptyList();
    }

    public LicenseSummary(List<DependencyElement> dependencies) {
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    public List<DependencyElement> getDependencies() {
        return dependencies;
    }

    @XmlElement(name = "dependency")
    @XmlElementWrapper
    public void setDependencies(List<DependencyElement> dependencies) {
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    public String toXmlString() throws JAXBException {
        StringWriter stringWriter = new StringWriter();
        JAXBContext jaxbContext = JAXBContext.newInstance(LicenseSummary.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(this, stringWriter);

        return stringWriter.toString();
    }

    @Override
    public String toString() {
        return String.format("%s{dependencies=%s}", LicenseSummary.class.getSimpleName(), dependencies);
    }

}
