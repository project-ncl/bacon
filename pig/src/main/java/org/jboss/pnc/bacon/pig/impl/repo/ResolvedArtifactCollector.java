package org.jboss.pnc.bacon.pig.impl.repo;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects all the artifacts resolved by a Maven artifact resolver
 */
public class ResolvedArtifactCollector extends AbstractRepositoryListener {

    private final Map<GAV, ResolvedGav> resolvedArtifacts = new ConcurrentHashMap<>();

    @Override
    public void artifactResolved(RepositoryEvent event) {
        var a = event.getArtifact();
        var resolved = resolvedArtifacts
                .computeIfAbsent(new GAV(a.getGroupId(), a.getArtifactId(), a.getVersion()), ResolvedGav::new);
        resolved.addArtifact(a);
        if (ArtifactCoords.TYPE_JAR.equals(a.getExtension())) {
            if (a.getClassifier().isEmpty()) {
                resolved.setDefaultJarResolved();
            } else if (a.getClassifier().equals("sources")) {
                resolved.setFlag(ResolvedGav.SOURCES_RESOLVED);
            } else if (a.getClassifier().equals("javadoc")) {
                resolved.setFlag(ResolvedGav.JAVADOC_RESOLVED);
            }
        }
    }

    public Collection<ResolvedGav> getResolvedArtifacts() {
        return resolvedArtifacts.values();
    }
}
