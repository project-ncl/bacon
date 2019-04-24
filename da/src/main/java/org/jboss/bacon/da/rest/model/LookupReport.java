package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class LookupReport   {
  
  private @Valid String bestMatchVersion;
  private @Valid List<String> availableVersions = new ArrayList<String>();
  private @Valid Boolean blacklisted;
  private @Valid List<ProductWithGav> whitelisted = new ArrayList<ProductWithGav>();
  private @Valid String version;
  private @Valid String groupId;
  private @Valid String artifactId;

  /**
   **/
  public LookupReport bestMatchVersion(String bestMatchVersion) {
    this.bestMatchVersion = bestMatchVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("bestMatchVersion")
  public String getBestMatchVersion() {
    return bestMatchVersion;
  }
  public void setBestMatchVersion(String bestMatchVersion) {
    this.bestMatchVersion = bestMatchVersion;
  }

  /**
   **/
  public LookupReport availableVersions(List<String> availableVersions) {
    this.availableVersions = availableVersions;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("availableVersions")
  public List<String> getAvailableVersions() {
    return availableVersions;
  }
  public void setAvailableVersions(List<String> availableVersions) {
    this.availableVersions = availableVersions;
  }

  /**
   **/
  public LookupReport blacklisted(Boolean blacklisted) {
    this.blacklisted = blacklisted;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("blacklisted")
  public Boolean getBlacklisted() {
    return blacklisted;
  }
  public void setBlacklisted(Boolean blacklisted) {
    this.blacklisted = blacklisted;
  }

  /**
   **/
  public LookupReport whitelisted(List<ProductWithGav> whitelisted) {
    this.whitelisted = whitelisted;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("whitelisted")
  public List<ProductWithGav> getWhitelisted() {
    return whitelisted;
  }
  public void setWhitelisted(List<ProductWithGav> whitelisted) {
    this.whitelisted = whitelisted;
  }

  /**
   **/
  public LookupReport version(String version) {
    this.version = version;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   **/
  public LookupReport groupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("groupId")
  public String getGroupId() {
    return groupId;
  }
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  /**
   **/
  public LookupReport artifactId(String artifactId) {
    this.artifactId = artifactId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("artifactId")
  public String getArtifactId() {
    return artifactId;
  }
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LookupReport lookupReport = (LookupReport) o;
    return Objects.equals(bestMatchVersion, lookupReport.bestMatchVersion) &&
        Objects.equals(availableVersions, lookupReport.availableVersions) &&
        Objects.equals(blacklisted, lookupReport.blacklisted) &&
        Objects.equals(whitelisted, lookupReport.whitelisted) &&
        Objects.equals(version, lookupReport.version) &&
        Objects.equals(groupId, lookupReport.groupId) &&
        Objects.equals(artifactId, lookupReport.artifactId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bestMatchVersion, availableVersions, blacklisted, whitelisted, version, groupId, artifactId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LookupReport {\n");
    
    sb.append("    bestMatchVersion: ").append(toIndentedString(bestMatchVersion)).append("\n");
    sb.append("    availableVersions: ").append(toIndentedString(availableVersions)).append("\n");
    sb.append("    blacklisted: ").append(toIndentedString(blacklisted)).append("\n");
    sb.append("    whitelisted: ").append(toIndentedString(whitelisted)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
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

