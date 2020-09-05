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

import static java.nio.file.Files.createTempDirectory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarConstants;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 7/28/17
 */
public final class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {

    }

    public static File mkTempDir(final String prefix) {
        try {
            return createTempDirectory(prefix).toFile();
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

    public static void setModeAndLastModifiedTime(Path path, ArchiveEntry entry) throws IOException {
        final FileSystem fileSystem = path.getFileSystem();
        final Set<String> attributeViews = fileSystem.supportedFileAttributeViews();

        log.debug("Supported file attribute views: {}", attributeViews);

        if (attributeViews.contains("posix")) {
            final Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            final int mode = PermissionUtils.modeFromPermissions(perms, PermissionUtils.FileType.of(path));

            if (log.isDebugEnabled()) {
                log.debug("chmod {} {}", String.format("%04o", mode), path);
            }

            if (entry instanceof TarArchiveEntry) {
                ((TarArchiveEntry) entry).setMode(mode);
            } else if (entry instanceof ZipArchiveEntry) {
                ((ZipArchiveEntry) entry).setUnixMode(mode);
            } else {
                throw new RuntimeException("Invalid entry type " + entry.getClass().getSimpleName());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Cannot set mode for {} since {} filesystem does not support POSIX",
                        path,
                        Files.getFileStore(path).type());
            }
        }

        final FileTime time = Files.getLastModifiedTime(path);

        if (log.isDebugEnabled()) {
            log.debug(
                    "touch -t {} {}",
                    new SimpleDateFormat("yyyyMMddHHmm.ss", Locale.US).format(new Date(time.toMillis())),
                    path);
        }

        if (entry instanceof TarArchiveEntry) {
            ((TarArchiveEntry) entry).setModTime(time.toMillis());
        } else if (entry instanceof ZipArchiveEntry) {
            ((ZipArchiveEntry) entry).setLastModifiedTime(time);
        } else {
            throw new RuntimeException("Invalid entry type " + entry.getClass().getSimpleName());
        }
    }

    // FIXME: Setting the mode on a directory before extracting its files can cause problems
    // FIXME: We should not set the mode until all files in that directory are extracted
    // FIXME: See <https://www.gnu.org/software/tar/manual/tar.html#SEC85>.
    public static void setModeAndLastModifiedTime(final Path path, final int mode, final FileTime time)
            throws IOException {
        final FileSystem fileSystem = path.getFileSystem();
        final Set<String> attributeViews = fileSystem.supportedFileAttributeViews();

        log.debug("Supported file attribute views: {}", attributeViews);

        if (mode != 0) {
            if (attributeViews.contains("posix")) {
                if (log.isDebugEnabled()) {
                    log.debug("chmod {} {}", String.format("%04o", mode), path);
                }

                final Set<PosixFilePermission> perms = PermissionUtils.permissionsFromMode(mode);
                Files.setPosixFilePermissions(path, perms);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Cannot set mode {} for {} since {} filesystem does not support POSIX",
                            String.format("%04o", mode),
                            path,
                            Files.getFileStore(path).type());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "touch -t {} {}",
                    new SimpleDateFormat("yyyyMMddHHmm.ss", Locale.US).format(new Date(time.toMillis())),
                    path);
        }

        if (time != null) {
            Files.setLastModifiedTime(path, time);
        }
    }

    public static Collection<String> untar(final File input, final File directory) {
        log.debug("tar -xf {} -C {}", input, directory);

        final String compressorType = getCompressorType(input);

        log.debug("untar: detected compressor type: {}", compressorType);

        final Collection<String> entries = new ArrayList<>();

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

                    try (final OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                        Files.createDirectories(path.getParent());
                        IOUtils.copy(in, out);
                    }
                } else {
                    throw new RuntimeException("Unsupported file type for: " + entryName);
                }

                final int mode = entry.getMode();
                final Date modTime = entry.getModTime();
                final long value = modTime.getTime();
                final FileTime time = FileTime.fromMillis(value);

                setModeAndLastModifiedTime(path, mode, time);
            }
        } catch (IOException | ArchiveException | CompressorException e) {
            throw new RuntimeException("Untar of " + input + " to " + directory + " failed", e);
        }

        return Collections.unmodifiableCollection(entries);
    }

    public static Collection<String> tar(final File output, final File workingDirectory, final File directoryToTar) {
        final Path directory = directoryToTar.toPath();

        log.debug("tar -cf {} {}", output, directory);

        final String compressorType = getCompressorType(output.getName());

        log.debug("tar: detected compressor type: {}", compressorType);

        final Collection<String> entries = new ArrayList<>();

        try (final OutputStream os = Files.newOutputStream(output.toPath());
                final OutputStream cout = compressorType != null
                        ? new CompressorStreamFactory().createCompressorOutputStream(compressorType, os)
                        : os;
                final ArchiveOutputStream out = new ArchiveStreamFactory()
                        .createArchiveOutputStream(ArchiveStreamFactory.TAR, cout)) {
            try (final Stream<Path> stream = Files.walk(directory)) {
                final Iterator<Path> iterator = stream.iterator();

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
                        entry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
                        final Path symlinkDestination = Files.readSymbolicLink(path);

                        entry.setLinkName(symlinkDestination.toString());
                    } else if (Files.isDirectory(path) || Files.isRegularFile(path)) {
                        entry = new TarArchiveEntry(path.toFile(), entryName);
                    } else {
                        throw new RuntimeException("Unsupported file type for: " + path);
                    }

                    setModeAndLastModifiedTime(path, entry);

                    out.putArchiveEntry(entry);

                    if (Files.isRegularFile(path)) {
                        try (final InputStream content = Files.newInputStream(path)) {
                            IOUtils.copy(content, out);
                        }
                    }

                    out.closeArchiveEntry();
                }
            }
        } catch (IOException | ArchiveException | CompressorException e) {
            throw new RuntimeException("Tar of directory " + directory + " to " + output + " failed", e);
        }

        return Collections.unmodifiableCollection(entries);
    }

    public static Collection<String> listZipContents(final File input) {
        log.debug("Listing contents of {}", input);

        try (final InputStream is = Files.newInputStream(input.toPath());
                final ArchiveInputStream in = new ArchiveStreamFactory()
                        .createArchiveInputStream(ArchiveStreamFactory.ZIP, is)) {
            final Collection<String> result = new ArrayList<>();
            ArchiveEntry entry;

            while ((entry = in.getNextEntry()) != null) {
                result.add(entry.getName());
            }

            return Collections.unmodifiableCollection(result);
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Listing contents of " + input + " failed", e);
        }
    }

    public static Collection<String> unzip(final File input, final File directory) {
        return unzip(input, directory, null);
    }

    public static Collection<String> unzip(final File input, final File directory, final String extraction) {
        log.debug("unzip -o {} -d {}", input, directory);

        Pattern extractionPattern = null;

        if (extraction != null && !extraction.isEmpty()) {
            extractionPattern = Pattern.compile(extraction);
        }

        final Collection<String> entries = new ArrayList<>();

        try (final InputStream is = Files.newInputStream(input.toPath());
                final ArchiveInputStream in = new ArchiveStreamFactory()
                        .createArchiveInputStream(ArchiveStreamFactory.ZIP, is)) {
            final Path dir = directory.toPath();
            final Path canonicalDir = dir.toAbsolutePath().normalize();

            Files.createDirectories(dir);

            ZipArchiveEntry entry;

            while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
                final String entryName = entry.getName();

                // If extraction is specified, only unzip the specified file or directory.
                // Directories must end in '/' otherwise just the empty directory will be created.
                if (extractionPattern != null) {
                    final Matcher matcher = extractionPattern.matcher(entryName);

                    if (!matcher.find()) {
                        // If the entry doesn't match the extraction, try the next.
                        continue;
                    }
                }

                final Path path = dir.resolve(entryName);

                entries.add(entryName);

                log.debug("unzip: {}", path);

                final Path canonicalPath = path.toAbsolutePath().normalize();

                if (!canonicalPath.startsWith(canonicalDir)) {
                    throw new RuntimeException("Path " + path + " is outside of destination directory " + directory);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else if (entry.isUnixSymlink()) {
                    final ZipEncoding entryEncoding = entry.getGeneralPurposeBit().usesUTF8ForNames()
                            ? ZipEncodingHelper.getZipEncoding(StandardCharsets.UTF_8.name())
                            : ZipEncodingHelper.getZipEncoding(Charset.defaultCharset().name());
                    final String targetName = entryEncoding.decode(IOUtils.toByteArray(in));
                    final Path target = path.getFileSystem().getPath(targetName);

                    Files.createSymbolicLink(path, target);
                } else {
                    Files.createDirectories(path.getParent());

                    try (final OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                        IOUtils.copy(in, out);
                    }
                }

                final int mode = entry.getUnixMode();
                final FileTime time = entry.getLastModifiedTime();

                setModeAndLastModifiedTime(path, mode, time);
            }
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Unzip of " + input + " to " + directory + " failed", e);
        }

        return Collections.unmodifiableCollection(entries);
    }

    public static Collection<String> zip(final File output, final File workingDirectory, final File directoryToZip) {
        final Path directory = directoryToZip.toPath();

        log.debug("zip -r {} {}", output, directory);

        final Collection<String> entries = new ArrayList<>();

        try (final OutputStream out = Files.newOutputStream(output.toPath());
                final ArchiveOutputStream os = new ArchiveStreamFactory()
                        .createArchiveOutputStream(ArchiveStreamFactory.ZIP, out)) {
            try (final Stream<Path> stream = Files.walk(directory)) {
                final Iterator<Path> iterator = stream.iterator();

                while (iterator.hasNext()) { // TODO get rid of iterator
                    final Path path = iterator.next();

                    if (path.equals(directory)) {
                        continue;
                    }

                    String entryName = FilenameUtils
                            .normalize(workingDirectory.toPath().relativize(path).toString(), true);
                    if (Files.isDirectory(path)) {
                        entryName += "/"; // required for directories
                    }

                    log.debug("zip: {}", entryName);

                    entries.add(entryName);

                    final ArchiveEntry entry = new ZipArchiveEntry(path.toFile(), entryName);

                    setModeAndLastModifiedTime(path, entry);

                    os.putArchiveEntry(entry);

                    if (Files.isSymbolicLink(path)) {
                        final Path symlinkDestination = Files.readSymbolicLink(path);
                        final byte[] bytes = symlinkDestination.toString().getBytes(StandardCharsets.UTF_8);
                        try (final InputStream content = new ByteArrayInputStream(bytes)) {
                            IOUtils.copy(content, os);
                            os.closeArchiveEntry();
                        }
                    } else if (Files.isRegularFile(path)) {
                        try (final InputStream content = Files.newInputStream(path)) {
                            IOUtils.copy(content, os);
                            os.closeArchiveEntry();
                        }
                    } else if (!Files.isDirectory(path)) {
                        throw new RuntimeException("Unsupported file type for: " + path);
                    }
                }
            }
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Zip of directory " + directory + " to " + output + " failed", e);
        }

        return Collections.unmodifiableCollection(entries);
    }

    public static void copy(final File srcFile, final File destFile) {
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

    /**
     * Moves the contents of srcDir into destDir. Equivalent of calling "mv srcDir/* destDir" in a shell.
     *
     * @param srcDir the path to the directory to move
     * @param destDir the path to the target directory
     */
    public static boolean moveDirectoryContents(final File srcDir, final File destDir) throws IOException {
        if (!srcDir.isDirectory() || !destDir.isDirectory()) {
            return false;
        }

        try (final Stream<Path> stream = Files.list(srcDir.toPath())) {
            stream.forEach(path -> {
                try {
                    Files.move(path, new File(destDir, path.toFile().getName()).toPath());
                } catch (IOException ex) {
                    throw new RuntimeException("Unable to move file " + path, ex);
                }
            });

            return true;
        }
    }
}
