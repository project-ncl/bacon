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
package org.jboss.pnc.bacon.pig.impl.documents;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 6/5/17
 */
public enum Template {
    README("README.ftl", "README.html", false),
    REPOSITORY_COORDINATES("REPOSITORY_COORDINATES.ftl", "REPOSITORY_COORDINATES.properties", true),
    ARTIFACT_LIST("ARTIFACT_LIST.ftl", "repository-artifact-list.txt", true),
    BUILD_LIST("BUILD_LIST.ftl", "build-list.json", true),
    DUPLICATE_ARTIFACT_LIST("DUPLICATE_ARTIFACT_LIST.ftl", "repository-duplicate-artifact-list.txt", true);

    private final String filename;
    private final String templateName;
    private final boolean extrasPath;

    Template(String template, String filename, boolean extrasPath) {
        templateName = template;
        this.filename = filename;
        this.extrasPath = extrasPath;
    }

    @java.lang.SuppressWarnings("all")
    public String getFilename() {
        return this.filename;
    }

    @java.lang.SuppressWarnings("all")
    public String getTemplateName() {
        return this.templateName;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isExtrasPath() {
        return this.extrasPath;
    }
}
