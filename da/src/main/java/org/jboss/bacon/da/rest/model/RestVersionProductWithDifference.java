package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.Objects;


public class RestVersionProductWithDifference   {
  
  private @Valid String version;
  private @Valid String differenceType;
  private @Valid
  ProductWithGav product = null;

  /**
   **/
  public RestVersionProductWithDifference version(String version) {
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
  public RestVersionProductWithDifference differenceType(String differenceType) {
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

  /**
   **/
  public RestVersionProductWithDifference product(ProductWithGav product) {
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
    RestVersionProductWithDifference restVersionProductWithDifference = (RestVersionProductWithDifference) o;
    return Objects.equals(version, restVersionProductWithDifference.version) &&
        Objects.equals(differenceType, restVersionProductWithDifference.differenceType) &&
        Objects.equals(product, restVersionProductWithDifference.product);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, differenceType, product);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestVersionProductWithDifference {\n");
    
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    differenceType: ").append(toIndentedString(differenceType)).append("\n");
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

