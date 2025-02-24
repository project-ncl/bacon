package org.jboss.pnc.bacon.pig.impl.repo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.ArtifactVisit;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.ArtifactVisitor;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.FileSystemArtifactVisit;
import org.jboss.pnc.bacon.pig.impl.repo.visitor.VisitableArtifactRepository;

import io.quarkus.domino.RhVersionPattern;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;

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

    public VisitableArtifactRepository toVisitableRepository() {
        return new VisitableRepository(getRhArtifacts());
    }

    private Collection<org.jboss.pnc.bacon.pig.impl.utils.GAV> getRhArtifacts() {
        Collection<org.jboss.pnc.bacon.pig.impl.utils.GAV> result = new ArrayList<>();
        for (var resolvedGav : getResolvedArtifacts()) {
            if (!RhVersionPattern.isRhVersion(resolvedGav.getGav().getVersion())) {
                continue;
            }
            for (var c : resolvedGav.getArtifacts()) {
                result.add(
                        new org.jboss.pnc.bacon.pig.impl.utils.GAV(
                                c.getGroupId(),
                                c.getArtifactId(),
                                c.getVersion(),
                                c.getExtension(),
                                c.getClassifier()));
            }
        }
        return result;
    }

    private class VisitableRepository implements VisitableArtifactRepository {
        private final Collection<org.jboss.pnc.bacon.pig.impl.utils.GAV> artifacts;

        public VisitableRepository(Collection<org.jboss.pnc.bacon.pig.impl.utils.GAV> artifacts) {
            this.artifacts = artifacts;
        }

        @Override
        public void visit(ArtifactVisitor visitor) {
            for (var gav : artifacts) {
                visitor.visit(new ResolvedArtifactVisit(gav));
            }
        }

        private Path getArtifactPath(org.jboss.pnc.bacon.pig.impl.utils.GAV artifact) {
            var resolvedGav = resolvedArtifacts
                    .get(new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
            if (resolvedGav == null) {
                throw new IllegalArgumentException(
                        "Failed to locate " + artifact.toGapvc() + " among the resolved artifacts");
            }
            var path = resolvedGav.getArtifactDirectory().resolve(artifact.toFileName());
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Failed to locate " + path);
            }
            return path;
        }

        @Override
        public int getArtifactsTotal() {
            return artifacts.size();
        }

        private class ResolvedArtifactVisit implements ArtifactVisit {
            private final org.jboss.pnc.bacon.pig.impl.utils.GAV gav;

            public ResolvedArtifactVisit(org.jboss.pnc.bacon.pig.impl.utils.GAV gav) {
                this.gav = gav;
            }

            @Override
            public org.jboss.pnc.bacon.pig.impl.utils.GAV getGav() {
                return gav;
            }

            @Override
            public Map<String, String> getChecksums() {
                return FileSystemArtifactVisit.readArtifactChecksums(getArtifactPath(gav));
            }
        }
    }
}
