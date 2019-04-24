package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class RestGA2GAVs   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid List<GAV> gavs = new ArrayList<GAV>();

  /**
   **/
  public RestGA2GAVs groupId(String groupId) {
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
  public RestGA2GAVs artifactId(String artifactId) {
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
  public RestGA2GAVs gavs(List<GAV> gavs) {
    this.gavs = gavs;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("gavs")
  public List<GAV> getGavs() {
    return gavs;
  }
  public void setGavs(List<GAV> gavs) {
    this.gavs = gavs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestGA2GAVs restGA2GAVs = (RestGA2GAVs) o;
    return Objects.equals(groupId, restGA2GAVs.groupId) &&
        Objects.equals(artifactId, restGA2GAVs.artifactId) &&
        Objects.equals(gavs, restGA2GAVs.gavs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, gavs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestGA2GAVs {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    gavs: ").append(toIndentedString(gavs)).append("\n");
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

