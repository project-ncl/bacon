package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.time.Instant;
import java.util.List;

public record ReleaseHandover(
        String version,
        String type,
        Metadata metadata,
        List<Signoffs> signoffs,
        Product product,
        List<Advisory> advisory, //TODO plural: advisories ? 
        List<Deliverable> deliverables,
        String releaseNotes) {

    public record Metadata(String owner, Instant producedAt) {
    }

    public record Signoffs(String actor, List<String> refs) {
    }

    public record Product(
            String id,
            String name,
            String version,
            String release,
            Instant releaseDate) {
    }

    public record Advisory(String id, String contentType) {
    }

    public record Deliverable(DeliverableType type, List<Ref> refs) {
    }

    public record Ref(
            String url,
            String checksum,
            String filename,
            String description,
            List<String> destinations) {
    }

    public enum DeliverableType {
        MAVEN_REPOSIRTOY,
        LICENSE,
        SOURCES
    }
}
