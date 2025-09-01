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
package org.jboss.pnc.bacon.pig.impl.config;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 5/25/18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicenseGenerationData extends GenerationData<LicenseGenerationStrategy> {
    private String licenseExceptionsPath;
    private String licenseNamesPath;

    @java.lang.SuppressWarnings("all")
    public LicenseGenerationData() {
    }

    @java.lang.SuppressWarnings("all")
    public String getLicenseExceptionsPath() {
        return this.licenseExceptionsPath;
    }

    @java.lang.SuppressWarnings("all")
    public String getLicenseNamesPath() {
        return this.licenseNamesPath;
    }

    @java.lang.SuppressWarnings("all")
    public void setLicenseExceptionsPath(final String licenseExceptionsPath) {
        this.licenseExceptionsPath = licenseExceptionsPath;
    }

    @java.lang.SuppressWarnings("all")
    public void setLicenseNamesPath(final String licenseNamesPath) {
        this.licenseNamesPath = licenseNamesPath;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LicenseGenerationData))
            return false;
        final LicenseGenerationData other = (LicenseGenerationData) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$licenseExceptionsPath = this.getLicenseExceptionsPath();
        final java.lang.Object other$licenseExceptionsPath = other.getLicenseExceptionsPath();
        if (this$licenseExceptionsPath == null ? other$licenseExceptionsPath != null
                : !this$licenseExceptionsPath.equals(other$licenseExceptionsPath))
            return false;
        final java.lang.Object this$licenseNamesPath = this.getLicenseNamesPath();
        final java.lang.Object other$licenseNamesPath = other.getLicenseNamesPath();
        if (this$licenseNamesPath == null ? other$licenseNamesPath != null
                : !this$licenseNamesPath.equals(other$licenseNamesPath))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LicenseGenerationData;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $licenseExceptionsPath = this.getLicenseExceptionsPath();
        result = result * PRIME + ($licenseExceptionsPath == null ? 43 : $licenseExceptionsPath.hashCode());
        final java.lang.Object $licenseNamesPath = this.getLicenseNamesPath();
        result = result * PRIME + ($licenseNamesPath == null ? 43 : $licenseNamesPath.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "LicenseGenerationData(licenseExceptionsPath=" + this.getLicenseExceptionsPath() + ", licenseNamesPath="
                + this.getLicenseNamesPath() + ")";
    }
}
