package org.jboss.pnc.bacon.pig.impl.addons.releasehandover;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.DocumentTemplate.RefMetadata;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.DeliverableType;
import org.jboss.pnc.bacon.pig.impl.addons.releasehandover.ReleaseHandover.Ref;
import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Files;

public class ReleaseHandoverGenerator {

    private DocumentTemplate documentTemplate;

    public ReleaseHandoverGenerator(DocumentTemplate documentTemplate) {
        this.documentTemplate = documentTemplate;
    }

    public void generate(List<String> licenseZipPath, List<String> repoPath, List<String> sourceZipPath) {

        var deliverables = new ArrayList<ReleaseHandover.Deliverable>();
        deliverables.add(new ReleaseHandover.Deliverable(DeliverableType.LICENSE, getRefs(licenseZipPath)));
        deliverables.add(new ReleaseHandover.Deliverable(DeliverableType.MAVEN_REPOSIRTOY, getRefs(repoPath)));
        deliverables.add(new ReleaseHandover.Deliverable(DeliverableType.SOURCES, getRefs(repoPath)));

        var ReleaseHandover = new ReleaseHandover(
                "0.0.3",
                "ibm-asmw-release-handover",
                new ReleaseHandover.Metadata(documentTemplate.owner(), Instant.now()),
                documentTemplate.signoffs(),
                documentTemplate.product(),
                documentTemplate.advisory(),
                deliverables,
                documentTemplate.releaseNotes());

        // write yaml to file
        Yaml yaml = new Yaml();
        var yamlString = yaml.dump(ReleaseHandover);
        var file = Path.of("release-handover.yaml");
        try {
            Files.write(yamlString.getBytes(StandardCharsets.UTF_8), file.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Could not write release-handover.yaml", e);
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
