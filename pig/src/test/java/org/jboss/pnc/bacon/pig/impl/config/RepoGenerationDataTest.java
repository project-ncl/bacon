package org.jboss.pnc.bacon.pig.impl.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RepoGenerationDataTest {

    @Test
    void testMergeWorksNoOverride() {
        RepoGenerationData base = new RepoGenerationData();
        base.setIncludeJavadoc(true);
        base.setIncludeLicenses(true);
        base.setIncludeMavenMetadata(true);

        RepoGenerationData override = new RepoGenerationData();

        RepoGenerationData merged = RepoGenerationData.merge(base, override);

        // test that the merged one doesn't have includeJavadoc set to false
        assertThat(merged.isIncludeJavadoc()).isEqualTo(true);
        assertThat(merged.isIncludeLicenses()).isEqualTo(true);
        assertThat(merged.isIncludeMavenMetadata()).isEqualTo(true);
    }

    @Test
    void testMergeWorksOverride() {
        RepoGenerationData base = new RepoGenerationData();
        // includeJavadoc not set
        base.setIncludeLicenses(true);
        base.setIncludeMavenMetadata(false);

        RepoGenerationData override = new RepoGenerationData();
        override.setIncludeJavadoc(true);
        override.setIncludeMavenMetadata(true);

        RepoGenerationData merged = RepoGenerationData.merge(base, override);
        assertThat(merged.isIncludeJavadoc()).isEqualTo(true);
        assertThat(merged.isIncludeLicenses()).isEqualTo(true);
        assertThat(merged.isIncludeMavenMetadata()).isEqualTo(true);
    }
}
