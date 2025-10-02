package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.DocumentTemplate.RefMetadata;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.DeliverableType;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.Ref;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ReleaseHandoverGenerator {

    private DocumentTemplate documentTemplate;

    public ReleaseHandoverGenerator(DocumentTemplate documentTemplate) {
        this.documentTemplate = documentTemplate;
    }

    public String generateYaml(List<String> licenseZipPath, List<String> repoPath, List<String> sourceZipPath) {

        var deliverables = new ArrayList<ReleaseHandover.Deliverable>();
        deliverables.add(new ReleaseHandover.Deliverable(DeliverableType.LICENSE, getRefs(licenseZipPath)));
        deliverables.add(new ReleaseHandover.Deliverable(DeliverableType.MAVEN_REPOSIRTOY, getRefs(repoPath)));
        deliverables.add(new ReleaseHandover.Deliverable(DeliverableType.SOURCES, getRefs(repoPath)));

        var releaseHandover = new ReleaseHandover(
                "0.0.3",
                "ibm-asmw-release-handover",
                new ReleaseHandover.Metadata(documentTemplate.owner(), Instant.now()),
                documentTemplate.signoffs(),
                documentTemplate.product(),
                documentTemplate.advisory(),
                deliverables,
                documentTemplate.releaseNotes());

        var yamlMapper = Mapper.getYamlMapper();
        try {
            return yamlMapper.writeValueAsString(releaseHandover);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }

    }

    private List<Ref> getRefs(List<String> zipsPath) {
        return zipsPath
                .stream()
                .map(f -> fileToRef(f))
                .collect(Collectors.toList());
    }

    private Ref fileToRef(String localFilePath) {
        var refMetadata = findRefMetadata(localFilePath);
        String refUrl = documentTemplate.refUrlPrefix() + localFilePath;
        // var refMeta = refMetadata.get(localFilePath);
        return new Ref(
                refUrl,
                "sha256:" + sha256(localFilePath),
                Path.of(localFilePath).getFileName().toString(),
                refMetadata.description(),
                refMetadata.destinations());
    }

    private String sha256(String filePath) {
        try (var fis = new java.io.FileInputStream(filePath)) {
            return DigestUtils.sha256Hex(fis);
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate sha256 for " + filePath, e);
        }
    }

    private RefMetadata findRefMetadata(String localFilePath) {
        var fileNameIdParts = documentTemplate.refsMetadata().keySet();
        for (var fileNameIdPart : fileNameIdParts) {
            if (localFilePath.contains(fileNameIdPart)) {
                return documentTemplate.refsMetadata().get(fileNameIdPart);
            }
        }
        throw new RuntimeException("No ref metadata found for " + localFilePath);
    }
}
