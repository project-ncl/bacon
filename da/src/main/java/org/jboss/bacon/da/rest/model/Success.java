package org.jboss.bacon.da.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;


public class Success   {
  
  private @Valid Boolean success;
  private @Valid Long id;
  private @Valid String message;

  /**
   **/
  public Success success(Boolean success) {
    this.success = success;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("success")
  @NotNull
  public Boolean getSuccess() {
    return success;
  }
  public void setSuccess(Boolean success) {
    this.success = success;
  }

  /**
   **/
  public Success id(Long id) {
    this.id = id;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("id")
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  /**
   **/
  public Success message(String message) {
    this.message = message;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Success success = (Success) o;
    return Objects.equals(success, success.success) &&
        Objects.equals(id, success.id) &&
        Objects.equals(message, success.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, id, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Success {\n");
    
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

