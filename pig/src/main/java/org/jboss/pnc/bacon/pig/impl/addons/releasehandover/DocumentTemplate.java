package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public record DocumentTemplate(
        String owner,
        List<ReleaseHandover.Signoffs> signoffs,
        ReleaseHandover.Product product,
        List<ReleaseHandover.Advisory> advisory,
        String refUrlPrefix,
        Map<String /* filename preffix as key */, RefMetadata> refsMetadata,
        String releaseNotes) {

    public static DocumentTemplate fromMap(Map<String, ?> map) {
        var mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
        return mapper.convertValue(map, DocumentTemplate.class);
    }

    public record RefMetadata(String description, List<String> destinations) { }

}