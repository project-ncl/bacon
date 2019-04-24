package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AdvancedReport   {
  
  private @Valid Report report = null;
  private @Valid List<GAV> blacklistedArtifacts = new ArrayList<GAV>();
  private @Valid List<RestGavProducts> whitelistedArtifacts = new ArrayList<RestGavProducts>();
  private @Valid List<GAVBestMatchVersion> communityGavsWithBestMatchVersions = new ArrayList<GAVBestMatchVersion>();
  private @Valid List<GAVAvailableVersions> communityGavsWithBuiltVersions = new ArrayList<GAVAvailableVersions>();
  private @Valid List<GAV> communityGavs = new ArrayList<GAV>();

  /**
   **/
  public AdvancedReport report(Report report) {
    this.report = report;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("report")
  public Report getReport() {
    return report;
  }
  public void setReport(Report report) {
    this.report = report;
  }

  /**
   **/
  public AdvancedReport blacklistedArtifacts(List<GAV> blacklistedArtifacts) {
    this.blacklistedArtifacts = blacklistedArtifacts;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("blacklistedArtifacts")
  public List<GAV> getBlacklistedArtifacts() {
    return blacklistedArtifacts;
  }
  public void setBlacklistedArtifacts(List<GAV> blacklistedArtifacts) {
    this.blacklistedArtifacts = blacklistedArtifacts;
  }

  /**
   **/
  public AdvancedReport whitelistedArtifacts(List<RestGavProducts> whitelistedArtifacts) {
    this.whitelistedArtifacts = whitelistedArtifacts;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("whitelistedArtifacts")
  public List<RestGavProducts> getWhitelistedArtifacts() {
    return whitelistedArtifacts;
  }
  public void setWhitelistedArtifacts(List<RestGavProducts> whitelistedArtifacts) {
    this.whitelistedArtifacts = whitelistedArtifacts;
  }

  /**
   **/
  public AdvancedReport communityGavsWithBestMatchVersions(List<GAVBestMatchVersion> communityGavsWithBestMatchVersions) {
    this.communityGavsWithBestMatchVersions = communityGavsWithBestMatchVersions;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("communityGavsWithBestMatchVersions")
  public List<GAVBestMatchVersion> getCommunityGavsWithBestMatchVersions() {
    return communityGavsWithBestMatchVersions;
  }
  public void setCommunityGavsWithBestMatchVersions(List<GAVBestMatchVersion> communityGavsWithBestMatchVersions) {
    this.communityGavsWithBestMatchVersions = communityGavsWithBestMatchVersions;
  }

  /**
   **/
  public AdvancedReport communityGavsWithBuiltVersions(List<GAVAvailableVersions> communityGavsWithBuiltVersions) {
    this.communityGavsWithBuiltVersions = communityGavsWithBuiltVersions;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("communityGavsWithBuiltVersions")
  public List<GAVAvailableVersions> getCommunityGavsWithBuiltVersions() {
    return communityGavsWithBuiltVersions;
  }
  public void setCommunityGavsWithBuiltVersions(List<GAVAvailableVersions> communityGavsWithBuiltVersions) {
    this.communityGavsWithBuiltVersions = communityGavsWithBuiltVersions;
  }

  /**
   **/
  public AdvancedReport communityGavs(List<GAV> communityGavs) {
    this.communityGavs = communityGavs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("communityGavs")
  public List<GAV> getCommunityGavs() {
    return communityGavs;
  }
  public void setCommunityGavs(List<GAV> communityGavs) {
    this.communityGavs = communityGavs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdvancedReport advancedReport = (AdvancedReport) o;
    return Objects.equals(report, advancedReport.report) &&
        Objects.equals(blacklistedArtifacts, advancedReport.blacklistedArtifacts) &&
        Objects.equals(whitelistedArtifacts, advancedReport.whitelistedArtifacts) &&
        Objects.equals(communityGavsWithBestMatchVersions, advancedReport.communityGavsWithBestMatchVersions) &&
        Objects.equals(communityGavsWithBuiltVersions, advancedReport.communityGavsWithBuiltVersions) &&
        Objects.equals(communityGavs, advancedReport.communityGavs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(report, blacklistedArtifacts, whitelistedArtifacts, communityGavsWithBestMatchVersions, communityGavsWithBuiltVersions, communityGavs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdvancedReport {\n");
    
    sb.append("    report: ").append(toIndentedString(report)).append("\n");
    sb.append("    blacklistedArtifacts: ").append(toIndentedString(blacklistedArtifacts)).append("\n");
    sb.append("    whitelistedArtifacts: ").append(toIndentedString(whitelistedArtifacts)).append("\n");
    sb.append("    communityGavsWithBestMatchVersions: ").append(toIndentedString(communityGavsWithBestMatchVersions)).append("\n");
    sb.append("    communityGavsWithBuiltVersions: ").append(toIndentedString(communityGavsWithBuiltVersions)).append("\n");
    sb.append("    communityGavs: ").append(toIndentedString(communityGavs)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

