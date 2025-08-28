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

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com <br>
 *         Date: 11/28/17
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductConfig {
    @NotBlank
    private String name;
    @NotBlank
    private String abbreviation;
    private String productPagesCode;
    private String productManagers;
    private String stage;
    private String issueTrackerUrl;

    public String prefix() {
        // noinspection StringToUpperCaseOrToLowerCaseWithoutLocale
        return getName().toLowerCase().replaceAll(" ", "-");
    }

    @java.lang.SuppressWarnings("all")
    public ProductConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getAbbreviation() {
        return this.abbreviation;
    }

    @java.lang.SuppressWarnings("all")
    public String getProductPagesCode() {
        return this.productPagesCode;
    }

    @java.lang.SuppressWarnings("all")
    public String getProductManagers() {
        return this.productManagers;
    }

    @java.lang.SuppressWarnings("all")
    public String getStage() {
        return this.stage;
    }

    @java.lang.SuppressWarnings("all")
    public String getIssueTrackerUrl() {
        return this.issueTrackerUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setAbbreviation(final String abbreviation) {
        this.abbreviation = abbreviation;
    }

    @java.lang.SuppressWarnings("all")
    public void setProductPagesCode(final String productPagesCode) {
        this.productPagesCode = productPagesCode;
    }

    @java.lang.SuppressWarnings("all")
    public void setProductManagers(final String productManagers) {
        this.productManagers = productManagers;
    }

    @java.lang.SuppressWarnings("all")
    public void setStage(final String stage) {
        this.stage = stage;
    }

    @java.lang.SuppressWarnings("all")
    public void setIssueTrackerUrl(final String issueTrackerUrl) {
        this.issueTrackerUrl = issueTrackerUrl;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProductConfig))
            return false;
        final ProductConfig other = (ProductConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name))
            return false;
        final java.lang.Object this$abbreviation = this.getAbbreviation();
        final java.lang.Object other$abbreviation = other.getAbbreviation();
        if (this$abbreviation == null ? other$abbreviation != null : !this$abbreviation.equals(other$abbreviation))
            return false;
        final java.lang.Object this$productPagesCode = this.getProductPagesCode();
        final java.lang.Object other$productPagesCode = other.getProductPagesCode();
        if (this$productPagesCode == null ? other$productPagesCode != null
                : !this$productPagesCode.equals(other$productPagesCode))
            return false;
        final java.lang.Object this$productManagers = this.getProductManagers();
        final java.lang.Object other$productManagers = other.getProductManagers();
        if (this$productManagers == null ? other$productManagers != null
                : !this$productManagers.equals(other$productManagers))
            return false;
        final java.lang.Object this$stage = this.getStage();
        final java.lang.Object other$stage = other.getStage();
        if (this$stage == null ? other$stage != null : !this$stage.equals(other$stage))
            return false;
        final java.lang.Object this$issueTrackerUrl = this.getIssueTrackerUrl();
        final java.lang.Object other$issueTrackerUrl = other.getIssueTrackerUrl();
        if (this$issueTrackerUrl == null ? other$issueTrackerUrl != null
                : !this$issueTrackerUrl.equals(other$issueTrackerUrl))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ProductConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $abbreviation = this.getAbbreviation();
        result = result * PRIME + ($abbreviation == null ? 43 : $abbreviation.hashCode());
        final java.lang.Object $productPagesCode = this.getProductPagesCode();
        result = result * PRIME + ($productPagesCode == null ? 43 : $productPagesCode.hashCode());
        final java.lang.Object $productManagers = this.getProductManagers();
        result = result * PRIME + ($productManagers == null ? 43 : $productManagers.hashCode());
        final java.lang.Object $stage = this.getStage();
        result = result * PRIME + ($stage == null ? 43 : $stage.hashCode());
        final java.lang.Object $issueTrackerUrl = this.getIssueTrackerUrl();
        result = result * PRIME + ($issueTrackerUrl == null ? 43 : $issueTrackerUrl.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ProductConfig(name=" + this.getName() + ", abbreviation=" + this.getAbbreviation()
                + ", productPagesCode=" + this.getProductPagesCode() + ", productManagers=" + this.getProductManagers()
                + ", stage=" + this.getStage() + ", issueTrackerUrl=" + this.getIssueTrackerUrl() + ")";
    }
}
