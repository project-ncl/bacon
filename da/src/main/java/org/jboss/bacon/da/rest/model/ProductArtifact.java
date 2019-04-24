package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ProductArtifact   {
  
  private @Valid String scmUrl;
  private @Valid String revision;
  private @Valid String pomPath;
  private @Valid List<String> repositories = new ArrayList<String>();
  private @Valid Long productId;

  /**
   **/
  public ProductArtifact scmUrl(String scmUrl) {
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
  public ProductArtifact revision(String revision) {
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
  public ProductArtifact pomPath(String pomPath) {
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

  /**
   **/
  public ProductArtifact repositories(List<String> repositories) {
    this.repositories = repositories;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("repositories")
  public List<String> getRepositories() {
    return repositories;
  }
  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }

  /**
   **/
  public ProductArtifact productId(Long productId) {
    this.productId = productId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("productId")
  public Long getProductId() {
    return productId;
  }
  public void setProductId(Long productId) {
    this.productId = productId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProductArtifact productArtifact = (ProductArtifact) o;
    return Objects.equals(scmUrl, productArtifact.scmUrl) &&
        Objects.equals(revision, productArtifact.revision) &&
        Objects.equals(pomPath, productArtifact.pomPath) &&
        Objects.equals(repositories, productArtifact.repositories) &&
        Objects.equals(productId, productArtifact.productId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scmUrl, revision, pomPath, repositories, productId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProductArtifact {\n");
    
    sb.append("    scmUrl: ").append(toIndentedString(scmUrl)).append("\n");
    sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
    sb.append("    pomPath: ").append(toIndentedString(pomPath)).append("\n");
    sb.append("    repositories: ").append(toIndentedString(repositories)).append("\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
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

