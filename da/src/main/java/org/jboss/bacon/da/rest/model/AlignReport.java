package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AlignReport   {
  
  private @Valid List<RestGA2RestGAV2VersionProducts> internallyBuilt = new ArrayList<RestGA2RestGAV2VersionProducts>();
  private @Valid List<RestGA2RestGAV2VersionProductsWithDiff> builtInDifferentVersion = new ArrayList<RestGA2RestGAV2VersionProductsWithDiff>();
  private @Valid List<RestGA2GAVs> notBuilt = new ArrayList<RestGA2GAVs>();
  private @Valid List<RestGA2GAVs> blacklisted = new ArrayList<RestGA2GAVs>();

  /**
   **/
  public AlignReport internallyBuilt(List<RestGA2RestGAV2VersionProducts> internallyBuilt) {
    this.internallyBuilt = internallyBuilt;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("internallyBuilt")
  public List<RestGA2RestGAV2VersionProducts> getInternallyBuilt() {
    return internallyBuilt;
  }
  public void setInternallyBuilt(List<RestGA2RestGAV2VersionProducts> internallyBuilt) {
    this.internallyBuilt = internallyBuilt;
  }

  /**
   **/
  public AlignReport builtInDifferentVersion(List<RestGA2RestGAV2VersionProductsWithDiff> builtInDifferentVersion) {
    this.builtInDifferentVersion = builtInDifferentVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("builtInDifferentVersion")
  public List<RestGA2RestGAV2VersionProductsWithDiff> getBuiltInDifferentVersion() {
    return builtInDifferentVersion;
  }
  public void setBuiltInDifferentVersion(List<RestGA2RestGAV2VersionProductsWithDiff> builtInDifferentVersion) {
    this.builtInDifferentVersion = builtInDifferentVersion;
  }

  /**
   **/
  public AlignReport notBuilt(List<RestGA2GAVs> notBuilt) {
    this.notBuilt = notBuilt;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("notBuilt")
  public List<RestGA2GAVs> getNotBuilt() {
    return notBuilt;
  }
  public void setNotBuilt(List<RestGA2GAVs> notBuilt) {
    this.notBuilt = notBuilt;
  }

  /**
   **/
  public AlignReport blacklisted(List<RestGA2GAVs> blacklisted) {
    this.blacklisted = blacklisted;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("blacklisted")
  public List<RestGA2GAVs> getBlacklisted() {
    return blacklisted;
  }
  public void setBlacklisted(List<RestGA2GAVs> blacklisted) {
    this.blacklisted = blacklisted;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AlignReport alignReport = (AlignReport) o;
    return Objects.equals(internallyBuilt, alignReport.internallyBuilt) &&
        Objects.equals(builtInDifferentVersion, alignReport.builtInDifferentVersion) &&
        Objects.equals(notBuilt, alignReport.notBuilt) &&
        Objects.equals(blacklisted, alignReport.blacklisted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(internallyBuilt, builtInDifferentVersion, notBuilt, blacklisted);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AlignReport {\n");
    
    sb.append("    internallyBuilt: ").append(toIndentedString(internallyBuilt)).append("\n");
    sb.append("    builtInDifferentVersion: ").append(toIndentedString(builtInDifferentVersion)).append("\n");
    sb.append("    notBuilt: ").append(toIndentedString(notBuilt)).append("\n");
    sb.append("    blacklisted: ").append(toIndentedString(blacklisted)).append("\n");
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

