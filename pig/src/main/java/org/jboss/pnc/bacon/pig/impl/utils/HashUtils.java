/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.resteasy.util.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.stream.Stream;

public class HashUtils {
    private HashUtils() {
    }

    public static String hashDirectory(Path directory) {
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
