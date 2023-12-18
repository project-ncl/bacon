package org.jboss.pnc.bacon.pig.impl.repo;

import io.quarkus.maven.dependency.GAV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Formatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Artifact processing progress tracker
 */
class ArtifactProgressTracker {
    private static final Logger log = LoggerFactory.getLogger(ArtifactProgressTracker.class);
    private static final String FORMAT_BASE = "[%s/%s %.2f%%] ";

    private final String messagePrefix;
    private volatile int total;
    private final AtomicInteger counter = new AtomicInteger();

    ArtifactProgressTracker(String messagePrefix) {
        this(messagePrefix, 0);
    }

    ArtifactProgressTracker(String messagePrefix, int total) {
        this.messagePrefix = messagePrefix;
        this.total = total;
    }

    void setTotal(int total) {
        this.total = total;
    }

    void finalized(String groupId, String artifactId, String classifier, String type, String version) {
        var sb = initMessage().append(groupId).append(':').append(artifactId).append(':');
        if (classifier != null && !classifier.isEmpty()) {
            sb.append(classifier).append(':');
        }
        if (!"jar".equals(type)) {
            if (classifier == null || classifier.isEmpty()) {
                sb.append(':');
            }
            sb.append(type).append(':');
        }
        log.info(sb.append(version).toString());
    }

    void finalized(GAV artifact) {
        log.info(
                initMessage().append(artifact.getGroupId())
                        .append(':')
                        .append(artifact.getArtifactId())
                        .append(':')
                        .append(artifact.getVersion())
                        .toString());
    }

    private StringBuilder initMessage() {
        var current = counter.incrementAndGet();
        var sb = new StringBuilder(160);
        if (total > 0) {
            var percentage = ((double) current * 100) / total;
            var formatter = new Formatter(sb);
            formatter.format(FORMAT_BASE, current, total, percentage);
        }
        return sb.append(messagePrefix);
    }
}
