package org.jboss.pnc.bacon.pig.impl.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.resteasy.util.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class HashUtils {
    private HashUtils() {
    }

    public static String hashDirectory(Path directory) {
        // don't ignore any file
        return hashDirectory(directory, path -> false);
    }

    /**
     * Hash the content of a directory and specify files to ignore in the hash
     *
     * @param directory path of the directory
     * @param ignorePredicate predicate that get the list of files, and decides whether to ignore them or not. returns
     *        true to ignore
     * @return the hash of the directory content
     */
    public static String hashDirectory(Path directory, Predicate<Path> ignorePredicate) {
        MessageDigest sha = DigestUtils.getSha512Digest();
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile).filter(ignorePredicate.negate()).sorted().forEach(path -> {
                try {
                    DigestUtils.updateDigest(sha, directory.relativize(path).toString());
                    DigestUtils.updateDigest(sha, path.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to calculate sha of " + path.toAbsolutePath(), e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk through " + directory, e);
        }
        return Hex.encodeHex(sha.digest());
    }

}
