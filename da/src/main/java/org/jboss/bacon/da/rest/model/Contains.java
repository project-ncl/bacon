package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Contains   {
  
  private @Valid Boolean contains;
  private @Valid List<Artifact> found = new ArrayList<Artifact>();

  /**
   **/
  public Contains contains(Boolean contains) {
    this.contains = contains;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("contains")
  @NotNull
  public Boolean getContains() {
    return contains;
  }
  public void setContains(Boolean contains) {
    this.contains = contains;
  }

  /**
   **/
  public Contains found(List<Artifact> found) {
    this.found = found;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("found")
  @NotNull
  public List<Artifact> getFound() {
    return found;
  }
  public void setFound(List<Artifact> found) {
    this.found = found;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Contains contains = (Contains) o;
    return Objects.equals(contains, contains.contains) &&
        Objects.equals(found, contains.found);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contains, found);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Contains {\n");
    
    sb.append("    contains: ").append(toIndentedString(contains)).append("\n");
    sb.append("    found: ").append(toIndentedString(found)).append("\n");
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

