package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ProductDiff   {
  
  private @Valid
  ProductWithGav leftProduct = null;
  private @Valid
  ProductWithGav rightProduct = null;
  private @Valid List<GAV> added = new ArrayList<GAV>();
  private @Valid List<GAV> removed = new ArrayList<GAV>();
  private @Valid List<GADiff> changed = new ArrayList<GADiff>();
  private @Valid List<GAV> unchanged = new ArrayList<GAV>();

  /**
   **/
  public ProductDiff leftProduct(ProductWithGav leftProduct) {
    this.leftProduct = leftProduct;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("leftProduct")
  public ProductWithGav getLeftProduct() {
    return leftProduct;
  }
  public void setLeftProduct(ProductWithGav leftProduct) {
    this.leftProduct = leftProduct;
  }

  /**
   **/
  public ProductDiff rightProduct(ProductWithGav rightProduct) {
    this.rightProduct = rightProduct;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("rightProduct")
  public ProductWithGav getRightProduct() {
    return rightProduct;
  }
  public void setRightProduct(ProductWithGav rightProduct) {
    this.rightProduct = rightProduct;
  }

  /**
   **/
  public ProductDiff added(List<GAV> added) {
    this.added = added;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("added")
  public List<GAV> getAdded() {
    return added;
  }
  public void setAdded(List<GAV> added) {
    this.added = added;
  }

  /**
   **/
  public ProductDiff removed(List<GAV> removed) {
    this.removed = removed;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("removed")
  public List<GAV> getRemoved() {
    return removed;
  }
  public void setRemoved(List<GAV> removed) {
    this.removed = removed;
  }

  /**
   **/
  public ProductDiff changed(List<GADiff> changed) {
    this.changed = changed;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("changed")
  public List<GADiff> getChanged() {
    return changed;
  }
  public void setChanged(List<GADiff> changed) {
    this.changed = changed;
  }

  /**
   **/
  public ProductDiff unchanged(List<GAV> unchanged) {
    this.unchanged = unchanged;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("unchanged")
  public List<GAV> getUnchanged() {
    return unchanged;
  }
  public void setUnchanged(List<GAV> unchanged) {
    this.unchanged = unchanged;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProductDiff productDiff = (ProductDiff) o;
    return Objects.equals(leftProduct, productDiff.leftProduct) &&
        Objects.equals(rightProduct, productDiff.rightProduct) &&
        Objects.equals(added, productDiff.added) &&
        Objects.equals(removed, productDiff.removed) &&
        Objects.equals(changed, productDiff.changed) &&
        Objects.equals(unchanged, productDiff.unchanged);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leftProduct, rightProduct, added, removed, changed, unchanged);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProductDiff {\n");
    
    sb.append("    leftProduct: ").append(toIndentedString(leftProduct)).append("\n");
    sb.append("    rightProduct: ").append(toIndentedString(rightProduct)).append("\n");
    sb.append("    added: ").append(toIndentedString(added)).append("\n");
    sb.append("    removed: ").append(toIndentedString(removed)).append("\n");
    sb.append("    changed: ").append(toIndentedString(changed)).append("\n");
    sb.append("    unchanged: ").append(toIndentedString(unchanged)).append("\n");
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

