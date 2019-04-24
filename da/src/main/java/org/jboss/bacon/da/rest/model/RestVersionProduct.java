package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.Objects;


public class RestVersionProduct   {
  
  private @Valid String version;
  private @Valid
  ProductWithGav product = null;

  /**
   **/
  public RestVersionProduct version(String version) {
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
  public RestVersionProduct product(ProductWithGav product) {
    this.product = product;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("product")
  public ProductWithGav getProduct() {
    return product;
  }
  public void setProduct(ProductWithGav product) {
    this.product = product;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RestVersionProduct restVersionProduct = (RestVersionProduct) o;
    return Objects.equals(version, restVersionProduct.version) &&
        Objects.equals(product, restVersionProduct.product);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, product);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestVersionProduct {\n");
    
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    product: ").append(toIndentedString(product)).append("\n");
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

