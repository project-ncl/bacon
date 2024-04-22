package org.jboss.pnc.bacon.pig.impl.repo;

import io.quarkus.maven.dependency.ArtifactCoords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Maven repository generation summary for the BOM resolution strategy
 */
class BomRepoGenerationSummary {

    private static final Logger log = LoggerFactory.getLogger(BomRepoGenerationSummary.class);

    private Collection<ArtifactCoords> missingSources = List.of();
    private Collection<ResolutionFailure> failedToResolveAdditionalArtifacts = List.of();
    private Collection<ArtifactCoords> jarsAccompanyingOrphanedPoms = List.of();
    private Collection<ResolutionFailure> jarsFailedToResolvedForOrphanedPoms = List.of();
    private Collection<ResolutionFailure> dependencyResolutionFailures = List.of();

    void addDependencyResolutionFailure(ArtifactCoords coords, Throwable e) {
        if (dependencyResolutionFailures.isEmpty()) {
            dependencyResolutionFailures = new ConcurrentLinkedQueue<>();
        }
        dependencyResolutionFailures.add(new ResolutionFailure(coords, e));
    }

    void addMissingSources(ArtifactCoords coords) {
        if (missingSources.isEmpty()) {
            missingSources = new ConcurrentLinkedQueue<>();
        }
        missingSources.add(coords);
    }

    void addFailedToResolveAdditionalArtifact(ArtifactCoords coords, Throwable e) {
        if (failedToResolveAdditionalArtifacts.isEmpty()) {
            failedToResolveAdditionalArtifacts = new ConcurrentLinkedQueue<>();
        }
        failedToResolveAdditionalArtifacts.add(new ResolutionFailure(coords, e));
    }

    void addJarAccompanyingOrphanedPom(ArtifactCoords coords) {
        if (jarsAccompanyingOrphanedPoms.isEmpty()) {
            jarsAccompanyingOrphanedPoms = new ConcurrentLinkedQueue<>();
        }
        jarsAccompanyingOrphanedPoms.add(coords);
    }

    void addFailedToResolveJarAccompanyingOrphanedPom(ArtifactCoords coords, Throwable e) {
        if (jarsFailedToResolvedForOrphanedPoms.isEmpty()) {
            jarsFailedToResolvedForOrphanedPoms = new ConcurrentLinkedQueue<>();
        }
        jarsFailedToResolvedForOrphanedPoms.add(new ResolutionFailure(coords, e));
    }

    /**
     * Checks whether there have been failures and if so, logs them and throws a {@link RuntimeException}
     */
    void assertNoErrors() {
        boolean failed = false;
        if (!failedToResolveAdditionalArtifacts.isEmpty()) {
            failed = true;
            log.error("= Additional JARs failed to resolve =");
            for (var e : failedToResolveAdditionalArtifacts) {
                if (e.error != null) {
                    log.error(e.coords.toCompactCoords(), e.error);
                } else {
                    log.error(e.coords.toCompactCoords());
                }
            }
        }

        if (!dependencyResolutionFailures.isEmpty()) {
            failed = true;
            log.error("= Dependency resolution failures =");
            for (var e : dependencyResolutionFailures) {
                if (e.error != null) {
                    log.error(e.coords.toCompactCoords(), e.error);
                } else {
                    log.error(e.coords.toCompactCoords());
                }
            }
        }

        if (failed) {
            throw new RuntimeException("Maven repository generation failed, please see the summary logged above");
        }
    }

    /**
     * Logs a summary of the performed operations, including warnings and errors by calling {@link #assertNoErrors()} at
     * the end.
     */
    void logSummary() {

        if (!missingSources.isEmpty()) {
            log.warn("= Sources JARs failed to resolve =");
            for (var a : missingSources) {
                log.warn(a.toCompactCoords());
            }
        }

        if (!jarsFailedToResolvedForOrphanedPoms.isEmpty()) {
            log.warn("= JARs for orphaned POMs failed to resolve =");
            for (var a : jarsFailedToResolvedForOrphanedPoms) {
                log.warn(a.coords.toCompactCoords());
            }
        }

        if (!jarsAccompanyingOrphanedPoms.isEmpty()) {
            log.info("= JARs added to accompany orphaned POMs =");
            for (var a : jarsAccompanyingOrphanedPoms) {
                log.info(a.toCompactCoords());
            }
        }
        assertNoErrors();
    }

    static class ResolutionFailure {
        final ArtifactCoords coords;
        final Throwable error;

        ResolutionFailure(ArtifactCoords coords, Throwable e) {
            this.coords = coords;
            this.error = e;
        }
    }
}
