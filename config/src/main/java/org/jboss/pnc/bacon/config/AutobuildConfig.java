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

public class AutobuildConfig implements Validate {
    private String gavExclusionUrl;

    @Override
    public void validate() {
        Validate.validateUrl(gavExclusionUrl, "GAV Exclusion file");
    }

    @java.lang.SuppressWarnings("all")
    public AutobuildConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getGavExclusionUrl() {
        return this.gavExclusionUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setGavExclusionUrl(final String gavExclusionUrl) {
        this.gavExclusionUrl = gavExclusionUrl;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AutobuildConfig))
            return false;
        final AutobuildConfig other = (AutobuildConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$gavExclusionUrl = this.getGavExclusionUrl();
        final java.lang.Object other$gavExclusionUrl = other.getGavExclusionUrl();
        if (this$gavExclusionUrl == null ? other$gavExclusionUrl != null
                : !this$gavExclusionUrl.equals(other$gavExclusionUrl))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AutobuildConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $gavExclusionUrl = this.getGavExclusionUrl();
        result = result * PRIME + ($gavExclusionUrl == null ? 43 : $gavExclusionUrl.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AutobuildConfig(gavExclusionUrl=" + this.getGavExclusionUrl() + ")";
    }
}
