package org.jboss.bacon.experimental.impl.projectfinder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JdkVersionTest {

    @ParameterizedTest
    @CsvSource({
            "1.8, JDK_1_8",
            "1.8.0, JDK_1_8",
            "1.8.0_362, JDK_1_8",
            "1.8.0_362-b09, JDK_1_8",
            "11, JDK_11",
            "11.0, JDK_11",
            "11.0.22, JDK_11",
            "17, JDK_17",
            "17.0, JDK_17",
            "17.0.13+7, JDK_17",
            "17.0.13-7, JDK_17",
            "21, JDK_21",
            "21.0.2-13, JDK_21",
            "25, JDK_25",
            "25.0.1, JDK_25"
    })
    void fromVersionString(String input, JdkVersion expected) {
        assertThat(JdkVersion.fromVersionString(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "foobar", "abc.def" })
    void fromVersionStringReturnsNull(String input) {
        assertThat(JdkVersion.fromVersionString(input)).isNull();
    }

    @Test
    void fromMajorVersion() {
        assertThat(JdkVersion.fromMajorVersion(8)).isEqualTo(JdkVersion.JDK_1_8);
        assertThat(JdkVersion.fromMajorVersion(11)).isEqualTo(JdkVersion.JDK_11);
        assertThat(JdkVersion.fromMajorVersion(17)).isEqualTo(JdkVersion.JDK_17);
        assertThat(JdkVersion.fromMajorVersion(21)).isEqualTo(JdkVersion.JDK_21);
        assertThat(JdkVersion.fromMajorVersion(25)).isEqualTo(JdkVersion.JDK_25);
    }

    @Test
    void fromMajorVersionMapsIntermediateValues() {
        assertThat(JdkVersion.fromMajorVersion(9)).isEqualTo(JdkVersion.JDK_1_8);
        assertThat(JdkVersion.fromMajorVersion(10)).isEqualTo(JdkVersion.JDK_1_8);
        assertThat(JdkVersion.fromMajorVersion(14)).isEqualTo(JdkVersion.JDK_11);
        assertThat(JdkVersion.fromMajorVersion(19)).isEqualTo(JdkVersion.JDK_17);
        assertThat(JdkVersion.fromMajorVersion(23)).isEqualTo(JdkVersion.JDK_21);
    }

    @Test
    void fromMajorVersionMapsOlderJdksToLowest() {
        assertThat(JdkVersion.fromMajorVersion(7)).isEqualTo(JdkVersion.JDK_1_8);
        assertThat(JdkVersion.fromMajorVersion(6)).isEqualTo(JdkVersion.JDK_1_8);
        assertThat(JdkVersion.fromMajorVersion(1)).isEqualTo(JdkVersion.JDK_1_8);
    }

    @Test
    void fromMajorVersionReturnsNullForZeroOrNegative() {
        assertThat(JdkVersion.fromMajorVersion(0)).isNull();
        assertThat(JdkVersion.fromMajorVersion(-1)).isNull();
    }

    @Test
    void labelAndMajorVersion() {
        assertThat(JdkVersion.JDK_1_8.getLabel()).isEqualTo("1.8");
        assertThat(JdkVersion.JDK_1_8.getMajorVersion()).isEqualTo(8);
        assertThat(JdkVersion.JDK_17.getLabel()).isEqualTo("17");
        assertThat(JdkVersion.JDK_17.getMajorVersion()).isEqualTo(17);
    }
}
