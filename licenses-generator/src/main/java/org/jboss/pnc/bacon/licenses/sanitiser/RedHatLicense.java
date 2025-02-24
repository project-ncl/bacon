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

package org.jboss.pnc.bacon.licenses.sanitiser;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.jboss.pnc.bacon.licenses.xml.LicenseElement;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class RedHatLicense {

    private String name;

    private String url;

    private String textUrl;

    private Set<String> aliases;

    private Set<String> urlAliases;

    public RedHatLicense(JsonObject jsonObject) {
        this.name = jsonObject.getString("name");
        this.url = jsonObject.getString("url");
        this.textUrl = jsonObject.getString("textUrl", this.url);
        this.aliases = initAliases(jsonObject);
        this.urlAliases = initUrlAliases(jsonObject);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public Set<String> getUrlAliases() {
        return urlAliases;
    }

    public LicenseElement toLicenseElement() {
        return new LicenseElement(name, url, textUrl);
    }

    public String getTextUrl() {
        return textUrl;
    }

    public boolean isAliasTo(LicenseElement licenseElement) {
        return isNameAlias(licenseElement) || isUrlAlias(licenseElement);
    }

    private boolean isNameAlias(LicenseElement licenseElement) {
        String name = sanitiseName(licenseElement.getName());

        return name != null && aliases.contains(name);
    }

    private boolean isUrlAlias(LicenseElement licenseElement) {
        String url = sanitiseUrl(licenseElement.getUrl());

        return url != null && urlAliases.contains(url);
    }

    private String sanitiseName(String name) {
        if (name == null) {
            return null;
        }

        return name.trim().toLowerCase();
    }

    private String sanitiseUrl(String url) {
        if (url == null) {
            return null;
        }

        String resultUrl = url.trim().toLowerCase();

        if (resultUrl.startsWith("http://")) {
            resultUrl = resultUrl.substring(7);
        } else if (resultUrl.startsWith("https://")) {
            resultUrl = resultUrl.substring(8);
        }

        if (resultUrl.startsWith("www.")) {
            resultUrl = resultUrl.substring(4);
        }

        if (resultUrl.endsWith("/")) {
            resultUrl = resultUrl.substring(0, resultUrl.length() - 1);
        }

        return resultUrl;
    }

    private Set<String> initAliases(JsonObject jsonObject) {
        if (!jsonObject.containsKey("aliases")) {
            return new HashSet<>();
        }

        return jsonObject.getJsonArray("aliases")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(this::sanitiseName)
                .collect(Collectors.toSet());
    }

    private Set<String> initUrlAliases(JsonObject jsonObject) {
        if (!jsonObject.containsKey("urlAliases")) {
            return new HashSet<>();
        }

        return jsonObject.getJsonArray("urlAliases")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(this::sanitiseUrl)
                .collect(Collectors.toSet());
    }
}
