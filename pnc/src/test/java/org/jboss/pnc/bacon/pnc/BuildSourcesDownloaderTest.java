package org.jboss.pnc.bacon.pnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfigurationRevisionRef;
import org.junit.jupiter.api.Test;

class BuildSourcesDownloaderTest {

    @Test
    void shouldPreferBuildConfigRevisionScmRevision() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        when(build.getScmRevision()).thenReturn("build-revision");
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn("config-revision");

        assertEquals("config-revision", BuildSourcesDownloader.selectScmRevision(build));
    }

    @Test
    void shouldFallbackToBuildScmRevision() {
        Build build = mock(Build.class);
        BuildConfigurationRevisionRef revision = mock(BuildConfigurationRevisionRef.class);

        when(build.getScmRevision()).thenReturn("build-revision");
        when(build.getBuildConfigRevision()).thenReturn(revision);
        when(revision.getScmRevision()).thenReturn(null);

        assertEquals("build-revision", BuildSourcesDownloader.selectScmRevision(build));
    }

    @Test
    void shouldRecognizeGzipMagicBytes() {
        assertTrue(BuildSourcesDownloader.isGzip(new byte[] { (byte) 0x1f, (byte) 0x8b, 0x08 }));
        assertFalse(BuildSourcesDownloader.isGzip("<!DOCTYPE html>".getBytes()));
    }
}
