package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Report   {
  
  private @Valid String groupId;
  private @Valid String artifactId;
  private @Valid String version;
  private @Valid List<String> availableVersions = new ArrayList<String>();
  private @Valid String bestMatchVersion;
  private @Valid Boolean dependencyVersionsSatisfied;
  private @Valid List<Report> dependencies = new ArrayList<Report>();
  private @Valid Boolean blacklisted;
  private @Valid List<ProductWithGav> whitelisted = new ArrayList<ProductWithGav>();
  private @Valid Integer notBuiltDependencies;

  /**
   **/
  public Report groupId(String groupId) {
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
  public Report artifactId(String artifactId) {
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
  public Report version(String version) {
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
  public Report availableVersions(List<String> availableVersions) {
    this.availableVersions = availableVersions;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("availableVersions")
  public List<String> getAvailableVersions() {
    return availableVersions;
  }
  public void setAvailableVersions(List<String> availableVersions) {
    this.availableVersions = availableVersions;
  }

  /**
   **/
  public Report bestMatchVersion(String bestMatchVersion) {
    this.bestMatchVersion = bestMatchVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("bestMatchVersion")
  public String getBestMatchVersion() {
    return bestMatchVersion;
  }
  public void setBestMatchVersion(String bestMatchVersion) {
    this.bestMatchVersion = bestMatchVersion;
  }

  /**
   **/
  public Report dependencyVersionsSatisfied(Boolean dependencyVersionsSatisfied) {
    this.dependencyVersionsSatisfied = dependencyVersionsSatisfied;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("dependencyVersionsSatisfied")
  public Boolean getDependencyVersionsSatisfied() {
    return dependencyVersionsSatisfied;
  }
  public void setDependencyVersionsSatisfied(Boolean dependencyVersionsSatisfied) {
    this.dependencyVersionsSatisfied = dependencyVersionsSatisfied;
  }

  /**
   **/
  public Report dependencies(List<Report> dependencies) {
    this.dependencies = dependencies;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("dependencies")
  public List<Report> getDependencies() {
    return dependencies;
  }
  public void setDependencies(List<Report> dependencies) {
    this.dependencies = dependencies;
  }

  /**
   **/
  public Report blacklisted(Boolean blacklisted) {
    this.blacklisted = blacklisted;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("blacklisted")
  public Boolean getBlacklisted() {
    return blacklisted;
  }
  public void setBlacklisted(Boolean blacklisted) {
    this.blacklisted = blacklisted;
  }

  /**
   **/
  public Report whitelisted(List<ProductWithGav> whitelisted) {
    this.whitelisted = whitelisted;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("whitelisted")
  public List<ProductWithGav> getWhitelisted() {
    return whitelisted;
  }
  public void setWhitelisted(List<ProductWithGav> whitelisted) {
    this.whitelisted = whitelisted;
  }

  /**
   **/
  public Report notBuiltDependencies(Integer notBuiltDependencies) {
    this.notBuiltDependencies = notBuiltDependencies;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("notBuiltDependencies")
  public Integer getNotBuiltDependencies() {
    return notBuiltDependencies;
  }
  public void setNotBuiltDependencies(Integer notBuiltDependencies) {
    this.notBuiltDependencies = notBuiltDependencies;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Report report = (Report) o;
    return Objects.equals(groupId, report.groupId) &&
        Objects.equals(artifactId, report.artifactId) &&
        Objects.equals(version, report.version) &&
        Objects.equals(availableVersions, report.availableVersions) &&
        Objects.equals(bestMatchVersion, report.bestMatchVersion) &&
        Objects.equals(dependencyVersionsSatisfied, report.dependencyVersionsSatisfied) &&
        Objects.equals(dependencies, report.dependencies) &&
        Objects.equals(blacklisted, report.blacklisted) &&
        Objects.equals(whitelisted, report.whitelisted) &&
        Objects.equals(notBuiltDependencies, report.notBuiltDependencies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, availableVersions, bestMatchVersion, dependencyVersionsSatisfied, dependencies, blacklisted, whitelisted, notBuiltDependencies);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Report {\n");
    
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    availableVersions: ").append(toIndentedString(availableVersions)).append("\n");
    sb.append("    bestMatchVersion: ").append(toIndentedString(bestMatchVersion)).append("\n");
    sb.append("    dependencyVersionsSatisfied: ").append(toIndentedString(dependencyVersionsSatisfied)).append("\n");
    sb.append("    dependencies: ").append(toIndentedString(dependencies)).append("\n");
    sb.append("    blacklisted: ").append(toIndentedString(blacklisted)).append("\n");
    sb.append("    whitelisted: ").append(toIndentedString(whitelisted)).append("\n");
    sb.append("    notBuiltDependencies: ").append(toIndentedString(notBuiltDependencies)).append("\n");
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

