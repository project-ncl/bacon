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
package org.jboss.pnc.bacon.common;

import lombok.experimental.UtilityClass;

/**
 * Utils class shared among all apps
 */
@UtilityClass
public class Utils {
    /**
     * Handle picky case where sourceUrl may end with "/" in the config file, and combine it gracefully with the path
     *
     * @param sourceUrl source url
     * @param path remaining part of url
     * @return beautifully assembled url
     */
    public String generateUrlPath(String sourceUrl, String path) {
        if (sourceUrl.endsWith("/")) {
            return sourceUrl.substring(0, sourceUrl.length() - 2) + path;
        } else {
            return sourceUrl + path;
        }
    }
}
