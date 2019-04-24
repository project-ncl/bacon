package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class BuiltReport   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String version;
  private @Valid String builtVersion;
  private @Valid List<String> availableVersions = new ArrayList<String>();

  /**
   **/
  public BuiltReport groupId(String groupId) {
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
  public BuiltReport artifactId(String artifactId) {
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
  public BuiltReport version(String version) {
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
  public BuiltReport builtVersion(String builtVersion) {
    this.builtVersion = builtVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("builtVersion")
  public String getBuiltVersion() {
    return builtVersion;
  }
  public void setBuiltVersion(String builtVersion) {
    this.builtVersion = builtVersion;
  }

  /**
   **/
  public BuiltReport availableVersions(List<String> availableVersions) {
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
    BuiltReport builtReport = (BuiltReport) o;
    return Objects.equals(groupId, builtReport.groupId) &&
        Objects.equals(artifactId, builtReport.artifactId) &&
        Objects.equals(version, builtReport.version) &&
        Objects.equals(builtVersion, builtReport.builtVersion) &&
        Objects.equals(availableVersions, builtReport.availableVersions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, builtVersion, availableVersions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BuiltReport {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    builtVersion: ").append(toIndentedString(builtVersion)).append("\n");
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

