package org.jboss.pnc.bacon.pig.impl.repo.visitor;

import org.jboss.pnc.bacon.pig.impl.repo.RepoDescriptor;
import org.jboss.pnc.bacon.pig.impl.utils.GAV;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

class FileSystemArtifactRepository implements VisitableArtifactRepository {

    /**
     * A directory that contains Maven repository content. The Maven repository content may not start at the root though
     * but be nested in some other directory.
     */
    private final Path mavenRepoPath;
    /**
     * Maven repository directory relative to the mavenRepoPath
     */
    private volatile Path mavenRepoDir;

    private final Collection<GAV> artifacts;

    /**
     * A directory that contains Maven repository content. The Maven repository content may not start at the root though
     * but be nested in some other directory.
     *
     * @param mavenRepoPath directory containing Maven repository content
     */
    FileSystemArtifactRepository(Path mavenRepoPath) {
        this.mavenRepoPath = Objects.requireNonNull(mavenRepoPath, "Maven repository path is null");
        if (!Files.isDirectory(mavenRepoPath)) {
            throw new IllegalArgumentException(mavenRepoPath + " is not a directory");
        }
        artifacts = RepoDescriptor.listArtifacts(mavenRepoPath);
    }

    @Override
    public void visit(ArtifactVisitor visitor) {
        for (var a : artifacts) {
            visitor.visit(new FileSystemArtifactVisit(this, a));
        }
    }

    @Override
    public int getArtifactsTotal() {
        return artifacts.size();
    }

    <T> T processArtifact(GAV gav, Function<Path, T> func) {
        return processArtifact(func, mavenRepoPath, gav.toUri());
    }

    private <T> T processArtifact(Function<Path, T> func, Path baseDir, String artifactRelativePath) {
        if (mavenRepoDir == null) {
            mavenRepoDir = getMavenRepoDir(baseDir, artifactRelativePath);
        }
        final Path artifactPath = mavenRepoDir.resolve(artifactRelativePath);
        if (!Files.exists(artifactPath)) {
            throw new RuntimeException("Failed to locate " + artifactPath + " in " + mavenRepoPath);
        }
        return func.apply(artifactPath);
    }

    private Path getMavenRepoDir(Path rootDir, String artifactRelativePath) {
        final AtomicReference<Path> mavenRepoDirRef = new AtomicReference<>();
        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (Files.exists(dir.resolve(artifactRelativePath))) {
                        mavenRepoDirRef.set(dir);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var mavenRepoDir = mavenRepoDirRef.get();
        if (mavenRepoDir == null) {
            throw new RuntimeException(
                    "Failed to locate Maven repository directory in " + mavenRepoPath + " containing "
                            + artifactRelativePath);
        }
        return mavenRepoDir;
    }
}
