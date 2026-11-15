/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc;

import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

/**
 * Where build sources and alignment diffs are downloaded from, set with {@code scmSource} in the active profile.
 */
@Slf4j
public enum ScmSource {

    /** The git provider first, then PNC (orch) when it fails. */
    AUTO,
    /** The git provider (GitHub, GitHub Enterprise or GitLab) only, with the profile's tokens. */
    DIRECT,
    /** PNC (orch) only, which reads the internal repository through reqour: no git provider access needed. */
    ORCH;

    static ScmSource fromProfile() {
        String value = null;
        try {
            value = org.jboss.pnc.bacon.config.Config.instance().getActiveProfile().getScmSource();
        } catch (RuntimeException e) {
            log.debug("Cannot read scmSource from the active Bacon profile", e);
        }
        return parse(value);
    }

    static ScmSource parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid scmSource '" + value + "' in the Bacon profile; use auto, direct or orch.");
        }
    }
}
