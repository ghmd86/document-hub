package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * PaginationResponse
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class PaginationResponse   {
  @JsonProperty("pageSize")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Integer pageSize = null;

  @JsonProperty("totalItems")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Integer totalItems = null;

  @JsonProperty("totalPages")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Integer totalPages = null;

  @JsonProperty("pageNumber")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Integer pageNumber = null;


  public PaginationResponse pageSize(Integer pageSize) { 

    this.pageSize = pageSize;
    return this;
  }

  /**
   * The number of items per page.
   * @return pageSize
   **/
  
  @Schema(description = "The number of items per page.")
  
  public Integer getPageSize() {  
    return pageSize;
  }



  public void setPageSize(Integer pageSize) { 
    this.pageSize = pageSize;
  }

  public PaginationResponse totalItems(Integer totalItems) { 

    this.totalItems = totalItems;
    return this;
  }

  /**
   * The total number of items.
   * @return totalItems
   **/
  
  @Schema(description = "The total number of items.")
  
  public Integer getTotalItems() {  
    return totalItems;
  }



  public void setTotalItems(Integer totalItems) { 
    this.totalItems = totalItems;
  }

  public PaginationResponse totalPages(Integer totalPages) { 

    this.totalPages = totalPages;
    return this;
  }

  /**
   * The total number of pages.
   * @return totalPages
   **/
  
  @Schema(description = "The total number of pages.")
  
  public Integer getTotalPages() {  
    return totalPages;
  }



  public void setTotalPages(Integer totalPages) { 
    this.totalPages = totalPages;
  }

  public PaginationResponse pageNumber(Integer pageNumber) { 

    this.pageNumber = pageNumber;
    return this;
  }

  /**
   * page number of the current page.
   * @return pageNumber
   **/
  
  @Schema(description = "page number of the current page.")
  
  public Integer getPageNumber() {  
    return pageNumber;
  }



  public void setPageNumber(Integer pageNumber) { 
    this.pageNumber = pageNumber;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaginationResponse paginationResponse = (PaginationResponse) o;
    return Objects.equals(this.pageSize, paginationResponse.pageSize) &&
        Objects.equals(this.totalItems, paginationResponse.totalItems) &&
        Objects.equals(this.totalPages, paginationResponse.totalPages) &&
        Objects.equals(this.pageNumber, paginationResponse.pageNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageSize, totalItems, totalPages, pageNumber);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaginationResponse {\n");
    
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    totalItems: ").append(toIndentedString(totalItems)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
    sb.append("    pageNumber: ").append(toIndentedString(pageNumber)).append("\n");
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
