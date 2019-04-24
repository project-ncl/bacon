package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class LookupGAVsRequest   {
  
  private @Valid List<String> productNames = new ArrayList<String>();
  private @Valid List<Long> productVersionIds = new ArrayList<Long>();
  private @Valid String repositoryGroup;
  private @Valid String versionSuffix;
  private @Valid List<GAV> gavs = new ArrayList<GAV>();

  /**
   **/
  public LookupGAVsRequest productNames(List<String> productNames) {
    this.productNames = productNames;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("productNames")
  public List<String> getProductNames() {
    return productNames;
  }
  public void setProductNames(List<String> productNames) {
    this.productNames = productNames;
  }

  /**
   **/
  public LookupGAVsRequest productVersionIds(List<Long> productVersionIds) {
    this.productVersionIds = productVersionIds;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("productVersionIds")
  public List<Long> getProductVersionIds() {
    return productVersionIds;
  }
  public void setProductVersionIds(List<Long> productVersionIds) {
    this.productVersionIds = productVersionIds;
  }

  /**
   **/
  public LookupGAVsRequest repositoryGroup(String repositoryGroup) {
    this.repositoryGroup = repositoryGroup;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("repositoryGroup")
  public String getRepositoryGroup() {
    return repositoryGroup;
  }
  public void setRepositoryGroup(String repositoryGroup) {
    this.repositoryGroup = repositoryGroup;
  }

  /**
   **/
  public LookupGAVsRequest versionSuffix(String versionSuffix) {
    this.versionSuffix = versionSuffix;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("versionSuffix")
  public String getVersionSuffix() {
    return versionSuffix;
  }
  public void setVersionSuffix(String versionSuffix) {
    this.versionSuffix = versionSuffix;
  }

  /**
   **/
  public LookupGAVsRequest gavs(List<GAV> gavs) {
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
    LookupGAVsRequest lookupGAVsRequest = (LookupGAVsRequest) o;
    return Objects.equals(productNames, lookupGAVsRequest.productNames) &&
        Objects.equals(productVersionIds, lookupGAVsRequest.productVersionIds) &&
        Objects.equals(repositoryGroup, lookupGAVsRequest.repositoryGroup) &&
        Objects.equals(versionSuffix, lookupGAVsRequest.versionSuffix) &&
        Objects.equals(gavs, lookupGAVsRequest.gavs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productNames, productVersionIds, repositoryGroup, versionSuffix, gavs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LookupGAVsRequest {\n");
    
    sb.append("    productNames: ").append(toIndentedString(productNames)).append("\n");
    sb.append("    productVersionIds: ").append(toIndentedString(productVersionIds)).append("\n");
    sb.append("    repositoryGroup: ").append(toIndentedString(repositoryGroup)).append("\n");
    sb.append("    versionSuffix: ").append(toIndentedString(versionSuffix)).append("\n");
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

