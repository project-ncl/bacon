package org.jboss.pnc.bacon.licenses.sanitiser.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RegexpVersionMatcherTest {
    private VersionMatcher matcher = new RegexpVersionMatcher("1.0.0(-redhat-\\d+)?");

    @Test
    public void matchingString() throws Exception {
        assertThat(matcher.matches("1.0.0")).isTrue();
        assertThat(matcher.matches("1.0.0-redhat-1")).isTrue();
        assertThat(matcher.matches("1.0.0-redhat-999")).isTrue();
    }

    @Test
    public void nonmatchingString() throws Exception {
        assertThat(matcher.matches("2.0.0")).isFalse();
    }

    @Test
    public void nullString() throws Exception {
        assertThat(matcher.matches(null)).isFalse();
    }

    @Test
    public void whitespaceDifference() throws Exception {
        assertThat(matcher.matches("1.0.0 ")).isFalse();
    }
}
