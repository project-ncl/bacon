package org.jboss.pnc.bacon.licenses.sanitiser;

import org.jboss.pnc.bacon.licenses.xml.DependencyElement;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ExceptionLicenseSanitiserTest {

    @Mock
    private LicenseSanitiser mockLicenseSanitiser;

    private ExceptionLicenseSanitiser exceptionLicenseSanitiser;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);

        exceptionLicenseSanitiser = new ExceptionLicenseSanitiser("rh-license-exceptions.json", mockLicenseSanitiser);
    }

    @Test
    public void shouldFixDependencyWithSingleLicense() {
        DependencyElement dependencyElement = new DependencyElement(
                "testGroupId",
                "testArtifactId",
                "testVersion",
                Collections.emptySet());

        DependencyElement fixedDependencyElement = exceptionLicenseSanitiser.fix(dependencyElement);

        assertThat(fixedDependencyElement).isEqualTo(dependencyElement);
        assertThat(fixedDependencyElement.getLicenses()).hasSize(1);

        Collection<LicenseElement> fixedLicenseElements = fixedDependencyElement.getLicenses();
        assertThat(fixedLicenseElements)
                .containsOnly(new LicenseElement("Test License Name", "http://test-license.com"));

        verify(mockLicenseSanitiser, times(0)).fix(any());
    }

    @Test
    public void shouldFixDependencyWithTwoLicenses() {
        DependencyElement dependencyElement = new DependencyElement(
                "testGroupId2",
                "testArtifactId2",
                "testVersion2",
                Collections.emptySet());

        DependencyElement fixedDependencyElement = exceptionLicenseSanitiser.fix(dependencyElement);

        assertThat(fixedDependencyElement).isEqualTo(dependencyElement);
        assertThat(fixedDependencyElement.getLicenses()).hasSize(2);

        Collection<LicenseElement> fixedLicenseElements = fixedDependencyElement.getLicenses();
        assertThat(fixedLicenseElements).containsOnly(
                new LicenseElement("Test License Name", "http://test-license.com"),
                new LicenseElement("Test License Name 2", "http://test-license-2.com"));

        verify(mockLicenseSanitiser, times(0)).fix(any());
    }

    @Test
    public void shouldFixDependencyFallingIntoVersionRange() {
        DependencyElement dependencyElement = new DependencyElement(
                "testGroupId3",
                "testArtifactId3",
                "1.5.0",
                Collections.emptySet());

        DependencyElement fixedDependencyElement = exceptionLicenseSanitiser.fix(dependencyElement);

        assertThat(fixedDependencyElement).isEqualTo(dependencyElement);
        assertThat(fixedDependencyElement.getLicenses()).hasSize(1);

        Collection<LicenseElement> fixedLicenseElements = fixedDependencyElement.getLicenses();
        assertThat(fixedLicenseElements)
                .containsOnly(new LicenseElement("Test License Name", "http://test-license.com"));

        verify(mockLicenseSanitiser, times(0)).fix(any());
    }

    @Test
    public void shouldFixDependencyMatchingVersionRegexp() {
        DependencyElement dependencyElement = new DependencyElement(
                "testGroupId4",
                "testArtifactId4",
                "1.0.0-redhat-1",
                Collections.emptySet());

        DependencyElement fixedDependencyElement = exceptionLicenseSanitiser.fix(dependencyElement);

        assertThat(fixedDependencyElement).isEqualTo(dependencyElement);
        assertThat(fixedDependencyElement.getLicenses()).hasSize(1);

        Collection<LicenseElement> fixedLicenseElements = fixedDependencyElement.getLicenses();
        assertThat(fixedLicenseElements)
                .containsOnly(new LicenseElement("Test License Name", "http://test-license.com"));

        verify(mockLicenseSanitiser, times(0)).fix(any());
    }

    @Test
    public void shouldDelegateUnknownLicense() {
        DependencyElement dependencyElement = new DependencyElement("", "", "", Collections.emptySet());

        DependencyElement fixedDependencyElement = exceptionLicenseSanitiser.fix(dependencyElement);

        assertThat(fixedDependencyElement).isNull();

        verify(mockLicenseSanitiser).fix(dependencyElement);
    }

}
