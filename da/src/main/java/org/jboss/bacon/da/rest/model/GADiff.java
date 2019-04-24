package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.Objects;


public class GADiff   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String leftVersion;
  private @Valid String rightVersion;
  private @Valid String differenceType;

  /**
   **/
  public GADiff groupId(String groupId) {
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
  public GADiff artifactId(String artifactId) {
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
  public GADiff leftVersion(String leftVersion) {
    this.leftVersion = leftVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("leftVersion")
  public String getLeftVersion() {
    return leftVersion;
  }
  public void setLeftVersion(String leftVersion) {
    this.leftVersion = leftVersion;
  }

  /**
   **/
  public GADiff rightVersion(String rightVersion) {
    this.rightVersion = rightVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("rightVersion")
  public String getRightVersion() {
    return rightVersion;
  }
  public void setRightVersion(String rightVersion) {
    this.rightVersion = rightVersion;
  }

  /**
   **/
  public GADiff differenceType(String differenceType) {
    this.differenceType = differenceType;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("differenceType")
  public String getDifferenceType() {
    return differenceType;
  }
  public void setDifferenceType(String differenceType) {
    this.differenceType = differenceType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GADiff gaDiff = (GADiff) o;
    return Objects.equals(groupId, gaDiff.groupId) &&
        Objects.equals(artifactId, gaDiff.artifactId) &&
        Objects.equals(leftVersion, gaDiff.leftVersion) &&
        Objects.equals(rightVersion, gaDiff.rightVersion) &&
        Objects.equals(differenceType, gaDiff.differenceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, leftVersion, rightVersion, differenceType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GADiff {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    leftVersion: ").append(toIndentedString(leftVersion)).append("\n");
    sb.append("    rightVersion: ").append(toIndentedString(rightVersion)).append("\n");
    sb.append("    differenceType: ").append(toIndentedString(differenceType)).append("\n");
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

