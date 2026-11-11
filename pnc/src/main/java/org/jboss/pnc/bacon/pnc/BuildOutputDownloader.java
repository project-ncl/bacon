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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

import org.jboss.pnc.bacon.auth.LdapClient;
import org.jboss.pnc.bacon.auth.client.PncClientHelper;
import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.pnc.client.BifrostClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.SlsaProvenanceV1Client;
import org.jboss.pnc.common.log.LogSanitizer;
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

    /**
     * Base URL replacing the scheme and authority of the artifact URLs stored in PNC, or {@code null} to download them
     * as stored.
     */
    private final URI artifactBaseUrl;

    /**
     * Base URL of bifrost to download the build and alignment logs from, or {@code null} to download the build log from
     * the profile's {@code bifrostBaseurl} and the alignment log through PNC's redirect.
     */
    private final URI logBaseUrl;

    public BuildOutputDownloader() {
        this(new BuildSourcesDownloader(), null, null);
    }

    /**
     * @param artifactBaseUrl base URL to download built artifacts from instead of the scheme and authority of their
     *        {@code publicUrl} in PNC (for example another route to the same repository manager); the path of the
     *        {@code publicUrl} is kept. {@code null} or blank downloads the artifacts from their {@code publicUrl} as
     *        is.
     */
    public BuildOutputDownloader(String artifactBaseUrl) {
        this(artifactBaseUrl, null);
    }

    /**
     * @param artifactBaseUrl see {@link #BuildOutputDownloader(String)}
     * @param logBaseUrl base URL of bifrost ({@code scheme://host[:port]}) to download the build and alignment logs
     *        from, instead of the profile's {@code bifrostBaseurl} for the build log and the bifrost host PNC redirects
     *        to for the alignment log. {@code null} or blank keeps both as they are.
     */
    public BuildOutputDownloader(String artifactBaseUrl, String logBaseUrl) {
        this(new BuildSourcesDownloader(), parseArtifactBaseUrl(artifactBaseUrl), parseLogBaseUrl(logBaseUrl));
    }

    BuildOutputDownloader(BuildSourcesDownloader sourcesDownloader) {
        this(sourcesDownloader, null, null);
    }

    BuildOutputDownloader(BuildSourcesDownloader sourcesDownloader, URI artifactBaseUrl, URI logBaseUrl) {
        this.sourcesDownloader = sourcesDownloader;
        this.artifactBaseUrl = artifactBaseUrl;
        this.logBaseUrl = logBaseUrl;
    }

    public Path downloadBuiltArtifacts(String buildId, Path outputDir) {
        Path workDir = outputDir.resolve(buildId + "-built-artifacts");
        recreateDirectory(workDir);
        Path repositoryDir = workDir.resolve("repository");
        createDirectories(repositoryDir);

        try (BuildClient client = new BuildClient(pncConfiguration())) {
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

    public Path getProvenanceOfBuild(String buildId, boolean redacted, Path outputFile) {
        if (buildId == null || buildId.isBlank()) {
            throw new IllegalArgumentException("buildId must not be blank");
        }
        createDirectories(outputFile.getParent());

        try (SlsaProvenanceV1Client slsaProvenanceV1Client = new SlsaProvenanceV1Client(pncConfiguration())) {
            Object provenance = redacted ? slsaProvenanceV1Client.getFromBuildIdRedacted(buildId)
                    : slsaProvenanceV1Client.getFromBuildId(buildId);
            writeJson(outputFile, provenance);
            return outputFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get provenance for build with buildId " + buildId, e);
        }
    }

    public List<Path> downloadAlignmentDiff(String buildId, Path outputFile, boolean manifest) {
        if (buildId == null || buildId.isBlank()) {
            throw new IllegalArgumentException("buildId must not be blank");
        }
        createDirectories(outputFile.getParent());

        try (BuildClient client = new BuildClient(PncClientHelper.getPncConfiguration(false))) {
            Build build = client.getSpecific(buildId);
            return sourcesDownloader.downloadAlignmentDiff(build, outputFile, manifest);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to download alignment diff for build " + buildId, e);
        }
    }

    public Path downloadAllOutput(String buildId, boolean redacted, Path outputDir) {
        Path workDir = outputDir.resolve(buildId + "-build-output");
        recreateDirectory(workDir);

        Path repositoryDir = workDir.resolve("repository");
        Path logsDir = workDir.resolve("logs");
        Path sourcesDir = workDir.resolve("sources");
        createDirectories(repositoryDir, logsDir, sourcesDir);

        List<Path> downloadedArtifacts;
        Collection<Artifact> artifacts;
        Build build;
        try (BuildClient client = new BuildClient(pncConfiguration())) {
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
        getProvenanceOfBuild(buildId, redacted, workDir.resolve("provenance.json"));

        downloadBuildLog(buildId, logsDir.resolve("build.log"));
        downloadAlignmentLog(buildId, logsDir.resolve("alignment.log"));
        sourcesDownloader.downloadSources(build, sourcesDir.resolve("sources.tar.gz"));

        // Best effort: not every build has alignment changes committed to the internal repository, so a failure here
        // must not abort the full output download.
        try {
            sourcesDownloader.downloadAlignmentDiff(build, sourcesDir.resolve("alignment.diff"), false);
        } catch (RuntimeException e) {
            log.warn("Skipping alignment diff for build {}: {}", buildId, e.getMessage());
        }

        Path zipFile = outputDir.resolve("build-output.zip");
        zipDirectory(workDir, zipFile);
        return zipFile;
    }

    private List<Path> downloadArtifactsToRepository(Collection<Artifact> artifacts, Path repositoryDir) {
        if (artifactBaseUrl != null && !artifacts.isEmpty()) {
            log.info("Downloading built artifacts from {} instead of the host stored in PNC", artifactBaseUrl);
        }
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
            downloadArtifact(URI.create(publicUrl), target);
            writeChecksumSidecars(target, artifact);
            downloaded.add(target);
        }
        return downloaded;
    }

    /** Downloads an artifact from its PNC {@code publicUrl}, rebased on {@link #artifactBaseUrl} when set. */
    private void downloadArtifact(URI publicUrl, Path target) {
        if (artifactBaseUrl == null) {
            downloadUri(publicUrl, target);
            return;
        }
        URI source = rebase(publicUrl, artifactBaseUrl);
        log.debug("Downloading {} from {}", publicUrl, source);
        try {
            downloadUri(source, target);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage() + " (publicUrl in PNC: " + publicUrl + ")", e);
        }
    }

    /**
     * Parses the {@code --artifact-base-url} value: an absolute http(s) URL with a host and optionally a path prefix,
     * without user info, query or fragment. Credentials come from the Bacon config, not from the URL.
     *
     * @return the URL without trailing slashes, or {@code null} when the value is {@code null} or blank
     */
    static URI parseArtifactBaseUrl(String value) {
        return parseBaseUrl(value, "artifact base URL", true);
    }

    /**
     * Parses the {@code --log-base-url} value: like {@link #parseArtifactBaseUrl(String)} but without a path, since
     * bifrost's endpoints are resolved from the root of its host.
     */
    static URI parseLogBaseUrl(String value) {
        return parseBaseUrl(value, "log base URL", false);
    }

    private static URI parseBaseUrl(String value, String what, boolean pathAllowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(value.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid " + what + " '" + value + "': " + e.getMessage(), e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Invalid " + what + " '" + value + "': expected an absolute http(s) URL such as "
                            + "https://repository.example.com");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "Invalid " + what + " '" + value + "': user info, query and fragment are not allowed");
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
        if (!pathAllowed && !path.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid " + what + " '" + value + "': a path is not allowed, use scheme://host[:port]");
        }
        return URI.create(scheme + "://" + uri.getRawAuthority() + path);
    }

    /**
     * Replaces the scheme and authority of {@code url} with those of {@code base}, puts the path of {@code base} (if
     * any) in front of the path of {@code url}, and keeps the query of {@code url}.
     * <p>
     * Example: {@code https://pnc-repo.example.com/artifactory/builds/a.jar} rebased on
     * {@code https://other-route.example.com} is {@code https://other-route.example.com/artifactory/builds/a.jar}.
     *
     * @param base a URL returned by {@link #parseArtifactBaseUrl(String)}; {@code null} returns {@code url} unchanged
     */
    static URI rebase(URI url, URI base) {
        if (base == null) {
            return url;
        }
        String path = url.getRawPath() == null ? "" : url.getRawPath();
        String query = url.getRawQuery() == null ? "" : "?" + url.getRawQuery();
        String basePath = base.getRawPath() == null ? "" : base.getRawPath();
        return URI.create(base.getScheme() + "://" + base.getRawAuthority() + basePath + path + query);
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
        URI bifrostUri = logBaseUrl != null ? logBaseUrl
                : URI.create(Config.instance().getActiveProfile().getPnc().getBifrostBaseurl());
        BifrostClient logProcessor = new BifrostClient(bifrostUri);
        try (var writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            logProcessor.writeLog(buildId, false, maskingLineWriter(writer), BifrostClient.LogType.BUILD);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write build log for build " + buildId, e);
        }
    }

    private void downloadAlignmentLog(String buildId, Path targetFile) {
        createDirectories(targetFile.getParent());
        if (logBaseUrl != null) {
            // The same log PNC redirects /builds/<id>/logs/align to, read from bifrost at logBaseUrl.
            log.info("Downloading the alignment log from {} instead of the bifrost host PNC redirects to", logBaseUrl);
            try (var writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                new BifrostClient(logBaseUrl).writeFinalLog(buildId, "alignment-log", maskingLineWriter(writer));
            } catch (IOException | RuntimeException e) {
                throw new RuntimeException("Failed to download alignment log for build " + buildId, e);
            }
            return;
        }
        try (BuildClient buildClient = new BuildClient(pncConfiguration())) {
            Optional<InputStream> streamLogs = buildClient.getAlignLogs(buildId);
            try (var writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                if (streamLogs.isPresent()) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(streamLogs.get(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(LogSanitizer.maskKnownSecrets(line));
                            writer.newLine();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download alignment log for build " + buildId, e);
        }
    }

    /** Writes each log line with the known secrets masked. */
    private static Consumer<String> maskingLineWriter(BufferedWriter writer) {
        return line -> {
            try {
                writer.write(LogSanitizer.maskKnownSecrets(line));
                writer.newLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Configuration pncConfiguration() {
        return PncClientHelper.getPncConfiguration(PncClientHelper.credentialsAvailable());
    }

    private void downloadUri(URI uri, Path target) {
        try {
            HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            var requestBuilder = HttpRequest.newBuilder().uri(uri).GET();
            LdapClient.basicAuthHeaderValue(Config.instance().getActiveProfile().getLdapUsernamePassword())
                    .ifPresent(header -> requestBuilder.header("Authorization", header));
            HttpRequest request = requestBuilder.build();
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
