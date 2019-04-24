package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class SCMLocator   {
  
  private @Valid String scmUrl;
  private @Valid String revision;
  private @Valid String pomPath;
  private @Valid List<String> repositories = new ArrayList<String>();

  /**
   **/
  public SCMLocator scmUrl(String scmUrl) {
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
  public SCMLocator revision(String revision) {
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
  public SCMLocator pomPath(String pomPath) {
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
  public SCMLocator repositories(List<String> repositories) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SCMLocator scMLocator = (SCMLocator) o;
    return Objects.equals(scmUrl, scMLocator.scmUrl) &&
        Objects.equals(revision, scMLocator.revision) &&
        Objects.equals(pomPath, scMLocator.pomPath) &&
        Objects.equals(repositories, scMLocator.repositories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scmUrl, revision, pomPath, repositories);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SCMLocator {\n");
    
    sb.append("    scmUrl: ").append(toIndentedString(scmUrl)).append("\n");
    sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
    sb.append("    pomPath: ").append(toIndentedString(pomPath)).append("\n");
    sb.append("    repositories: ").append(toIndentedString(repositories)).append("\n");
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

