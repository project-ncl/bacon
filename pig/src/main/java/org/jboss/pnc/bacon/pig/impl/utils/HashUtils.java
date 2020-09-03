package org.jboss.pnc.bacon.pig.impl.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.resteasy.util.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.stream.Stream;

@UtilityClass
public class HashUtils {
    public String hashDirectory(Path directory) {
        MessageDigest sha = DigestUtils.getSha512Digest();
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile).sorted().forEach(path -> {
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
