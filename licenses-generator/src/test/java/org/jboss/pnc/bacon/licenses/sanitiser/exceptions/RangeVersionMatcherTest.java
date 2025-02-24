package org.jboss.pnc.bacon.licenses.sanitiser.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RangeVersionMatcherTest {
    @Test
    public void singleVersion() throws Exception {
        VersionMatcher matcher = new RangeVersionMatcher("1.0.0");
        assertThat(matcher.matches("1.0.0")).isTrue();
        assertThat(matcher.matches("1.0.0-redhat")).isFalse();
        assertThat(matcher.matches("2.0.0")).isFalse();
    }

    @Test
    public void lowerBoundInclusive_upperBoundInclusive() throws Exception {
        VersionMatcher matcher = new RangeVersionMatcher("[1.0.0, 2.0.0]");
        assertThat(matcher.matches("0.5.0")).isFalse();
        assertThat(matcher.matches("1.0.0")).isTrue();
        assertThat(matcher.matches("1.0.0-redhat")).isTrue();
        assertThat(matcher.matches("1.5.0")).isTrue();
        assertThat(matcher.matches("2.0.0")).isTrue();
        assertThat(matcher.matches("2.0.0-redhat")).isFalse();
        assertThat(matcher.matches("2.5.0")).isFalse();
    }

    @Test
    public void lowerBoundExclusive_upperBoundInclusive() throws Exception {
        VersionMatcher matcher = new RangeVersionMatcher("(1.0.0, 2.0.0]");
        assertThat(matcher.matches("0.5.0")).isFalse();
        assertThat(matcher.matches("1.0.0")).isFalse();
        assertThat(matcher.matches("1.0.0-redhat")).isTrue();
        assertThat(matcher.matches("1.5.0")).isTrue();
        assertThat(matcher.matches("2.0.0")).isTrue();
        assertThat(matcher.matches("2.0.0-redhat")).isFalse();
        assertThat(matcher.matches("2.5.0")).isFalse();
    }

    @Test
    public void lowerBoundInclusive_upperBoundExclusive() throws Exception {
        VersionMatcher matcher = new RangeVersionMatcher("[1.0.0, 2.0.0)");
        assertThat(matcher.matches("0.5.0")).isFalse();
        assertThat(matcher.matches("1.0.0")).isTrue();
        assertThat(matcher.matches("1.0.0-redhat")).isTrue();
        assertThat(matcher.matches("1.5.0")).isTrue();
        assertThat(matcher.matches("2.0.0")).isFalse();
        assertThat(matcher.matches("2.0.0-redhat")).isFalse();
        assertThat(matcher.matches("2.5.0")).isFalse();
    }

    @Test
    public void lowerBoundExclusive_upperBoundExclusive() throws Exception {
        VersionMatcher matcher = new RangeVersionMatcher("(1.0.0, 2.0.0)");
        assertThat(matcher.matches("0.5.0")).isFalse();
        assertThat(matcher.matches("1.0.0")).isFalse();
        assertThat(matcher.matches("1.0.0-redhat")).isTrue();
        assertThat(matcher.matches("1.5.0")).isTrue();
        assertThat(matcher.matches("2.0.0")).isFalse();
        assertThat(matcher.matches("2.0.0-redhat")).isFalse();
        assertThat(matcher.matches("2.5.0")).isFalse();
    }
}
