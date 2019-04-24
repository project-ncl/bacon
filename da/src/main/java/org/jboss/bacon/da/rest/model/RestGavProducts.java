package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class RestGavProducts   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String version;
  private @Valid List<ProductWithGav> products = new ArrayList<ProductWithGav>();

  /**
   **/
  public RestGavProducts groupId(String groupId) {
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
  public RestGavProducts artifactId(String artifactId) {
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
  public RestGavProducts version(String version) {
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
  public RestGavProducts products(List<ProductWithGav> products) {
    this.products = products;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("products")
  public List<ProductWithGav> getProducts() {
    return products;
  }
  public void setProducts(List<ProductWithGav> products) {
    this.products = products;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestGavProducts restGavProducts = (RestGavProducts) o;
    return Objects.equals(groupId, restGavProducts.groupId) &&
        Objects.equals(artifactId, restGavProducts.artifactId) &&
        Objects.equals(version, restGavProducts.version) &&
        Objects.equals(products, restGavProducts.products);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, products);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestGavProducts {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    products: ").append(toIndentedString(products)).append("\n");
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

