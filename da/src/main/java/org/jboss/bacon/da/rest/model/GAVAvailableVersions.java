package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class GAVAvailableVersions   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String version;
  private @Valid List<String> availableVersions = new ArrayList<String>();

  /**
   **/
  public GAVAvailableVersions groupId(String groupId) {
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
  public GAVAvailableVersions artifactId(String artifactId) {
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
  public GAVAvailableVersions version(String version) {
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
  public GAVAvailableVersions availableVersions(List<String> availableVersions) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GAVAvailableVersions gaVAvailableVersions = (GAVAvailableVersions) o;
    return Objects.equals(groupId, gaVAvailableVersions.groupId) &&
        Objects.equals(artifactId, gaVAvailableVersions.artifactId) &&
        Objects.equals(version, gaVAvailableVersions.version) &&
        Objects.equals(availableVersions, gaVAvailableVersions.availableVersions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, availableVersions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GAVAvailableVersions {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    availableVersions: ").append(toIndentedString(availableVersions)).append("\n");
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

