package org.jboss.pnc.bacon.pig.impl.repo;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;
import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represent a set of resolved artifacts sharing the same GAV.
 */
public class ResolvedGav {

    /* @formatter:off */
    static final int JAR_RESOLVED = 0b001;
    static final int SOURCES_RESOLVED = 0b010;
    static final int JAVADOC_RESOLVED = 0b100;
    /* @formatter:on */

    private final GAV gav;
    private final AtomicInteger flags = new AtomicInteger();
    private List<Artifact> artifacts = new CopyOnWriteArrayList<>();

    ResolvedGav(GAV gav) {
        this.gav = gav;
    }

    GAV getGav() {
        return gav;
    }

    boolean isRedHatVersion() {
        return gav.getVersion().contains("redhat");
    }

    Path getArtifactDirectory() {
        return artifacts.isEmpty() ? null : artifacts.get(0).getFile().toPath().getParent();
    }

    boolean isPackagingJar() {
        File pom = null;
        for (var a : artifacts) {
            if (ArtifactCoords.TYPE_POM.equals(a.getExtension())) {
                pom = a.getFile();
                break;
            }
        }
        if (pom == null) {
            throw new RuntimeException("POM for " + gav + " was not resolved");
        }
        try (FileInputStream is = new FileInputStream(pom)) {
            var model = ModelUtils.readModel(is);
            return model.getPackaging().equals("jar");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void setDefaultJarResolved() {
        setFlag(JAR_RESOLVED);
    }

    void setFlag(int flag) {
        flags.updateAndGet(i -> i | flag);
    }

    boolean isFlagSet(int flag) {
        return (flags.get() & flag) == flag;
    }

    void addArtifact(Artifact a) {
        if (!artifacts.contains(a) && a.getFile() != null) {
            artifacts.add(a);
        }
    }

    Collection<Artifact> getArtifacts() {
        return artifacts;
    }
}
