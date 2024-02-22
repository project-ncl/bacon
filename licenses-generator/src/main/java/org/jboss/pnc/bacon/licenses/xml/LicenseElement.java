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

package org.jboss.pnc.bacon.licenses.xml;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.License;

import javax.json.JsonObject;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class LicenseElement {

    private String name;

    private String url;

    private String textUrl;

    public LicenseElement() {
    }

    public LicenseElement(License license) {
        this(license.getName(), license.getUrl(), license.getUrl());
    }

    public LicenseElement(LicenseElement licenseElement) {
        this.name = licenseElement.getName();
        this.url = licenseElement.getUrl();
        this.textUrl = licenseElement.getTextUrl();
    }

    public LicenseElement(JsonObject licenseElementJson) {
        this(licenseElementJson.getString("name"), licenseElementJson.getString("url"));
    }

    public LicenseElement(String name, String url) {
        this(name, url, url);
    }

    public LicenseElement(String name, String url, String textUrl) {
        this.name = name;
        this.url = url;
        this.textUrl = textUrl;
    }

    public String getName() {
        return name;
    }

    @XmlElement
    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    @XmlElement
    public void setUrl(String url) {
        this.url = url;
    }

    @XmlTransient
    public void setTextUrl(String textUrl) {
        this.textUrl = textUrl;
    }

    public String getTextUrl() {
        return textUrl;
    }

    @Override
    public String toString() {
        return String.format(
                "%s{name='%s', url='%s', textUrl=%s}",
                LicenseElement.class.getSimpleName(),
                name,
                url,
                textUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LicenseElement that = (LicenseElement) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (url != null ? !url.equals(that.url) : that.url != null)
            return false;
        if (textUrl != null ? !textUrl.equals(that.textUrl) : that.textUrl != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (textUrl != null ? textUrl.hashCode() : 0);
        return result;
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(name);
    }
}
