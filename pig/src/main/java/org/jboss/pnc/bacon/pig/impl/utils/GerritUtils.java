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
package org.jboss.pnc.bacon.pig.impl.utils;

import java.net.URI;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 8/31/18
 */
public class GerritUtils {

    private GerritUtils(){
    }

    public static final String GERRIT = "gerrit";

    public static URI gerritSnapshotDownloadUrl(String scmUrl, String scmRevision) {
        String uriString = String.format("https://%s/gitweb?p=%s;a=snapshot;h=%s;sf=tgz",
                gerritUrl(scmUrl), repository(scmUrl), scmRevision);
        return URI.create(uriString);
    }

    public static String repository(String scmUrl) {
        return scmUrl.substring(scmUrl.indexOf(GERRIT) + GERRIT.length() + 1);
    }

    public static String gerritUrl(String scmUrl) {
        String result = scmUrl.replaceAll("^.*://", "");
        result = result.substring(0, result.indexOf(GERRIT) + GERRIT.length());
        return result;
    }
}
