/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pnc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pnc.client.BifrostClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper for build output collection commands.
 */
@Slf4j
public class BuildOutputDownloader {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private final BuildSourcesDownloader sourcesDownloader;

    public BuildOutputDownloader() {
        this(new BuildSourcesDownloader());
    }

    BuildOutputDownloader(BuildSourcesDownloader sourcesDownloader) {
        this.sourcesDownloader = sourcesDownloader;
    }

    public Path downloadBuiltArtifacts(String buildId, Path outputDir) {
        Path workDir = outputDir.resolve(buildId + "-built-artifacts");
        recreateDirectory(workDir);
        Path repositoryDir = workDir.resolve("repository");
        createDirectories(repositoryDir);

        try (BuildClient client = new BuildClient(PncClientHelper.getPncConfiguration(false))) {
            Collection<Artifact> artifacts = client.getBuiltArtifacts(buildId, Optional.empty(), Optional.empty())
                    .getAll();
            downloadArtifactsToRepository(artifacts, repositoryDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download built artifacts for build " + buildId, e);
        }

        Path zipFile = outputDir.resolve(buildId + "-built-artifacts.zip");
        zipDirectory(workDir, zipFile);
        return zipFile;
    }

    public Path getProvenance(String sha256, Path outputFile) {
        if (sha256 == null || sha256.isBlank()) {
            throw new IllegalArgumentException("sha256 must not be blank");
        }
        createDirectories(outputFile.getParent());
        Object provenance = fetchProvenanceViaGeneratedClient(sha256).orElseGet(() -> fetchProvenanceViaHttp(sha256));
        writeJson(outputFile, provenance);
        return outputFile;
    }

    public Path downloadAllOutput(String buildId, Path outputDir) {
        Path workDir = outputDir.resolve(buildId + "-build-output");
        recreateDirectory(workDir);

        Path repositoryDir = workDir.resolve("repository");
        Path logsDir = workDir.resolve("logs");
        Path sourcesDir = workDir.resolve("sources");
        createDirectories(repositoryDir, logsDir, sourcesDir);

        List<Path> downloadedArtifacts;
        Collection<Artifact> artifacts;
        Build build;
        try (BuildClient client = new BuildClient(PncClientHelper.getPncConfiguration(false))) {
            build = client.getSpecific(buildId);
            artifacts = client.getBuiltArtifacts(buildId, Optional.empty(), Optional.empty()).getAll();
            downloadedArtifacts = downloadArtifactsToRepository(artifacts, repositoryDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download build metadata/artifacts for build " + buildId, e);
        }

        String sha256 = firstSha256(artifacts).orElseGet(() -> firstComputedSha256(downloadedArtifacts));
        if (sha256 == null || sha256.isBlank()) {
            throw new RuntimeException("Unable to determine sha256 from built artifacts for build " + buildId);
        }
        getProvenance(sha256, workDir.resolve("provenance.json"));

        downloadBuildLog(buildId, logsDir.resolve("build.log"));
        downloadAlignmentLog(buildId, logsDir.resolve("alignment.log"));
        sourcesDownloader.downloadSources(build, sourcesDir.resolve("sources.tar.gz"));

        Path zipFile = outputDir.resolve("build-output.zip");
        zipDirectory(workDir, zipFile);
        return zipFile;
    }

    private List<Path> downloadArtifactsToRepository(Collection<Artifact> artifacts, Path repositoryDir) {
        List<Path> downloaded = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            String publicUrl = stringGetter(artifact, "getPublicUrl");
            if (publicUrl == null || publicUrl.isBlank()) {
                throw new RuntimeException(
                        "Artifact " + artifactDescription(artifact) + " has no publicUrl to download from");
            }

            Path relativePath = artifactRepositoryPath(artifact);
            Path target = safeResolve(repositoryDir, relativePath);
            createDirectories(target.getParent());
            downloadUri(URI.create(publicUrl), target);
            writeChecksumSidecars(target, artifact);
            downloaded.add(target);
        }
        return downloaded;
    }

    private void writeChecksumSidecars(Path artifactPath, Artifact artifact) {
        writeChecksumSidecar(artifactPath, "md5", stringGetter(artifact, "getMd5"));
        writeChecksumSidecar(artifactPath, "sha1", stringGetter(artifact, "getSha1"));
        writeChecksumSidecar(artifactPath, "sha256", stringGetter(artifact, "getSha256"));
    }

    private void writeChecksumSidecar(Path artifactPath, String algorithm, String checksum) {
        if (checksum == null || checksum.isBlank()) {
            log.debug(
                    "Skipping {} checksum sidecar for {} because the artifact DTO does not contain that checksum",
                    algorithm,
                    artifactPath);
            return;
        }

        Path checksumPath = checksumPath(artifactPath, algorithm);
        try {
            createDirectories(checksumPath.getParent());
            Files.writeString(checksumPath, checksum.trim(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write checksum file " + checksumPath, e);
        }
    }

    static Path checksumPath(Path artifactPath, String algorithm) {
        if (artifactPath.getFileName() == null) {
            throw new IllegalArgumentException("Artifact path has no file name: " + artifactPath);
        }

        return artifactPath.resolveSibling(artifactPath.getFileName().toString() + "." + algorithm);
    }

    private void downloadBuildLog(String buildId, Path targetFile) {
        createDirectories(targetFile.getParent());
        String bifrostBase = Config.instance().getActiveProfile().getPnc().getBifrostBaseurl();
        URI bifrostUri = URI.create(bifrostBase);
        BifrostClient logProcessor = new BifrostClient(bifrostUri);
        try (var writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            Consumer<String> lineWriter = line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            logProcessor.writeLog(buildId, false, lineWriter, BifrostClient.LogType.BUILD);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write build log for build " + buildId, e);
        }
    }

    private void downloadAlignmentLog(String buildId, Path targetFile) {
        createDirectories(targetFile.getParent());
        try (BuildClient buildClient = new BuildClient(PncClientHelper.getPncConfiguration(false))) {
            Optional<InputStream> streamLogs = buildClient.getAlignLogs(buildId);
            try (var writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                if (streamLogs.isPresent()) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(streamLogs.get(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download alignment log for build " + buildId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Object> fetchProvenanceViaGeneratedClient(String sha256) {
        try {
            Class<?> clientClass = Class.forName("org.jboss.pnc.client.SlsaProvenanceV1Client");
            Constructor<?> constructor = clientClass.getConstructor(Configuration.class);
            Object client = constructor.newInstance(PncClientHelper.getPncConfiguration(false));
            try {
                for (Method method : clientClass.getMethods()) {
                    if (!method.getName().equals("getFromArtifactDigest") || method.getParameterCount() < 1) {
                        continue;
                    }
                    Object[] args = new Object[method.getParameterCount()];
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    args[0] = sha256;
                    for (int i = 1; i < parameterTypes.length; i++) {
                        if (Optional.class.isAssignableFrom(parameterTypes[i])) {
                            args[i] = Optional.empty();
                        } else {
                            args[i] = null;
                        }
                    }
                    return Optional.ofNullable(method.invoke(client, args));
                }
            } finally {
                if (client instanceof AutoCloseable autoCloseable) {
                    autoCloseable.close();
                }
            }
            return Optional.empty();
        } catch (ClassNotFoundException e) {
            log.debug("SlsaProvenanceV1Client is not available; falling back to direct HTTP provenance lookup");
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve provenance for sha256 " + sha256, e);
        }
    }

    private Object fetchProvenanceViaHttp(String sha256) {
        String baseUrl = Config.instance().getActiveProfile().getPnc().getUrl();
        String separator = baseUrl.endsWith("/") ? "" : "/";
        String encodedSha = URLEncoder.encode(sha256, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + separator + "slsa/build-provenance/v1/artifacts?sha256=" + encodedSha);
        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw new RuntimeException(
                        "Failed to retrieve provenance for sha256 " + sha256 + ". HTTP status: "
                                + response.statusCode() + ", body: " + response.body());
            }
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve provenance for sha256 " + sha256, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while retrieving provenance for sha256 " + sha256, e);
        }
    }

    private void downloadUri(URI uri, Path target) {
        try {
            HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw new RuntimeException("Failed to download " + uri + ". HTTP status: " + response.statusCode());
            }
            try (InputStream in = response.body()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to download " + uri + " to " + target, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading " + uri + " to " + target, e);
        }
    }

    private Optional<String> firstSha256(Collection<Artifact> artifacts) {
        return artifacts.stream()
                .map(artifact -> stringGetter(artifact, "getSha256"))
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private String firstComputedSha256(List<Path> downloadedArtifacts) {
        return downloadedArtifacts.stream()
                .sorted(Comparator.comparing(Path::toString))
                .findFirst()
                .map(this::sha256)
                .orElse(null);
    }

    private String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file);
                    DigestInputStream dis = new DigestInputStream(in, digest)) {
                dis.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute sha256 for " + file, e);
        }
    }

    private void writeJson(Path file, Object value) {
        try {
            createDirectories(file.getParent());
            JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON file " + file, e);
        }
    }

    private void zipDirectory(Path sourceDirectory, Path zipFile) {
        try {
            createDirectories(zipFile.getParent());
            Files.deleteIfExists(zipFile);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = sourceDirectory.relativize(file);
                        ZipEntry entry = new ZipEntry(relative.toString().replace('\\', '/'));
                        zip.putNextEntry(entry);
                        Files.copy(file, zip);
                        zip.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZIP " + zipFile, e);
        }
    }

    private void recreateDirectory(Path directory) {
        deleteDirectory(directory);
        createDirectories(directory);
    }

    private void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete directory " + directory, e);
        }
    }

    private void createDirectories(Path... directories) {
        try {
            for (Path directory : directories) {
                if (directory != null) {
                    Files.createDirectories(directory);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories", e);
        }
    }

    static Path artifactRepositoryPath(Artifact artifact) {
        String deployPath = stringGetter(artifact, "getDeployPath");
        if (deployPath != null && !deployPath.isBlank()) {
            return Path.of(stripLeadingSlash(deployPath));
        }

        String identifier = stringGetter(artifact, "getIdentifier");
        Path mavenPath = mavenPathFromIdentifier(identifier);
        if (mavenPath != null) {
            return mavenPath;
        }

        String publicUrl = stringGetter(artifact, "getPublicUrl");
        if (publicUrl != null && !publicUrl.isBlank()) {
            String path = URI.create(publicUrl).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            if (!fileName.isBlank()) {
                return Path.of(fileName);
            }
        }

        String id = stringGetter(artifact, "getId");
        return Path.of(id == null || id.isBlank() ? "artifact" : id);
    }

    static Path mavenPathFromIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String[] parts = identifier.split(":");
        if (parts.length != 4 && parts.length != 5) {
            return null;
        }

        String groupId = parts[0];
        String artifactId = parts[1];
        String extension = parts[2];
        String classifier = parts.length == 5 ? parts[3] : null;
        String version = parts.length == 5 ? parts[4] : parts[3];

        if (groupId.isBlank() || artifactId.isBlank() || extension.isBlank() || version.isBlank()) {
            return null;
        }

        String fileName = artifactId + "-" + version
                + (classifier == null || classifier.isBlank() ? "" : "-" + classifier) + "." + extension;
        return Path.of(groupId.replace('.', '/'), artifactId, version, fileName);
    }

    static Path safeResolve(Path root, Path relativePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new RuntimeException("Refusing to write outside output directory: " + relativePath);
        }
        return resolved;
    }

    private static String stripLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private static String artifactDescription(Artifact artifact) {
        String id = stringGetter(artifact, "getId");
        String identifier = stringGetter(artifact, "getIdentifier");
        return id + (identifier == null ? "" : " (" + identifier + ")");
    }

    private static String stringGetter(Object target, String getterName) {
        if (target == null) {
            return null;
        }
        try {
            Object value = target.getClass().getMethod(getterName).invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

}
