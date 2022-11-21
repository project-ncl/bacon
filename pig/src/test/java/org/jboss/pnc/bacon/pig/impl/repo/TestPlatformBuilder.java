package org.jboss.pnc.bacon.pig.impl.repo;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestPlatformBuilder {

    public static TestPlatformBuilder newInstance(Path workDir) {
        return new TestPlatformBuilder(workDir);
    }

    private final Path workDir;
    private final List<TestArtifactBuilder> artifacts = new ArrayList<>();
    private MavenArtifactResolver resolver;

    private TestPlatformBuilder(Path workDir) {
        this.workDir = workDir;
    }

    public TestPlatformBomBuilder newBom(String bomGroupId, String bomArtifactId, String bomVersion) {
        return newBom(new GACTV(bomGroupId, bomArtifactId, null, "pom", bomVersion));
    }

    public TestPlatformBomBuilder newBom(ArtifactCoords bomCoords) {
        return new TestPlatformBomBuilder(bomCoords);
    }

    public TestPlatformBuilder installArtifact(String groupId, String artifactId, String version) {
        artifacts.add(new TestArtifactBuilder(new GACTV(groupId, artifactId, version)));
        return this;
    }

    public TestPlatformBuilder installArtifact(
            String groupId,
            String artifactId,
            String classifier,
            String type,
            String version) {
        artifacts.add(new TestArtifactBuilder(new GACTV(groupId, artifactId, classifier, type, version)));
        return this;
    }

    public TestArtifactBuilder installArtifactWithDependencies(String groupId, String artifactId, String version) {
        final TestArtifactBuilder ab = new TestArtifactBuilder(new GACTV(groupId, artifactId, version));
        artifacts.add(ab);
        return ab;
    }

    public TestPlatformBuilder setMavenResolver(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    public void build() {
        if (resolver == null) {
            try {
                resolver = MavenArtifactResolver.builder().build();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize Maven artifact resolver", e);
            }
        }
        artifacts.forEach(TestArtifactBuilder::build);
    }

    public class TestPlatformBomBuilder {

        private final TestArtifactBuilder bomBuilder;

        private TestPlatformBomBuilder(ArtifactCoords bomCoords) {
            this.bomBuilder = new TestArtifactBuilder(bomCoords);
            artifacts.add(bomBuilder);
        }

        public TestPlatformBuilder platform() {
            return TestPlatformBuilder.this;
        }

        public TestPlatformBomBuilder addConstraint(String groupId, String artifactId, String version) {
            bomBuilder.addManagedDependency(groupId, artifactId, version);
            return this;
        }

        public TestPlatformBomBuilder addConstraint(
                String groupId,
                String artifactId,
                String classifier,
                String type,
                String version) {
            final Dependency d = new Dependency();
            d.setGroupId(groupId);
            d.setArtifactId(artifactId);
            d.setVersion(version);
            d.setClassifier(classifier);
            d.setType(type);
            bomBuilder.addManagedDependency(d);
            return this;
        }

        public TestPlatformBomBuilder addConstraint(Dependency d) {
            bomBuilder.addManagedDependency(d);
            return this;
        }
    }

    public class TestArtifactBuilder {

        private final Artifact coords;
        private final Model pom;

        private TestArtifactBuilder(ArtifactCoords coords) {
            this.coords = new DefaultArtifact(
                    coords.getGroupId(),
                    coords.getArtifactId(),
                    coords.getClassifier(),
                    coords.getType(),
                    coords.getVersion());
            pom = new Model();
            pom.setModelVersion("4.0.0");
            pom.setGroupId(coords.getGroupId());
            pom.setArtifactId(coords.getArtifactId());
            pom.setVersion(coords.getVersion());
            if (!coords.getType().isEmpty() && !coords.getType().equals("jar")) {
                pom.setPackaging("pom");
            }
        }

        public TestArtifactBuilder addManagedDependency(String groupId, String artifactId, String version) {
            final Dependency d = new Dependency();
            d.setGroupId(groupId);
            d.setArtifactId(artifactId);
            d.setVersion(version);
            return addManagedDependency(d);
        }

        public TestArtifactBuilder addManagedDependency(ArtifactCoords coords) {
            final Dependency d = new Dependency();
            d.setGroupId(coords.getGroupId());
            d.setArtifactId(coords.getArtifactId());
            d.setVersion(coords.getVersion());
            if (!"jar".equals(coords.getType())) {
                d.setType(coords.getType());
            }
            if (!coords.getClassifier().isEmpty()) {
                d.setClassifier(coords.getClassifier());
            }
            return addDependency(d);
        }

        public TestArtifactBuilder addManagedDependency(Dependency dep) {
            DependencyManagement dm = pom.getDependencyManagement();
            if (dm == null) {
                dm = new DependencyManagement();
                pom.setDependencyManagement(dm);
            }
            dm.addDependency(dep);
            return this;
        }

        public TestArtifactBuilder addDependency(String groupId, String artifactId, String version) {
            final Dependency d = new Dependency();
            d.setGroupId(groupId);
            d.setArtifactId(artifactId);
            d.setVersion(version);
            return addDependency(d);
        }

        public TestArtifactBuilder addDependency(ArtifactCoords coords) {
            final Dependency d = new Dependency();
            d.setGroupId(coords.getGroupId());
            d.setArtifactId(coords.getArtifactId());
            d.setVersion(coords.getVersion());
            if (!"jar".equals(coords.getType())) {
                d.setType(coords.getType());
            }
            if (!coords.getClassifier().isEmpty()) {
                d.setClassifier(coords.getClassifier());
            }
            return addDependency(d);
        }

        public TestArtifactBuilder addDependency(Dependency dep) {
            pom.addDependency(dep);
            return this;
        }

        public TestPlatformBuilder platform() {
            return TestPlatformBuilder.this;
        }

        private void build() {
            final Path localRepoDir = workDir.resolve("artifacts");
            Path artifactPath = localRepoDir.resolve(toFileName((coords)));
            try {
                Files.createDirectories(artifactPath.getParent());
                if (coords.getExtension().equals("pom")) {
                    ModelUtils.persistModel(artifactPath, pom);
                    resolver.install(coords.setFile(artifactPath.toFile()));
                } else {
                    try (BufferedWriter writer = Files.newBufferedWriter(artifactPath)) {
                        writer.write("content");
                    }
                    resolver.install(coords.setFile(artifactPath.toFile()));
                    // install the pom
                    final Artifact pomArtifact = new DefaultArtifact(
                            coords.getGroupId(),
                            coords.getArtifactId(),
                            "pom",
                            coords.getVersion());
                    artifactPath = localRepoDir.resolve(toFileName(pomArtifact));
                    ModelUtils.persistModel(artifactPath, pom);
                    resolver.install(pomArtifact.setFile(artifactPath.toFile()));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to install " + coords, e);
            }
        }

        private String toFileName(Artifact a) {
            final StringBuilder sb = new StringBuilder();
            sb.append(a.getGroupId()).append('.').append(a.getArtifactId()).append('-').append(a.getVersion());
            if (!a.getClassifier().isEmpty()) {
                sb.append('-').append(a.getVersion());
            }
            return sb.append('.').append(a.getExtension()).toString();
        }
    }
}
