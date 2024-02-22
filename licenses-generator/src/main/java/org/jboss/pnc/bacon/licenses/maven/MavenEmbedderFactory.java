/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
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

package org.jboss.pnc.bacon.licenses.maven;

import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import org.apache.maven.model.building.ModelBuildingRequest;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MavenEmbedderFactory {

    public SnowdropMavenEmbedder getSnowdropMavenEmbedder() {
        try {
            return new SnowdropMavenEmbedder(getMavenRequest());
        } catch (MavenEmbedderException e) {
            throw new RuntimeException("Failed to create Maven embedder", e);
        }
    }

    private MavenRequest getMavenRequest() {
        MavenRequest mavenRequest = new MavenRequest();
        mavenRequest.setSystemProperties(System.getProperties());
        mavenRequest.setProcessPlugins(false);
        mavenRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        return mavenRequest;
    }

}
