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

import org.jboss.pnc.bacon.pig.impl.sources.SourcesGenerationData;
import org.jboss.pnc.bacon.pig.impl.validation.GenerationDataCheck;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Flow {
    private @GenerationDataCheck LicenseGenerationData licensesGeneration;

    private @GenerationDataCheck RepoGenerationData repositoryGeneration;

    private @GenerationDataCheck JavadocGenerationData javadocGeneration;

    /**
     * Add defaults to avoid having existing configurations having to define a sourceGeneration object in the flow
     * section
     */
    private SourcesGenerationData sourcesGeneration = new SourcesGenerationData();
}
