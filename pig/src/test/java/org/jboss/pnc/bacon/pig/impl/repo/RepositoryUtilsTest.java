package org.jboss.pnc.bacon.pig.impl.repo;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryUtilsTest {

    @Test
    void testRecursivelyDeleteEmptyFolder() throws IOException {
        // temp dirs created
        // <tmpdir>/<test1>/<test2>/<test4>
        // <tmpdir>/<test3>

        // when running recursivelyDeleteEmptyFolder applied on test2, nothing should be deleted
        // when running recursivelyDeleteEmptyFolder applied on test4, only folders test1, test2 and test4 should be
        // deleted
        Path tmpDir = Files.createTempDirectory("tmpDir");
        File test1 = new File(Paths.get(tmpDir.toFile().getAbsolutePath(), "test1").toAbsolutePath().toString());
        File test2 = new File(
                Paths.get(tmpDir.toFile().getAbsolutePath(), "test1", "test2").toAbsolutePath().toString());
        File test3 = new File(Paths.get(tmpDir.toFile().getAbsolutePath(), "test3").toAbsolutePath().toString());
        File test4 = new File(
                Paths.get(tmpDir.toFile().getAbsolutePath(), "test1", "test2", "test4").toAbsolutePath().toString());
        test3.mkdirs();
        test4.mkdirs();
        RepositoryUtils.recursivelyDeleteEmptyFolder(test2);
        assertTrue(test2.exists());

        RepositoryUtils.recursivelyDeleteEmptyFolder(test4);
        assertFalse(test1.exists());
        assertFalse(test2.exists());
        assertFalse(test4.exists());
        assertTrue(tmpDir.toFile().exists());
        assertTrue(test3.exists());
    }

    @Test
    void testConvertArtifactPathToIdentifier() {
        String path = "/tmp/repository/test.me.here/letmego/1.2.3/letmego-1.2.3.tar";
        String identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("test.me.here:letmego:tar:1.2.3", identifier);

        path = "/tmp/repository/test.me.here/letmego/1.2.3/letmego-1.2.3-docs.zip";
        identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("test.me.here:letmego:zip:1.2.3:docs", identifier);

        path = "/tmp/repository/test-me-here/let-me-go/1.2.3-some/let-me-go-1.2.3-some-nice-docs.zip";
        identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("test-me-here:let-me-go:zip:1.2.3-some:nice-docs", identifier);
    }
}
