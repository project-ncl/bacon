package org.jboss.pnc.bacon.pig.impl.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryUtilsTest {

    @Test
    void testRecursivelyDeleteEmptyFolder(@TempDir Path tmpDir) {
        // temp dirs created
        // <tmpdir>/<test1>/<test2>/<test4>
        // <tmpdir>/<test3>

        // when running recursivelyDeleteEmptyFolder applied on test2, nothing should be deleted
        // when running recursivelyDeleteEmptyFolder applied on test4, only folders test1, test2 and test4 should be
        // deleted
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
    void testRemoveExcludedArtifacts(@TempDir Path tmpDir) throws IOException {
        File toRemove = new File(
                Paths.get(tmpDir.toFile().getAbsolutePath(), "test1", "com", "1.1").toAbsolutePath().toString());
        File another = new File(
                Paths.get(tmpDir.toFile().getAbsolutePath(), "test1", "org", "1.1").toAbsolutePath().toString());
        toRemove.mkdirs();
        another.mkdirs();
        File sampleFile = new File(toRemove, "sample");
        sampleFile.createNewFile();

        RepositoryUtils.removeExcludedArtifacts(new File(tmpDir.toFile(), "test1"), Collections.singletonList("com.*"));

        assertTrue(another.exists());
    }

    @Test
    void testConvertArtifactPathToIdentifier() {
        String path = "test.me.here/letmego/1.2.3/letmego-1.2.3.tar";
        String identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("test.me.here:letmego:tar:1.2.3", identifier);

        path = "test.me.here/letmego/1.2.3/letmego-1.2.3-docs.zip";
        identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("test.me.here:letmego:zip:1.2.3:docs", identifier);

        path = "test-me-here/let-me-go/1.2.3-some/let-me-go-1.2.3-some-nice-docs.zip";
        identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("test-me-here:let-me-go:zip:1.2.3-some:nice-docs", identifier);

        path = "org/jboss/jboss-parent/43.0.0.redhat-00001/jboss-parent-43.0.0.redhat-00001.pom";
        identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("org.jboss:jboss-parent:pom:43.0.0.redhat-00001", identifier);

        path = "io/hawt/examples/hawtio-example-quarkus-keycloak/4.1.0.redhat-00015/hawtio-example-quarkus-keycloak-4.1.0.redhat-00015.jar";
        identifier = RepositoryUtils.convertArtifactPathToIdentifier(path);
        assertEquals("io.hawt.examples:hawtio-example-quarkus-keycloak:jar:4.1.0.redhat-00015", identifier);
    }

    @Test
    void testIsCommunity() {

        // this tests that "redhat-" string appearing somewhere besides the file name does not get reported as an
        // artifact built by Red Hat
        assertTrue(
                RepositoryUtils.isCommunity(
                        new File(
                                "rhaf-camel-4.8.0.redhat-00008-for-quarkus-3.15.0.CQ2-maven-repository/maven-repository/com/hierynomus/smbj/0.13.0/smbj-0.13.0.jar")));
        assertTrue(
                RepositoryUtils.isCommunity(
                        new File(
                                "rhaf-camel-4.8.0.redhat-00008-for-quarkus-3.15.0.CQ2-maven-repository/maven-repository/com/hierynomus/smbj/0.13.0/_remote.repositories")));
        assertFalse(
                RepositoryUtils.isCommunity(
                        new File(
                                "rhaf-camel-4.8.0.redhat-00008-for-quarkus-3.15.0.CQ2-maven-repository/maven-repository/com/hierynomus/smbj/0.13.0.redhat-00001/smbj-0.13.0.redhat-00001.jar")));
        assertFalse(
                RepositoryUtils.isCommunity(
                        new File(
                                "rhaf-camel-4.8.0.redhat-00008-for-quarkus-3.15.0.CQ2-maven-repository/maven-repository/com/hierynomus/smbj/0.13.0.redhat-00001/_remote.repositories")));
    }
}
