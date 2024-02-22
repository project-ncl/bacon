package org.jboss.pnc.bacon.licenses.xml;

import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class LicenseSummaryTest {

    @Test
    public void shouldGetDependencies() {
        DependencyElement dependency = new DependencyElement("testG", "testA", "testV", Collections.emptySet());
        LicenseSummary summary = new LicenseSummary(Collections.singletonList(dependency));
        assertThat(summary.getDependencies()).containsOnly(dependency);
    }

    @Test
    public void shouldSetDependencies() {
        DependencyElement dependency = new DependencyElement("testG", "testA", "testV", Collections.emptySet());
        LicenseSummary summary = new LicenseSummary();
        assertThat(summary.getDependencies()).isEmpty();

        summary.setDependencies(Collections.singletonList(dependency));
        assertThat(summary.getDependencies()).containsOnly(dependency);
    }

    @Test
    public void shouldGetXmlWithoutLicenses() throws JAXBException {
        DependencyElement dependency = new DependencyElement("testG", "testA", "testV", Collections.emptySet());
        LicenseSummary summary = new LicenseSummary(Collections.singletonList(dependency));
        String xml = summary.toXmlString();
        assertThat(xml).isXmlEqualTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<licenseSummary>" + "<dependencies>"
                        + "<dependency>" + "<groupId>testG</groupId>" + "<artifactId>testA</artifactId>"
                        + "<version>testV</version>" + "<licenses/>" + "</dependency>" + "</dependencies>"
                        + "</licenseSummary>");
    }

    @Test
    public void shouldGetXmlWithLicense() throws JAXBException {
        LicenseElement license = new LicenseElement("licenseName", "licenseUrl");
        DependencyElement dependency = new DependencyElement("testG", "testA", "testV", Collections.singleton(license));
        LicenseSummary summary = new LicenseSummary(Collections.singletonList(dependency));
        String xml = summary.toXmlString();
        assertThat(xml).isXmlEqualTo(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<licenseSummary>" + "<dependencies>"
                        + "<dependency>" + "<groupId>testG</groupId>" + "<artifactId>testA</artifactId>"
                        + "<version>testV</version>" + "<licenses>" + "<license>" + "<name>licenseName</name>"
                        + "<url>licenseUrl</url>" + "</license>" + "</licenses>" + "</dependency>" + "</dependencies>"
                        + "</licenseSummary>");
    }

}
