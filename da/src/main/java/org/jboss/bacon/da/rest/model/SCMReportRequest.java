package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class SCMReportRequest   {
  
  private @Valid List<String> productNames = new ArrayList<String>();
  private @Valid List<Long> productVersionIds = new ArrayList<Long>();
  private @Valid SCMLocator scml = null;

  /**
   **/
  public SCMReportRequest productNames(List<String> productNames) {
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
  public SCMReportRequest productVersionIds(List<Long> productVersionIds) {
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
  public SCMReportRequest scml(SCMLocator scml) {
    this.scml = scml;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("scml")
  public SCMLocator getScml() {
    return scml;
  }
  public void setScml(SCMLocator scml) {
    this.scml = scml;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SCMReportRequest scMReportRequest = (SCMReportRequest) o;
    return Objects.equals(productNames, scMReportRequest.productNames) &&
        Objects.equals(productVersionIds, scMReportRequest.productVersionIds) &&
        Objects.equals(scml, scMReportRequest.scml);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productNames, productVersionIds, scml);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SCMReportRequest {\n");
    
    sb.append("    productNames: ").append(toIndentedString(productNames)).append("\n");
    sb.append("    productVersionIds: ").append(toIndentedString(productVersionIds)).append("\n");
    sb.append("    scml: ").append(toIndentedString(scml)).append("\n");
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

