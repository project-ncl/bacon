package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class RestGAV2VersionProducts   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String version;
  private @Valid List<RestVersionProduct> gavProducts = new ArrayList<RestVersionProduct>();

  /**
   **/
  public RestGAV2VersionProducts groupId(String groupId) {
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
  public RestGAV2VersionProducts artifactId(String artifactId) {
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
  public RestGAV2VersionProducts version(String version) {
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
  public RestGAV2VersionProducts gavProducts(List<RestVersionProduct> gavProducts) {
    this.gavProducts = gavProducts;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("gavProducts")
  public List<RestVersionProduct> getGavProducts() {
    return gavProducts;
  }
  public void setGavProducts(List<RestVersionProduct> gavProducts) {
    this.gavProducts = gavProducts;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestGAV2VersionProducts restGAV2VersionProducts = (RestGAV2VersionProducts) o;
    return Objects.equals(groupId, restGAV2VersionProducts.groupId) &&
        Objects.equals(artifactId, restGAV2VersionProducts.artifactId) &&
        Objects.equals(version, restGAV2VersionProducts.version) &&
        Objects.equals(gavProducts, restGAV2VersionProducts.gavProducts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, gavProducts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestGAV2VersionProducts {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    gavProducts: ").append(toIndentedString(gavProducts)).append("\n");
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

