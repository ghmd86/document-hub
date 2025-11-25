package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ErrorResponse
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class ErrorResponse   {
  @JsonProperty("errorMsg")
  @Valid
  private List<String> errorMsg = null;
  @JsonProperty("statusCode")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Integer statusCode = 1;

  @JsonProperty("timestamp")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String timestamp = null;


  public ErrorResponse errorMsg(List<String> errorMsg) { 

    this.errorMsg = errorMsg;
    return this;
  }

  public ErrorResponse addErrorMsgItem(String errorMsgItem) {
    if (this.errorMsg == null) {
      this.errorMsg = new ArrayList<String>();
    }
    this.errorMsg.add(errorMsgItem);
    return this;
  }

  /**
   * Get errorMsg
   * @return errorMsg
   **/
  
  @Schema(description = "")
  
  public List<String> getErrorMsg() {  
    return errorMsg;
  }



  public void setErrorMsg(List<String> errorMsg) { 
    this.errorMsg = errorMsg;
  }

  public ErrorResponse statusCode(Integer statusCode) { 

    this.statusCode = statusCode;
    return this;
  }

  /**
   * Get statusCode
   * @return statusCode
   **/
  
  @Schema(example = "1", description = "")
  
  public Integer getStatusCode() {  
    return statusCode;
  }



  public void setStatusCode(Integer statusCode) { 
    this.statusCode = statusCode;
  }

  public ErrorResponse timestamp(String timestamp) { 

    this.timestamp = timestamp;
    return this;
  }

  /**
   * Epoch time 
   * @return timestamp
   **/
  
  @Schema(example = "1736770074", description = "Epoch time ")
  
  public String getTimestamp() {  
    return timestamp;
  }



  public void setTimestamp(String timestamp) { 
    this.timestamp = timestamp;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorResponse errorResponse = (ErrorResponse) o;
    return Objects.equals(this.errorMsg, errorResponse.errorMsg) &&
        Objects.equals(this.statusCode, errorResponse.statusCode) &&
        Objects.equals(this.timestamp, errorResponse.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMsg, statusCode, timestamp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorResponse {\n");
    
    sb.append("    errorMsg: ").append(toIndentedString(errorMsg)).append("\n");
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
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
