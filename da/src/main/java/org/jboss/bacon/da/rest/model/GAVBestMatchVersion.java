package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.Objects;


public class GAVBestMatchVersion   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String version;
  private @Valid String bestMatchVersion;

  /**
   **/
  public GAVBestMatchVersion groupId(String groupId) {
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
  public GAVBestMatchVersion artifactId(String artifactId) {
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

  /**
   **/
  public GAVBestMatchVersion version(String version) {
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
  public GAVBestMatchVersion bestMatchVersion(String bestMatchVersion) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GAVBestMatchVersion gaVBestMatchVersion = (GAVBestMatchVersion) o;
    return Objects.equals(groupId, gaVBestMatchVersion.groupId) &&
        Objects.equals(artifactId, gaVBestMatchVersion.artifactId) &&
        Objects.equals(version, gaVBestMatchVersion.version) &&
        Objects.equals(bestMatchVersion, gaVBestMatchVersion.bestMatchVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, bestMatchVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GAVBestMatchVersion {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    bestMatchVersion: ").append(toIndentedString(bestMatchVersion)).append("\n");
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

