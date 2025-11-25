package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SortAndOrder
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class SortAndOrder   {
  @JsonProperty("orderBy")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String orderBy = null;

  /**
   * Gets or Sets sortBy
   */
  public enum SortByEnum {
    ASC("asc"),
    
    DESC("desc");

    private String value;

    SortByEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SortByEnum fromValue(String text) {
      for (SortByEnum b : SortByEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
  @JsonProperty("sortBy")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private SortByEnum sortBy = null;


  public SortAndOrder orderBy(String orderBy) { 

    this.orderBy = orderBy;
    return this;
  }

  /**
   * Get orderBy
   * @return orderBy
   **/
  
  @Schema(example = "creationDate", description = "")
  
  public String getOrderBy() {  
    return orderBy;
  }



  public void setOrderBy(String orderBy) { 
    this.orderBy = orderBy;
  }

  public SortAndOrder sortBy(SortByEnum sortBy) { 

    this.sortBy = sortBy;
    return this;
  }

  /**
   * Get sortBy
   * @return sortBy
   **/
  
  @Schema(description = "")
  
  public SortByEnum getSortBy() {  
    return sortBy;
  }



  public void setSortBy(SortByEnum sortBy) { 
    this.sortBy = sortBy;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SortAndOrder sortAndOrder = (SortAndOrder) o;
    return Objects.equals(this.orderBy, sortAndOrder.orderBy) &&
        Objects.equals(this.sortBy, sortAndOrder.sortBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderBy, sortBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SortAndOrder {\n");
    
    sb.append("    orderBy: ").append(toIndentedString(orderBy)).append("\n");
    sb.append("    sortBy: ").append(toIndentedString(sortBy)).append("\n");
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
