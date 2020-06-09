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
package org.jboss.pnc.bacon.pig.impl.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 5/25/18
 */
@Data
public class JavadocGenerationData extends GenerationData<JavadocGenerationStrategy> {
    private static final Logger log = LoggerFactory.getLogger(JavadocGenerationData.class);

    private List<String> sourceBuilds = new ArrayList<>();
    private String scmRevision;
    private String generationProject;
    private String buildScript;
    // customPmeParameters Deprecated - alignmentParameters should be used
    private Set<String> customPmeParameters = new TreeSet<>();
    private Set<String> alignmentParameters = new TreeSet<>();
    private String importBom;

    public Set<String> getAlignmentParameters() {
        if (!customPmeParameters.isEmpty() && alignmentParameters.isEmpty()) {
            log.warn("[Deprecated] Please rename 'customPmeParameters' section to 'alignmentParameters'");
            alignmentParameters = customPmeParameters;
        }
        return alignmentParameters;
    }
}
