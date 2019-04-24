package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.Objects;



public class ProductWithGav {
  
  private @Valid String name;
  private @Valid String version;
  private GAV gav;

public enum SupportStatusEnum {

    SUPPORTED(String.valueOf("SUPPORTED")), SUPERSEDED(String.valueOf("SUPERSEDED")), UNSUPPORTED(String.valueOf("UNSUPPORTED")), UNKNOWN(String.valueOf("UNKNOWN"));


    private String value;

    SupportStatusEnum (String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static SupportStatusEnum fromValue(String v) {
        for (SupportStatusEnum b : SupportStatusEnum.values()) {
            if (String.valueOf(b.value).equals(v)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + v + "'");
    }
}

  private @Valid SupportStatusEnum supportStatus;

  /**
   **/
  public ProductWithGav name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   **/
  public ProductWithGav version(String version) {
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
  public ProductWithGav supportStatus(SupportStatusEnum supportStatus) {
    this.supportStatus = supportStatus;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("supportStatus")
  public SupportStatusEnum getSupportStatus() {
    return supportStatus;
  }
  public void setSupportStatus(SupportStatusEnum supportStatus) {
    this.supportStatus = supportStatus;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProductWithGav product = (ProductWithGav) o;
    return Objects.equals(name, product.name) &&
        Objects.equals(version, product.version) &&
        Objects.equals(supportStatus, product.supportStatus);
  }

    public GAV getGav() {
        return gav;
    }

    public void setGav(GAV gav) {
        this.gav = gav;
    }

    @Override
  public int hashCode() {
    return Objects.hash(name, version, supportStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Product {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    supportStatus: ").append(toIndentedString(supportStatus)).append("\n");
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

