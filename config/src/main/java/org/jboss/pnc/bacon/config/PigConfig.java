package org.jboss.pnc.bacon.config;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com 2020-06-05
 */
public class PigConfig implements Validate {
    private String kojiHubUrl;
    private String licenseServiceUrl;
    private String indyUrl;

    @Override
    public void validate() {
        Validate.validateUrl(kojiHubUrl, "KojiHub URL");
        Validate.validateUrl(licenseServiceUrl, "License Service URL");
        Validate.validateUrl(indyUrl, "Indy URL");
    }

    @java.lang.SuppressWarnings("all")
    public PigConfig() {
    }

    @java.lang.SuppressWarnings("all")
    public String getKojiHubUrl() {
        return this.kojiHubUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getLicenseServiceUrl() {
        return this.licenseServiceUrl;
    }

    @java.lang.SuppressWarnings("all")
    public String getIndyUrl() {
        return this.indyUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setKojiHubUrl(final String kojiHubUrl) {
        this.kojiHubUrl = kojiHubUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setLicenseServiceUrl(final String licenseServiceUrl) {
        this.licenseServiceUrl = licenseServiceUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setIndyUrl(final String indyUrl) {
        this.indyUrl = indyUrl;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PigConfig))
            return false;
        final PigConfig other = (PigConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$kojiHubUrl = this.getKojiHubUrl();
        final java.lang.Object other$kojiHubUrl = other.getKojiHubUrl();
        if (this$kojiHubUrl == null ? other$kojiHubUrl != null : !this$kojiHubUrl.equals(other$kojiHubUrl))
            return false;
        final java.lang.Object this$licenseServiceUrl = this.getLicenseServiceUrl();
        final java.lang.Object other$licenseServiceUrl = other.getLicenseServiceUrl();
        if (this$licenseServiceUrl == null ? other$licenseServiceUrl != null
                : !this$licenseServiceUrl.equals(other$licenseServiceUrl))
            return false;
        final java.lang.Object this$indyUrl = this.getIndyUrl();
        final java.lang.Object other$indyUrl = other.getIndyUrl();
        if (this$indyUrl == null ? other$indyUrl != null : !this$indyUrl.equals(other$indyUrl))
            return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PigConfig;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $kojiHubUrl = this.getKojiHubUrl();
        result = result * PRIME + ($kojiHubUrl == null ? 43 : $kojiHubUrl.hashCode());
        final java.lang.Object $licenseServiceUrl = this.getLicenseServiceUrl();
        result = result * PRIME + ($licenseServiceUrl == null ? 43 : $licenseServiceUrl.hashCode());
        final java.lang.Object $indyUrl = this.getIndyUrl();
        result = result * PRIME + ($indyUrl == null ? 43 : $indyUrl.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "PigConfig(kojiHubUrl=" + this.getKojiHubUrl() + ", licenseServiceUrl=" + this.getLicenseServiceUrl()
                + ", indyUrl=" + this.getIndyUrl() + ")";
    }
}
