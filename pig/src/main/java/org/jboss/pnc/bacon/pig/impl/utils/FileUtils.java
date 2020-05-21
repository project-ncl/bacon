/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig.impl.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tools.ant.util.PermissionUtils;
import org.apache.tools.ant.util.PermissionUtils.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.Files.createTempDirectory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 7/28/17
 */
public class FileUtils {
    private FileUtils() {
    }

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static File mkTempDir(String prefix) {
        try {
            File temporaryDirectory;
            switch (OSCheck.getOperatingSystemType()) {
                case MacOS:
                    // TODO: remove when we figure out what the problem with mounting directories outside user home is
                    File tmpSpace = Paths.get("target", "tmpSpace").toFile();
                    tmpSpace.mkdirs();
                    temporaryDirectory = createTempDirectory(tmpSpace.toPath(), prefix).toFile();
                    break;
                default:
                    temporaryDirectory = createTempDirectory(prefix).toFile();
                    break;
            }
            return temporaryDirectory;
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary directory", e);
        }
    }

    public static String getCompressorType(final File file) {
        try (final InputStream is = Files.newInputStream(file.toPath());
                final BufferedInputStream bis = new BufferedInputStream(is)) {
            return CompressorStreamFactory.detect(bis);
        } catch (IOException | CompressorException e) {
            return null;
        }
    }

    public static String getCompressorType(final String filename) {
        if (BZip2Utils.isCompressedFilename(filename)) {
            return CompressorStreamFactory.BZIP2;
        } else if (GzipUtils.isCompressedFilename(filename)) {
            return CompressorStreamFactory.GZIP;
        } else if (LZMAUtils.isCompressedFilename(filename)) {
            return CompressorStreamFactory.LZMA;
        } else if (XZUtils.isCompressedFilename(filename)) {
            return CompressorStreamFactory.XZ;
        } else {
            return null;
        }
    }

    public static Collection<String> untar(final File input, final File directory) {
        log.debug("tar -xf {} -C {}", input, directory);

        final String compressorType = getCompressorType(input);

        log.debug("untar: detected compressor type: {}", compressorType);

        final List<String> entries = new ArrayList<>();

        try (final InputStream is = Files.newInputStream(input.toPath());
                final InputStream cin = compressorType != null
                        ? new CompressorStreamFactory().createCompressorInputStream(compressorType, is)
                        : is;
                final ArchiveInputStream in = new ArchiveStreamFactory()
                        .createArchiveInputStream(ArchiveStreamFactory.TAR, cin)) {
            final Path dir = directory.toPath();
            final Path canonicalDir = dir.toAbsolutePath().normalize();

            Files.createDirectories(dir);

            TarArchiveEntry entry;

            while ((entry = (TarArchiveEntry) in.getNextEntry()) != null) {
                final String entryName = entry.getName();

                entries.add(entryName);

                final Path path = dir.resolve(entryName);

                log.debug("untar: {}", path);

                final Path canonicalPath = path.toAbsolutePath().normalize();

                if (!canonicalPath.startsWith(canonicalDir)) {
                    throw new RuntimeException("Path " + path + " is outside of destination directory " + directory);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else if (entry.isLink()) {
                    final String linkName = entry.getLinkName();
                    final Path target = FileSystems.getDefault().getPath(linkName);

                    Files.createLink(path, target);
                } else if (entry.isSymbolicLink()) {
                    final String linkName = entry.getLinkName();
                    final Path target = FileSystems.getDefault().getPath(linkName);

                    Files.createSymbolicLink(path, target);
                } else if (entry.isFile()) {
                    try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                        Files.createDirectories(path.getParent());
                        IOUtils.copy(in, out);
                    }
                } else {
                    throw new RuntimeException("Unsupported file type for: " + entryName);
                }

            }
        } catch (IOException | ArchiveException | CompressorException e) {
            throw new RuntimeException("Untar of " + input + " to " + directory + " failed", e);
        }

        return entries;
    }

    public static Collection<String> tar(final File output, final File workingDirectory, final File directoryToTar) {
        final Path directory = directoryToTar.toPath();

        log.debug("tar -cf {} {}", output, directory);

        final String compressorType = getCompressorType(output.getName());

        log.debug("tar: detected compressor type: {}", compressorType);

        List<String> entries = new ArrayList<>();

        try (final OutputStream os = Files.newOutputStream(output.toPath());
                OutputStream cout = compressorType != null
                        ? new CompressorStreamFactory().createCompressorOutputStream(compressorType, os)
                        : os;
                final ArchiveOutputStream out = new ArchiveStreamFactory()
                        .createArchiveOutputStream(ArchiveStreamFactory.TAR, cout)) {
            try (Stream<Path> stream = Files.walk(directory)) {
                Iterator<Path> iterator = stream.iterator();

                while (iterator.hasNext()) {
                    final Path path = iterator.next();

                    if (path.equals(directory)) {
                        continue;
                    }

                    final String entryName = FilenameUtils
                            .normalize(workingDirectory.toPath().relativize(path).toString(), true);

                    log.debug("tar: {}", entryName);

                    entries.add(entryName);

                    final TarArchiveEntry entry;

                    if (Files.isSymbolicLink(path)) {
                        entry = new TarArchiveEntry(entryName, TarArchiveEntry.LF_SYMLINK);
                        final Path symlinkDestination = Files.readSymbolicLink(path);

                        entry.setLinkName(symlinkDestination.toString());
                    } else if (Files.isDirectory(path) || Files.isRegularFile(path)) {
                        entry = new TarArchiveEntry(path.toFile(), entryName);
                    } else {
                        throw new RuntimeException("Unsupported file type for: " + path);
                    }

                    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                        final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path, new LinkOption[] {});
                        final int mode = PermissionUtils.modeFromPermissions(perms, FileType.of(path));

                        entry.setMode(mode);
                    } else {
                        log.debug(
                                "Cannot set mode since {} filesystem does not support POSIX",
                                Files.getFileStore(path).type());
                    }

                    out.putArchiveEntry(entry);

                    final InputStream content;

                    if (Files.isRegularFile(path)) {
                        content = Files.newInputStream(path);

                        IOUtils.copy(content, out);
                    }

                    out.closeArchiveEntry();
                }
            }
        } catch (IOException | ArchiveException | CompressorException e) {
            throw new RuntimeException("Tar of directory " + directory + " to " + output + " failed", e);
        }

        return entries;
    }

    public static Collection<String> unzip(final File input, final File directory) {
        log.debug("unzip -o {} -d {}", input, directory);

        final List<String> entries = new ArrayList<>();

        try (final InputStream is = Files.newInputStream(input.toPath());
                final ArchiveInputStream in = new ArchiveStreamFactory()
                        .createArchiveInputStream(ArchiveStreamFactory.ZIP, is)) {
            final Path dir = directory.toPath();
            final Path canonicalDir = dir.toAbsolutePath().normalize();

            Files.createDirectories(dir);

            ZipArchiveEntry entry;

            while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
                final String entryName = entry.getName();

                entries.add(entryName);

                final Path path = dir.resolve(entry.getName());

                log.debug("unzip: {}", path);

                final Path canonicalPath = path.toAbsolutePath().normalize();

                if (!canonicalPath.startsWith(canonicalDir)) {
                    throw new RuntimeException("Path " + path + " is outside of destination directory " + directory);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else if (entry.isUnixSymlink()) {
                    final ZipEncoding entryEncoding = entry.getGeneralPurposeBit().usesUTF8ForNames()
                            ? ZipEncodingHelper.getZipEncoding("UTF8")
                            : ZipEncodingHelper.getZipEncoding(Charset.defaultCharset().name());
                    final String targetName = entryEncoding.decode(IOUtils.toByteArray(in));
                    final Path target = FileSystems.getDefault().getPath(targetName);

                    Files.createSymbolicLink(path, target);
                } else {
                    Files.createDirectories(path.getParent());

                    try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                        IOUtils.copy(in, out);
                    }
                }

                final FileTime time = entry.getLastModifiedTime();

                Files.setLastModifiedTime(path, time);

                final int mode = entry.getUnixMode();

                if (mode != 0) {
                    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                        final Set<PosixFilePermission> perms = PermissionUtils.permissionsFromMode(mode);

                        Files.setPosixFilePermissions(path, perms);
                    } else {
                        log.debug(
                                "Cannot set mode: {} since {} filesystem does not support POSIX",
                                String.format("%o", mode),
                                Files.getFileStore(path).type());
                    }
                }
            }
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Unzip of " + input + " to " + directory + " failed", e);
        }

        return entries;
    }

    public static Collection<String> zip(final File output, final File workingDirectory, final File directoryToZip) {
        final Path directory = directoryToZip.toPath();

        log.debug("zip -r {} {}", output, directory);

        List<String> entries = new ArrayList<>();

        try (final OutputStream out = Files.newOutputStream(output.toPath());
                final ArchiveOutputStream os = new ArchiveStreamFactory()
                        .createArchiveOutputStream(ArchiveStreamFactory.ZIP, out)) {
            try (Stream<Path> stream = Files.walk(directory)) {
                Iterator<Path> iterator = stream.iterator();

                while (iterator.hasNext()) { // TODO get rid of iterator
                    final Path path = iterator.next();

                    if (path.equals(directory)) {
                        continue;
                    }

                    final String entryName = FilenameUtils
                            .normalize(workingDirectory.toPath().relativize(path).toString(), true);

                    log.debug("zip: {}", entryName);

                    entries.add(entryName);

                    final ZipArchiveEntry entry = new ZipArchiveEntry(path.toFile(), entryName);

                    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                        final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path, new LinkOption[] {});
                        final int mode = PermissionUtils.modeFromPermissions(perms, FileType.of(path));

                        entry.setUnixMode(mode);
                    } else {
                        log.debug(
                                "Cannot set mode since {} filesystem does not support POSIX",
                                Files.getFileStore(path).type());
                    }

                    os.putArchiveEntry(entry);

                    final InputStream content;

                    if (Files.isSymbolicLink(path)) {
                        final Path symlinkDestination = Files.readSymbolicLink(path);
                        final byte[] bytes = symlinkDestination.toString().getBytes(StandardCharsets.UTF_8);
                        content = new ByteArrayInputStream(bytes);
                        IOUtils.copy(content, os);
                        content.close();
                    } else if (Files.isRegularFile(path)) {
                        content = Files.newInputStream(path);
                        IOUtils.copy(content, os);
                        content.close();
                    } else if (!Files.isDirectory(path)) {
                        throw new RuntimeException("Unsupported file type for: " + path);
                    }
                    os.closeArchiveEntry();
                }
            }
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Zip of directory " + directory + " to " + output + " failed", e);
        }

        return entries;
    }

    public static void copy(File srcFile, File destFile) {
        try {
            if (srcFile.isFile()) {
                if (destFile.isFile()) {
                    // TODO: replace with Files.copy where possible
                    org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);
                } else {
                    org.apache.commons.io.FileUtils.copyFileToDirectory(srcFile, destFile);
                }
            } else {
                if (!destFile.exists()) {
                    org.apache.commons.io.FileUtils.copyDirectory(srcFile, destFile);
                } else {
                    org.apache.commons.io.FileUtils.copyDirectoryToDirectory(srcFile, destFile);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy " + srcFile + " to " + destFile, e);
        }
    }

    public static Collection<String> listZipContents(final File input) {
        log.debug("listing contents of {}", input);
        try (final InputStream is = Files.newInputStream(input.toPath());
                final ArchiveInputStream in = new ArchiveStreamFactory()
                        .createArchiveInputStream(ArchiveStreamFactory.ZIP, is)) {
            List<String> result = new ArrayList<>();
            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                result.add(entry.getName());
            }
            return result;
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Listing contents of " + input + " failed", e);
        }
    }
}
