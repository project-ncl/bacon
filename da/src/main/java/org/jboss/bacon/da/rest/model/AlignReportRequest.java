package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AlignReportRequest   {
  
  private @Valid List<Long> products = new ArrayList<Long>();
  private @Valid Boolean searchUnknownProducts;
  private @Valid String scmUrl;
  private @Valid String revision;
  private @Valid List<String> additionalRepos = new ArrayList<String>();
  private @Valid String pomPath;

  /**
   **/
  public AlignReportRequest products(List<Long> products) {
    this.products = products;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("products")
  public List<Long> getProducts() {
    return products;
  }
  public void setProducts(List<Long> products) {
    this.products = products;
  }

  /**
   **/
  public AlignReportRequest searchUnknownProducts(Boolean searchUnknownProducts) {
    this.searchUnknownProducts = searchUnknownProducts;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("searchUnknownProducts")
  public Boolean getSearchUnknownProducts() {
    return searchUnknownProducts;
  }
  public void setSearchUnknownProducts(Boolean searchUnknownProducts) {
    this.searchUnknownProducts = searchUnknownProducts;
  }

  /**
   **/
  public AlignReportRequest scmUrl(String scmUrl) {
    this.scmUrl = scmUrl;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("scmUrl")
  public String getScmUrl() {
    return scmUrl;
  }
  public void setScmUrl(String scmUrl) {
    this.scmUrl = scmUrl;
  }

  /**
   **/
  public AlignReportRequest revision(String revision) {
    this.revision = revision;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("revision")
  public String getRevision() {
    return revision;
  }
  public void setRevision(String revision) {
    this.revision = revision;
  }

  /**
   **/
  public AlignReportRequest additionalRepos(List<String> additionalRepos) {
    this.additionalRepos = additionalRepos;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("additionalRepos")
  public List<String> getAdditionalRepos() {
    return additionalRepos;
  }
  public void setAdditionalRepos(List<String> additionalRepos) {
    this.additionalRepos = additionalRepos;
  }

  /**
   **/
  public AlignReportRequest pomPath(String pomPath) {
    this.pomPath = pomPath;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("pomPath")
  public String getPomPath() {
    return pomPath;
  }
  public void setPomPath(String pomPath) {
    this.pomPath = pomPath;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AlignReportRequest alignReportRequest = (AlignReportRequest) o;
    return Objects.equals(products, alignReportRequest.products) &&
        Objects.equals(searchUnknownProducts, alignReportRequest.searchUnknownProducts) &&
        Objects.equals(scmUrl, alignReportRequest.scmUrl) &&
        Objects.equals(revision, alignReportRequest.revision) &&
        Objects.equals(additionalRepos, alignReportRequest.additionalRepos) &&
        Objects.equals(pomPath, alignReportRequest.pomPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(products, searchUnknownProducts, scmUrl, revision, additionalRepos, pomPath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AlignReportRequest {\n");
    
    sb.append("    products: ").append(toIndentedString(products)).append("\n");
    sb.append("    searchUnknownProducts: ").append(toIndentedString(searchUnknownProducts)).append("\n");
    sb.append("    scmUrl: ").append(toIndentedString(scmUrl)).append("\n");
    sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
    sb.append("    additionalRepos: ").append(toIndentedString(additionalRepos)).append("\n");
    sb.append("    pomPath: ").append(toIndentedString(pomPath)).append("\n");
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

