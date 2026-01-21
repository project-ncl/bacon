package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.util.List;
import java.util.Map;

import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.Destination;

/**
 * Presets used to generate ReleaseHandover document.
 */
public record DocumentTemplate(
        String owner,
        List<ReleaseHandover.Signoffs> signoffs,
        ReleaseHandover.Product product,
        List<ReleaseHandover.Advisory> advisory,
        String refUrlPrefix,
        Map<String /* filename preffix as key */, RefMetadata> refsMetadata,
        String releaseNotes) {

    public static DocumentTemplate fromMap(Map<String, ?> map) {
        var mapper = Mapper.getYamlMapper();
        return mapper.convertValue(map, DocumentTemplate.class);
    }

    public record RefMetadata(String description, List<Destination> destinations) {
    }
}
