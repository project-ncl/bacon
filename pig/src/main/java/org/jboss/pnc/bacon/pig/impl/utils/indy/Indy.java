/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.utils.indy;

import org.jboss.pnc.bacon.config.Config;
import org.jboss.pnc.bacon.config.PigConfig;

public class Indy {
    private static volatile String indyRepoUrl;
    private static volatile String indyTempRepoUrl;

    private Indy() {
    }

    public static String getIndyUrl() {
        if (indyRepoUrl == null) {
            indyRepoUrl = pigUrl() + "api/content/maven/group/static";
        }

        return indyRepoUrl;
    }

    public static String getIndyTempUrl() {
        if (indyTempRepoUrl == null) {
            indyTempRepoUrl = pigUrl() + "api/content/maven/group/temporary-builds";
        }

        return indyTempRepoUrl;
    }

    private static String pigUrl() {
        PigConfig pig = Config.instance().getActiveProfile().getPig();
        String indyUrl = pig.getIndyUrl();
        if (!indyUrl.endsWith("/")) {
            indyUrl = indyUrl + "/";
        }
        return indyUrl;
    }
}
