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
package org.jboss.pnc.bacon.config;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 12/17/18
 */
public class PncConfig implements Validate {
    private String url;
    private String bifrostBaseurl;

    @Override
    public void validate() {
        Validate.validateUrl(url, "PNC");
        Validate.validateUrl(bifrostBaseurl, "Bifrost");
    }

    @java.lang.SuppressWarnings("all")
    public PncConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getUrl() {
        return this.url;
    }

    @java.lang.SuppressWarnings("all")
    public String getBifrostBaseurl() {
        return this.bifrostBaseurl;
    }

    @java.lang.SuppressWarnings("all")
    public void setUrl(final String url) {
        this.url = url;
    }

    @java.lang.SuppressWarnings("all")
    public void setBifrostBaseurl(final String bifrostBaseurl) {
        this.bifrostBaseurl = bifrostBaseurl;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PncConfig))
            return false;
        final PncConfig other = (PncConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$url = this.getUrl();
        final java.lang.Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url))
            return false;
        final java.lang.Object this$bifrostBaseurl = this.getBifrostBaseurl();
        final java.lang.Object other$bifrostBaseurl = other.getBifrostBaseurl();
        if (this$bifrostBaseurl == null ? other$bifrostBaseurl != null
                : !this$bifrostBaseurl.equals(other$bifrostBaseurl))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PncConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final java.lang.Object $bifrostBaseurl = this.getBifrostBaseurl();
        result = result * PRIME + ($bifrostBaseurl == null ? 43 : $bifrostBaseurl.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PncConfig(url=" + this.getUrl() + ", bifrostBaseurl=" + this.getBifrostBaseurl() + ")";
    }
}
