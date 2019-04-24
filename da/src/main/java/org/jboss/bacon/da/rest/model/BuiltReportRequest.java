package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class BuiltReportRequest   {
  
  private @Valid String scmUrl;
  private @Valid String revision;
  private @Valid List<String> additionalRepos = new ArrayList<String>();
  private @Valid String pomPath;

  /**
   **/
  public BuiltReportRequest scmUrl(String scmUrl) {
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
  public BuiltReportRequest revision(String revision) {
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
  public BuiltReportRequest additionalRepos(List<String> additionalRepos) {
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
  public BuiltReportRequest pomPath(String pomPath) {
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
    BuiltReportRequest builtReportRequest = (BuiltReportRequest) o;
    return Objects.equals(scmUrl, builtReportRequest.scmUrl) &&
        Objects.equals(revision, builtReportRequest.revision) &&
        Objects.equals(additionalRepos, builtReportRequest.additionalRepos) &&
        Objects.equals(pomPath, builtReportRequest.pomPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scmUrl, revision, additionalRepos, pomPath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BuiltReportRequest {\n");
    
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

