/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bacon.pnc.common;

import org.jboss.pnc.bacon.config.Config;

public class UrlGenerator {

    private static String getPNCBaseUrlMinusTrailingSlash() {
        String pncUrl = Config.instance().getActiveProfile().getPnc().getUrl();
        if (pncUrl.endsWith("/")) {
            return pncUrl.substring(0, pncUrl.length() - 1);
        } else {
            return pncUrl;
        }
    }

    public static String generateBuildUrl(String buildId) {
        return getPNCBaseUrlMinusTrailingSlash() + "/pnc-web/#/builds/" + buildId;
    }

    public static String generateGroupBuildUrl(String groupBuildId) {
        return getPNCBaseUrlMinusTrailingSlash() + "/pnc-web/#/group-builds/" + groupBuildId;
    }

    public static String generateGroupConfigUrl(String groupConfigId) {
        return getPNCBaseUrlMinusTrailingSlash() + "/pnc-web/#/group-configs/" + groupConfigId;
    }
}
