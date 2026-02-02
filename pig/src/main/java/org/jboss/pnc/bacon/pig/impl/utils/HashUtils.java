package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.resteasy.util.Hex;

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

    /**
     * Computes the SHA-256 cryptographic hash of the given byte array and
     * returns the result encoded as a lowercase hexadecimal string.
     * <p>
     * The returned string is always 64 hexadecimal characters long
     * (32 bytes × 2 hex characters per byte), as defined by the SHA-256
     * specification.
     * <p>
     * This method is deterministic: the same input bytes will always
     * produce the same output hash.
     *
     * @param bytes the input data to hash; must not be {@code null}
     * @return the SHA-256 digest of the input data, encoded as a lowercase
     *         hexadecimal string
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm is not
     *         available in the current Java runtime
     */
    public static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(bytes);
        return Hex.encodeHex(digest);
    }

}
