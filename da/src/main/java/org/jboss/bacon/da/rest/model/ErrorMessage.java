package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.Objects;



public class ErrorMessage   {
  

public enum ErrorTypeEnum {

    BLACKLIST(String.valueOf("BLACKLIST")), UNEXPECTED_SERVER_ERR(String.valueOf("UNEXPECTED_SERVER_ERR")), PRODUCT_NOT_FOUND(String.valueOf("PRODUCT_NOT_FOUND")), PARAMS_REQUIRED(String.valueOf("PARAMS_REQUIRED")), NO_RELATIONSHIP_FOUND(String.valueOf("NO_RELATIONSHIP_FOUND")), GA_NOT_FOUND(String.valueOf("GA_NOT_FOUND")), COMMUNICATION_FAIL(String.valueOf("COMMUNICATION_FAIL")), SCM_ENDPOINT(String.valueOf("SCM_ENDPOINT")), POM_ANALYSIS(String.valueOf("POM_ANALYSIS")), ILLEGAL_ARGUMENTS(String.valueOf("ILLEGAL_ARGUMENTS")), INCORRECT_DATA(String.valueOf("INCORRECT_DATA")), SCM_ANALYSIS(String.valueOf("SCM_ANALYSIS")), INPUT_VALIDATION(String.valueOf("INPUT_VALIDATION"));


    private String value;

    ErrorTypeEnum (String v) {
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
    public static ErrorTypeEnum fromValue(String v) {
        for (ErrorTypeEnum b : ErrorTypeEnum.values()) {
            if (String.valueOf(b.value).equals(v)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + v + "'");
    }
}

  private @Valid ErrorTypeEnum errorType;
  private @Valid String errorMessage;
  private @Valid Object details = null;

  /**
   **/
  public ErrorMessage errorType(ErrorTypeEnum errorType) {
    this.errorType = errorType;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("errorType")
  public ErrorTypeEnum getErrorType() {
    return errorType;
  }
  public void setErrorType(ErrorTypeEnum errorType) {
    this.errorType = errorType;
  }

  /**
   **/
  public ErrorMessage errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   **/
  public ErrorMessage details(Object details) {
    this.details = details;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("details")
  public Object getDetails() {
    return details;
  }
  public void setDetails(Object details) {
    this.details = details;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorMessage errorMessage = (ErrorMessage) o;
    return Objects.equals(errorType, errorMessage.errorType) &&
        Objects.equals(errorMessage, errorMessage.errorMessage) &&
        Objects.equals(details, errorMessage.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorType, errorMessage, details);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorMessage {\n");
    
    sb.append("    errorType: ").append(toIndentedString(errorType)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
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

