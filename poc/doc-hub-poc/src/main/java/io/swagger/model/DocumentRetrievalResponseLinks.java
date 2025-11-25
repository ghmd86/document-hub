package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.LinksPagination;
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
 * HATEOAS links for collection navigation. Client must resen dthe full original request with the new pageNumber and pageSize values. - self: Current page - next: Next page (if available) - prev: Previous page (if available) 
 */
@Schema(description = "HATEOAS links for collection navigation. Client must resen dthe full original request with the new pageNumber and pageSize values. - self: Current page - next: Next page (if available) - prev: Previous page (if available) ")
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class DocumentRetrievalResponseLinks   {
  @JsonProperty("self")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private LinksPagination self = null;

  @JsonProperty("next")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private LinksPagination next = null;

  @JsonProperty("prev")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private LinksPagination prev = null;


  public DocumentRetrievalResponseLinks self(LinksPagination self) { 

    this.self = self;
    return this;
  }

  /**
   * Get self
   * @return self
   **/
  
  @Schema(description = "")
  
@Valid
  public LinksPagination getSelf() {  
    return self;
  }



  public void setSelf(LinksPagination self) { 
    this.self = self;
  }

  public DocumentRetrievalResponseLinks next(LinksPagination next) { 

    this.next = next;
    return this;
  }

  /**
   * Get next
   * @return next
   **/
  
  @Schema(description = "")
  
@Valid
  public LinksPagination getNext() {  
    return next;
  }



  public void setNext(LinksPagination next) { 
    this.next = next;
  }

  public DocumentRetrievalResponseLinks prev(LinksPagination prev) { 

    this.prev = prev;
    return this;
  }

  /**
   * Get prev
   * @return prev
   **/
  
  @Schema(description = "")
  
@Valid
  public LinksPagination getPrev() {  
    return prev;
  }



  public void setPrev(LinksPagination prev) { 
    this.prev = prev;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocumentRetrievalResponseLinks documentRetrievalResponseLinks = (DocumentRetrievalResponseLinks) o;
    return Objects.equals(this.self, documentRetrievalResponseLinks.self) &&
        Objects.equals(this.next, documentRetrievalResponseLinks.next) &&
        Objects.equals(this.prev, documentRetrievalResponseLinks.prev);
  }

  @Override
  public int hashCode() {
    return Objects.hash(self, next, prev);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentRetrievalResponseLinks {\n");
    
    sb.append("    self: ").append(toIndentedString(self)).append("\n");
    sb.append("    next: ").append(toIndentedString(next)).append("\n");
    sb.append("    prev: ").append(toIndentedString(prev)).append("\n");
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
