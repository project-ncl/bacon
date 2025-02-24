package org.jboss.pnc.bacon.licenses.sanitiser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.jboss.pnc.bacon.licenses.xml.DependencyElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NoopLicenseSanitiserTest {

    @Test
    public void shouldDoNothing() {
        NoopLicenseSanitiser sanitiser = new NoopLicenseSanitiser();
        DependencyElement dependencyElement = new DependencyElement(
                "testGroupId",
                "testArtifactId",
                "testVersion",
                Collections.singleton(new LicenseElement("testLicenseName", "testLicenseUrl")));

        DependencyElement sanitisedDependencyElement = sanitiser.fix(dependencyElement);

        assertThat(sanitisedDependencyElement).isEqualTo(dependencyElement);
        assertThat(sanitisedDependencyElement.getLicenses()).isEqualTo(dependencyElement.getLicenses());
    }
}
