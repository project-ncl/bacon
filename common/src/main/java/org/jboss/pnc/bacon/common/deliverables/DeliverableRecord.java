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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DeliverableRecord {

    private final String id;
    private final DeliverableType type;
    private final Path path;
    private final String createdBy; // phase/addon name
    private final Instant createdAt;

    // optional metadata
    private volatile Map<String, String> digests; // sha256, sha512, ...
    private final Map<String, Object> attributes; // strategy, groupId, etc.

    private DeliverableRecord(
            String id,
            DeliverableType type,
            Path path,
            String createdBy,
            Instant createdAt,
            Map<String, String> digests,
            Map<String, Object> attributes) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.path = Objects.requireNonNull(path);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.digests = (digests == null) ? Map.of() : Map.copyOf(digests);
        this.attributes = (attributes == null) ? Map.of() : Map.copyOf(attributes);
    }

    public static DeliverableRecord create(
            DeliverableType type,
            Path path,
            String createdBy,
            Map<String, Object> attributes) {
        return new DeliverableRecord(
                UUID.randomUUID().toString(),
                type,
                path,
                createdBy,
                Instant.now(),
                Map.of(),
                attributes);
    }

    public String id() {
        return id;
    }

    public DeliverableType type() {
        return type;
    }

    public Path path() {
        return path;
    }

    public String createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Map<String, String> digests() {
        return digests;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public void setDigests(Map<String, String> digests) {
        this.digests = Map.copyOf(digests);
    }
}
