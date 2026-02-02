/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.common.deliverables;

import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DeliverableRegistry {

    private final List<DeliverableRecord> records = new CopyOnWriteArrayList<>();

    public DeliverableRecord register(DeliverableRecord record) {
        records.add(record);
        return record;
    }

    public List<DeliverableRecord> list() {
        return List.copyOf(records);
    }

    public void computeMissingSha256() {
        for (DeliverableRecord r : records) {
            if (!r.digests().containsKey("sha256")) {
                try {
                    var sha256 = sha256Hex(r.path());
                    var newDigests = new HashMap<>(r.digests());
                    newDigests.put("sha256", sha256);
                    r.setDigests(newDigests);
                } catch (Exception e) {
                    // keep record, but annotate error
                    var newDigests = new HashMap<>(r.digests());
                    newDigests.put("sha256_error", e.getClass().getSimpleName() + ": " + e.getMessage());
                    r.setDigests(newDigests);
                }
            }
        }
    }

    private static String sha256Hex(java.nio.file.Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > 0)
                md.update(buf, 0, read);
        }
        return toHex(md.digest());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
