package org.jboss.bacon.experimental.impl.dependencies;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

public class GACTVParserTest {

    @Test
    void testFails() {
        Assertions.assertThatThrownBy(() -> GACTVParser.parse("test-artifact"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("should have at least 3 parts");
    }

    @Test
    void testGAFails() {
        Assertions.assertThatThrownBy(() -> GACTVParser.parse("io.quarkus:test-artifact"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("should have at least 3 parts");
    }

    @Test
    void testOverflowFails() {
        Assertions.assertThatThrownBy(() -> GACTVParser.parse("io.quarkus:test-artifact:foo:bar:baz:qux"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("should have at most 5 parts");
    }

    @Test
    void testGAV() {
        final ArtifactCoords artifactCoords = GACTVParser.parse("io.quarkus:test-artifact:1.1");
        assertThat(artifactCoords.getGroupId()).isEqualTo("io.quarkus");
        assertThat(artifactCoords.getArtifactId()).isEqualTo("test-artifact");
        assertThat(artifactCoords.getVersion()).isEqualTo("1.1");
        assertThat(artifactCoords.getClassifier()).isEqualTo("*");
        assertThat(artifactCoords.getType()).isEqualTo("*");
    }

    @Test
    void testGACV() {
        final ArtifactCoords artifactCoords = GACTVParser.parse("io.quarkus:test-artifact:classif:1.1");
        assertThat(artifactCoords.getGroupId()).isEqualTo("io.quarkus");
        assertThat(artifactCoords.getArtifactId()).isEqualTo("test-artifact");
        assertThat(artifactCoords.getVersion()).isEqualTo("1.1");
        assertThat(artifactCoords.getClassifier()).isEqualTo("classif");
        assertThat(artifactCoords.getType()).isEqualTo("*");
    }

    @Test
    void testGACTV() {
        final ArtifactCoords artifactCoords = GACTVParser.parse("io.quarkus:test-artifact:classif:json:1.1");
        assertThat(artifactCoords.getGroupId()).isEqualTo("io.quarkus");
        assertThat(artifactCoords.getArtifactId()).isEqualTo("test-artifact");
        assertThat(artifactCoords.getVersion()).isEqualTo("1.1");
        assertThat(artifactCoords.getClassifier()).isEqualTo("classif");
        assertThat(artifactCoords.getType()).isEqualTo("json");
    }
}
