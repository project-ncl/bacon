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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.jboss.pnc.api.slsa.dto.provenance.v1.BuildDefinition;
import org.jboss.pnc.api.slsa.dto.provenance.v1.Builder;
import org.jboss.pnc.api.slsa.dto.provenance.v1.Metadata;
import org.jboss.pnc.api.slsa.dto.provenance.v1.Predicate;
import org.jboss.pnc.api.slsa.dto.provenance.v1.Provenance;
import org.jboss.pnc.api.slsa.dto.provenance.v1.ResourceDescriptor;
import org.jboss.pnc.api.slsa.dto.provenance.v1.RunDetails;
import org.jboss.pnc.bacon.common.deliverables.DeliverableRecord;
import org.jboss.pnc.bacon.pig.Pig;
import org.jboss.pnc.bacon.pig.impl.PigContext;
import org.jboss.pnc.mavenmanipulator.common.util.ManifestUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps Bacon deliverable records + invocation context into pnc-api SLSA provenance v1 DTOs.
 */
@Slf4j
public final class SlsaProvenanceV1Utils {

    private static final String BUILD_TYPE = "https://project-ncl.github.io/slsa-pnc-cli-buildtypes/workflow/v1";
    private static final String SLSLA_BUILD_PROVENANCE_ATTESTATION_TYPE = "https://in-toto.io/Statement/v1";
    private static final String SLSLA_BUILD_PROVENANCE_PREDICATE_TYPE = "https://slsa.dev/provenance/v1";
    public static final String BUILDER_ID_PREFIX = "urn:uuid:";

    private SlsaProvenanceV1Utils() {
    }

    /**
     * Creates a Provenance attestation with predicateType "https://slsa.dev/provenance/v1" and type
     * "https://in-toto.io/Statement/v1"
     */
    public static Provenance toProvenance(PigContext ctx, InvocationInfo inv, DeliverableRecord d) {
        Predicate predicate = Predicate.builder()
                .buildDefinition(createBuildDefinition(ctx, inv, d))
                .runDetails(createRunDetails(inv))
                .build();
        Provenance prov = Provenance.builder()
                .type(SLSLA_BUILD_PROVENANCE_ATTESTATION_TYPE)
                .predicateType(SLSLA_BUILD_PROVENANCE_PREDICATE_TYPE)
                .subject(createSubjects(List.of(d)))
                .predicate(predicate)
                .build();
        return prov;
    }

    /**
     * Convert a collection of deliverable records into subjects.
     */
    private static List<ResourceDescriptor> createSubjects(Collection<DeliverableRecord> deliverableRecords) {
        return deliverableRecords.stream().map((DeliverableRecord d) -> {
            return ResourceDescriptor.builder()
                    .name(d.path().getFileName().toString())
                    .digest(d.digests() == null ? Map.of() : d.digests())
                    .annotations(Map.of("path", d.path()))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Creates a buildDefinition with all external and internal parameters, and the resolved dependencies
     */
    private static BuildDefinition createBuildDefinition(PigContext ctx, InvocationInfo inv, DeliverableRecord d) {

        // External parameters: what can be set by a caller
        Map<String, Object> externalParameters = new LinkedHashMap<>();
        externalParameters.put("commandLine", inv.commandLine());
        externalParameters.put("prefix", ctx.getPrefix());
        externalParameters.put("fullVersion", ctx.getFullVersion());
        externalParameters.put("tempBuild", String.valueOf(ctx.isTempBuild()));
        externalParameters.put("targetPath", ctx.getTargetPath());
        externalParameters.put("releaseDirName", ctx.getReleaseDirName());
        externalParameters.put("pigConfiguration", ctx.getPigConfiguration());

        // Internal parameters: derived/runtime details
        Map<String, Object> internalParameters = new LinkedHashMap<>();
        internalParameters.put("deliverableType", d.type().name());
        internalParameters.put("createdBy", d.createdBy());
        internalParameters.put("createdAt", d.createdAt().toString());

        internalParameters.put("releasePath", relativizeFromTarget(ctx.getTargetPath(), ctx.getReleasePath()));
        internalParameters.put("extrasPath", relativizeFromTarget(ctx.getTargetPath(), ctx.getExtrasPath()));

        System.out.println(
                "relativizeFromTarget.getTargetPath()"
                        + relativizeFromTarget(ctx.getTargetPath(), ctx.getReleasePath()));
        System.out.println(
                "relativizeFromTarget.getReleasePath()"
                        + relativizeFromTarget(ctx.getTargetPath(), ctx.getExtrasPath()));

        // ResolvedDependencies:
        // - bacon github URI
        // - preprocessed build-config.yaml digest (after substitution of variables)
        // - Pig config dir hash (context hash)
        // - any DeliverableInput materials registered
        List<ResourceDescriptor> deps = new ArrayList<>();
        deps.add(
                ResourceDescriptor.builder()
                        .name("bacon")
                        .uri("https://github.com/project-ncl/bacon")
                        .build());

        // 1) Preprocessed YAML digest
        if (inv != null && inv.configDigests() != null) {
            String preSha = inv.configDigests().get("build-config.preprocessed.sha256");
            if (preSha != null) {
                deps.add(
                        ResourceDescriptor.builder()
                                .name("build-config.yaml (preprocessed)")
                                .digest(Map.of("sha256", preSha))
                                .build());
            }
        }

        // 2) Pig config directory hash as a dependency/material
        if (ctx.getConfigSha() != null) {
            deps.add(
                    ResourceDescriptor.builder()
                            .name("pig-config-dir")
                            .digest(Map.of("sha512", ctx.getConfigSha()))
                            .build());
        }

        return BuildDefinition.builder()
                .buildType(BUILD_TYPE)
                .externalParameters(externalParameters)
                .internalParameters(internalParameters)
                .resolvedDependencies(deps)
                .build();
    }

    private static RunDetails createRunDetails(InvocationInfo inv) {

        EnvironmentInfo env = EnvironmentInfo.collect();

        Map<String, String> v = new LinkedHashMap<>();
        v.put("bacon", ManifestUtils.getManifestInformation(Pig.class));
        v.put("hostName", env.hostname());
        v.put("os.name", env.os().name());
        v.put("os.version", env.os().version());
        v.put("os.arch", env.os().arch());
        v.put("java.runtimeVersion", env.java().runtimeVersion());
        v.put("java.vendor", env.java().vendor());
        env.ci().forEach((k, val) -> v.put("ci." + k, val));

        String invocationId = inv != null ? inv.invocationId() : UUID.randomUUID().toString();
        Instant started = inv != null && inv.startedOn() != null ? inv.startedOn() : Instant.now();
        Instant finished = inv != null && inv.finishedOn() != null ? inv.finishedOn() : Instant.now();

        Builder builder = Builder.newBuilder()
                .id(BUILDER_ID_PREFIX + invocationId)
                .version(Collections.unmodifiableMap(v))
                .build();

        Metadata metadata = Metadata.builder()
                .invocationId(invocationId)
                .startedOn(started)
                .finishedOn(finished)
                .build();

        return RunDetails.builder().builder(builder).metadata(metadata).build();
    }

    private static String relativizeFromTarget(String targetPath, String absolutePath) {
        try {
            Path absTarget = Paths.get(targetPath).toAbsolutePath().normalize();
            Path abs = Paths.get(absolutePath).toAbsolutePath().normalize();

            Path base = absTarget.getParent() == null ? absTarget : absTarget.getParent();
            if (abs.startsWith(base)) {
                String rel = base.relativize(abs).toString();
                if (!rel.endsWith(File.separator)) {
                    rel = rel + File.separator;
                }
                return rel;
            }

            // fallback: filename only (avoid leaking absolute paths)
            return abs.getFileName().toString();

        } catch (Exception e) {
            return Paths.get(absolutePath).getFileName().toString();
        }
    }

}
