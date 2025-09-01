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

import java.util.Map;

public class ConfigProfile {
    private String name;
    private String keycloakUrl;
    private PncConfig pnc;
    private DaConfig da;
    private IndyConfig indy;
    private PigConfig pig;
    private KeycloakConfig keycloak;
    private AutobuildConfig autobuild;
    private RexConfig rex;
    private boolean enableExperimental;
    private Map<String, Map<String, ?>> addOns;

    @java.lang.SuppressWarnings("all")
    public ConfigProfile() {
    }

    @java.lang.SuppressWarnings("all")
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    public String getKeycloakUrl() {
        return this.keycloakUrl;
    }

    @java.lang.SuppressWarnings("all")
    public PncConfig getPnc() {
        return this.pnc;
    }

    @java.lang.SuppressWarnings("all")
    public DaConfig getDa() {
        return this.da;
    }

    @java.lang.SuppressWarnings("all")
    public IndyConfig getIndy() {
        return this.indy;
    }

    @java.lang.SuppressWarnings("all")
    public PigConfig getPig() {
        return this.pig;
    }

    @java.lang.SuppressWarnings("all")
    public KeycloakConfig getKeycloak() {
        return this.keycloak;
    }

    @java.lang.SuppressWarnings("all")
    public AutobuildConfig getAutobuild() {
        return this.autobuild;
    }

    @java.lang.SuppressWarnings("all")
    public RexConfig getRex() {
        return this.rex;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isEnableExperimental() {
        return this.enableExperimental;
    }

    @java.lang.SuppressWarnings("all")
    public Map<String, Map<String, ?>> getAddOns() {
        return this.addOns;
    }

    @java.lang.SuppressWarnings("all")
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    public void setKeycloakUrl(final String keycloakUrl) {
        this.keycloakUrl = keycloakUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnc(final PncConfig pnc) {
        this.pnc = pnc;
    }

    @java.lang.SuppressWarnings("all")
    public void setDa(final DaConfig da) {
        this.da = da;
    }

    @java.lang.SuppressWarnings("all")
    public void setIndy(final IndyConfig indy) {
        this.indy = indy;
    }

    @java.lang.SuppressWarnings("all")
    public void setPig(final PigConfig pig) {
        this.pig = pig;
    }

    @java.lang.SuppressWarnings("all")
    public void setKeycloak(final KeycloakConfig keycloak) {
        this.keycloak = keycloak;
    }

    @java.lang.SuppressWarnings("all")
    public void setAutobuild(final AutobuildConfig autobuild) {
        this.autobuild = autobuild;
    }

    @java.lang.SuppressWarnings("all")
    public void setRex(final RexConfig rex) {
        this.rex = rex;
    }

    @java.lang.SuppressWarnings("all")
    public void setEnableExperimental(final boolean enableExperimental) {
        this.enableExperimental = enableExperimental;
    }

    @java.lang.SuppressWarnings("all")
    public void setAddOns(final Map<String, Map<String, ?>> addOns) {
        this.addOns = addOns;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ConfigProfile))
            return false;
        final ConfigProfile other = (ConfigProfile) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isEnableExperimental() != other.isEnableExperimental())
            return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name))
            return false;
        final java.lang.Object this$keycloakUrl = this.getKeycloakUrl();
        final java.lang.Object other$keycloakUrl = other.getKeycloakUrl();
        if (this$keycloakUrl == null ? other$keycloakUrl != null : !this$keycloakUrl.equals(other$keycloakUrl))
            return false;
        final java.lang.Object this$pnc = this.getPnc();
        final java.lang.Object other$pnc = other.getPnc();
        if (this$pnc == null ? other$pnc != null : !this$pnc.equals(other$pnc))
            return false;
        final java.lang.Object this$da = this.getDa();
        final java.lang.Object other$da = other.getDa();
        if (this$da == null ? other$da != null : !this$da.equals(other$da))
            return false;
        final java.lang.Object this$indy = this.getIndy();
        final java.lang.Object other$indy = other.getIndy();
        if (this$indy == null ? other$indy != null : !this$indy.equals(other$indy))
            return false;
        final java.lang.Object this$pig = this.getPig();
        final java.lang.Object other$pig = other.getPig();
        if (this$pig == null ? other$pig != null : !this$pig.equals(other$pig))
            return false;
        final java.lang.Object this$keycloak = this.getKeycloak();
        final java.lang.Object other$keycloak = other.getKeycloak();
        if (this$keycloak == null ? other$keycloak != null : !this$keycloak.equals(other$keycloak))
            return false;
        final java.lang.Object this$autobuild = this.getAutobuild();
        final java.lang.Object other$autobuild = other.getAutobuild();
        if (this$autobuild == null ? other$autobuild != null : !this$autobuild.equals(other$autobuild))
            return false;
        final java.lang.Object this$rex = this.getRex();
        final java.lang.Object other$rex = other.getRex();
        if (this$rex == null ? other$rex != null : !this$rex.equals(other$rex))
            return false;
        final java.lang.Object this$addOns = this.getAddOns();
        final java.lang.Object other$addOns = other.getAddOns();
        if (this$addOns == null ? other$addOns != null : !this$addOns.equals(other$addOns))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ConfigProfile;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isEnableExperimental() ? 79 : 97);
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $keycloakUrl = this.getKeycloakUrl();
        result = result * PRIME + ($keycloakUrl == null ? 43 : $keycloakUrl.hashCode());
        final java.lang.Object $pnc = this.getPnc();
        result = result * PRIME + ($pnc == null ? 43 : $pnc.hashCode());
        final java.lang.Object $da = this.getDa();
        result = result * PRIME + ($da == null ? 43 : $da.hashCode());
        final java.lang.Object $indy = this.getIndy();
        result = result * PRIME + ($indy == null ? 43 : $indy.hashCode());
        final java.lang.Object $pig = this.getPig();
        result = result * PRIME + ($pig == null ? 43 : $pig.hashCode());
        final java.lang.Object $keycloak = this.getKeycloak();
        result = result * PRIME + ($keycloak == null ? 43 : $keycloak.hashCode());
        final java.lang.Object $autobuild = this.getAutobuild();
        result = result * PRIME + ($autobuild == null ? 43 : $autobuild.hashCode());
        final java.lang.Object $rex = this.getRex();
        result = result * PRIME + ($rex == null ? 43 : $rex.hashCode());
        final java.lang.Object $addOns = this.getAddOns();
        result = result * PRIME + ($addOns == null ? 43 : $addOns.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ConfigProfile(name=" + this.getName() + ", keycloakUrl=" + this.getKeycloakUrl() + ", pnc="
                + this.getPnc() + ", da=" + this.getDa() + ", indy=" + this.getIndy() + ", pig=" + this.getPig()
                + ", keycloak=" + this.getKeycloak() + ", autobuild=" + this.getAutobuild() + ", rex=" + this.getRex()
                + ", enableExperimental=" + this.isEnableExperimental() + ", addOns=" + this.getAddOns() + ")";
    }
}
