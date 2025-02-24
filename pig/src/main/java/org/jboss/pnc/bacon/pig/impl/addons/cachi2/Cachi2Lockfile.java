package org.jboss.pnc.bacon.pig.impl.addons.cachi2;

import static org.jboss.pnc.bacon.pig.impl.addons.cachi2.YamlUtil.initYamlMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cachi2 lockfile
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Cachi2Lockfile {

    /**
     * Reads a Cachi2 lockfile.
     *
     * @param lockfile Cachi2 lock file
     * @return Java object model representation of a Cachi2 lock file
     */
    public static Cachi2Lockfile readFrom(Path lockfile) {
        try (BufferedReader reader = Files.newBufferedReader(lockfile)) {
            return initYamlMapper().readValue(reader, Cachi2Lockfile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Serializes an instance of {@link Cachi2Lockfile} to a YAML file.
     *
     * @param lockfile target YAML file
     */
    public static void persistTo(Cachi2Lockfile lockfile, Path file) {
        var parentDir = file.getParent();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            initYamlMapper().writeValue(writer, lockfile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class Cachi2Artifact {

        private String type;
        private String filename;
        private Map<String, String> attributes = new TreeMap<>();
        private String checksum;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }

        public void setGroupId(String groupId) {
            attributes.put("group_id", groupId);
        }

        public void setArtifactId(String artifactId) {
            attributes.put("artifact_id", artifactId);
        }

        public void setArtifactType(String type) {
            attributes.put("type", type);
        }

        public void setClassifier(String classifier) {
            attributes.put("classifier", classifier);
        }

        public void setVersion(String version) {
            attributes.put("version", version);
        }

        public void setRepositoryUrl(String repositoryUrl) {
            attributes.put("repository_url", repositoryUrl);
        }
    }

    private Map<String, String> metadata = new TreeMap<>();
    private List<Cachi2Artifact> artifacts = new ArrayList<>();

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<Cachi2Artifact> getArtifacts() {
        return artifacts;
    }

    public void addArtifact(Cachi2Artifact artifact) {
        artifacts.add(artifact);
    }

    public void setArtifacts(List<Cachi2Artifact> content) {
        this.artifacts = content;
    }
}
