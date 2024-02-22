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
package org.jboss.pnc.bacon.licenses.sanitiser.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.pnc.bacon.licenses.xml.LicenseElement;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalLicenseDto {
    private String name;
    private String url;
    private String textUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTextUrl() {
        return textUrl;
    }

    @JsonProperty("license_text_url")
    public void setTextUrl(String textUrl) {
        this.textUrl = textUrl;
    }

    public LicenseElement toLicenseElement() {
        return new LicenseElement(name, url, StringUtils.isBlank(textUrl) ? url : textUrl);
    }
}
