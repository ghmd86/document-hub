package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.model.LinksPagination;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * HATEOAS links for collection navigation. Client must resen dthe full original request with the new pageNumber and pageSize values. - self: Current page - next: Next page (if available) - prev: Previous page (if available) 
 */

@Schema(name = "DocumentRetrievalResponse__links", description = "HATEOAS links for collection navigation. Client must resen dthe full original request with the new pageNumber and pageSize values. - self: Current page - next: Next page (if available) - prev: Previous page (if available) ")
@JsonTypeName("DocumentRetrievalResponse__links")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class DocumentRetrievalResponseLinks {

  private LinksPagination self;

  private LinksPagination next;

  private LinksPagination prev;

  public DocumentRetrievalResponseLinks self(LinksPagination self) {
    this.self = self;
    return this;
  }

  /**
   * Get self
   * @return self
  */
  @Valid 
  @Schema(name = "self", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("self")
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
  */
  @Valid 
  @Schema(name = "next", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("next")
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
  */
  @Valid 
  @Schema(name = "prev", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("prev")
  public LinksPagination getPrev() {
    return prev;
  }

  public void setPrev(LinksPagination prev) {
    this.prev = prev;
  }

  @Override
  public boolean equals(Object o) {
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

