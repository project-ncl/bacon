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
package org.jboss.pnc.bacon.pig.impl.addons.provenance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.jboss.pnc.api.slsa.dto.provenance.v1.Provenance;
import org.jboss.pnc.bacon.common.deliverables.DeliverableRecord;
import org.jboss.pnc.bacon.common.deliverables.DeliverableRegistry;
import org.jboss.pnc.bacon.common.deliverables.DeliverableType;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.bacon.pig.impl.addons.AddOn;
import org.jboss.pnc.bacon.pig.impl.config.PigConfiguration;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuild;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Emits a provenance JSON file for each registered deliverable.
 */
@Slf4j
public final class ProvenanceAddOn extends AddOn {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

    public ProvenanceAddOn(
            PigConfiguration pigConfiguration,
            Map<String, PncBuild> builds,
            String releasePath,
            String extrasPath) {
        super(pigConfiguration, builds, releasePath, extrasPath);
    }

    @Override
    public String getName() {
        return "provenance";
    }

    @Override
    public boolean shouldRun() {
        // Run always.
        return true;
    }

    @Override
    public void trigger() {
        log.debug("Triggering ProvenanceAddOn...");
        PigContext ctx = PigContext.get();
        DeliverableRegistry reg = ctx.getDeliverableRegistry();
        if (reg == null) {
            throw new IllegalStateException("Deliverable registry missing; cannot generate provenance.");
        }

        // Ensure each deliverable has sha256
        reg.computeMissingSha256();

        Path outDir = Paths.get(ctx.getExtrasPath()).resolve("provenance");
        try {
            Files.createDirectories(outDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create provenance output dir: " + outDir, e);
        }

        InvocationInfo inv = ctx.getInvocationInfo();

        log.debug("Number of registered deliverables: " + reg.list().size());

        for (DeliverableRecord d : reg.list()) {
            log.debug("Generating provenance for deliverable {} ...", d.path().getFileName().toString());

            // Avoid recursion: skip provenance outputs if you register them as deliverables
            if (d.path().getFileName().toString().endsWith(".provenance.json")) {
                continue;
            }

            Provenance provenance = SlsaProvenanceV1Utils.toProvenance(ctx, inv, d);

            Path outFile = outDir.resolve(d.path().getFileName().toString() + ".provenance.json");
            log.debug("Created output outFile: " + outFile.toAbsolutePath().toString());

            try {

                // Sanitize the final provenance attestation file which could contain some sensible data
                JsonSecretSanitizer sanitizer = new JsonSecretSanitizer(
                        MAPPER,
                        Set.of(
                                "version",
                                "scmRevision",
                                "baconConfigSha",
                                "bacon-config-hash",
                                "baconConfigHash",
                                "build-config.yaml.preprocessed" // optional extra synonym
                        ),
                        Set.of(),
                        false,
                        true);

                var provenanceTree = MAPPER.valueToTree(provenance);
                var provenanceSanitizedTree = sanitizer.sanitize(provenanceTree);

                Files.writeString(outFile, MAPPER.writeValueAsString(provenanceSanitizedTree));

            } catch (Exception e) {
                throw new RuntimeException("Failed to write provenance for " + d.path(), e);
            }

            // Register the provenance file itself for consistency
            reg.register(
                    org.jboss.pnc.bacon.common.deliverables.DeliverableRecord.create(
                            DeliverableType.OTHER,
                            outFile.toAbsolutePath(),
                            "addon:provenance",
                            Map.of("forDeliverableId", d.id(), "forDeliverablePath", d.path().toString())));
        }
    }

}
