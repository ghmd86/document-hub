package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.model.DocumentDetailsNode;
import org.openapitools.model.DocumentRetrievalResponseLinks;
import org.openapitools.model.PaginationResponse;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * DocumentRetrievalResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class DocumentRetrievalResponse {

  @Valid
  private List<@Valid DocumentDetailsNode> documentList = new ArrayList<>();

  private PaginationResponse pagination;

  private DocumentRetrievalResponseLinks links;

  /**
   * Default constructor
   * @deprecated Use {@link DocumentRetrievalResponse#DocumentRetrievalResponse(List<@Valid DocumentDetailsNode>, PaginationResponse, DocumentRetrievalResponseLinks)}
   */
  @Deprecated
  public DocumentRetrievalResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DocumentRetrievalResponse(List<@Valid DocumentDetailsNode> documentList, PaginationResponse pagination, DocumentRetrievalResponseLinks links) {
    this.documentList = documentList;
    this.pagination = pagination;
    this.links = links;
  }

  public DocumentRetrievalResponse documentList(List<@Valid DocumentDetailsNode> documentList) {
    this.documentList = documentList;
    return this;
  }

  public DocumentRetrievalResponse addDocumentListItem(DocumentDetailsNode documentListItem) {
    if (this.documentList == null) {
      this.documentList = new ArrayList<>();
    }
    this.documentList.add(documentListItem);
    return this;
  }

  /**
   * Get documentList
   * @return documentList
  */
  @NotNull @Valid 
  @Schema(name = "documentList", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("documentList")
  public List<@Valid DocumentDetailsNode> getDocumentList() {
    return documentList;
  }

  public void setDocumentList(List<@Valid DocumentDetailsNode> documentList) {
    this.documentList = documentList;
  }

  public DocumentRetrievalResponse pagination(PaginationResponse pagination) {
    this.pagination = pagination;
    return this;
  }

  /**
   * Get pagination
   * @return pagination
  */
  @NotNull @Valid 
  @Schema(name = "pagination", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("pagination")
  public PaginationResponse getPagination() {
    return pagination;
  }

  public void setPagination(PaginationResponse pagination) {
    this.pagination = pagination;
  }

  public DocumentRetrievalResponse links(DocumentRetrievalResponseLinks links) {
    this.links = links;
    return this;
  }

  /**
   * Get links
   * @return links
  */
  @NotNull @Valid 
  @Schema(name = "_links", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("_links")
  public DocumentRetrievalResponseLinks getLinks() {
    return links;
  }

  public void setLinks(DocumentRetrievalResponseLinks links) {
    this.links = links;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocumentRetrievalResponse documentRetrievalResponse = (DocumentRetrievalResponse) o;
    return Objects.equals(this.documentList, documentRetrievalResponse.documentList) &&
        Objects.equals(this.pagination, documentRetrievalResponse.pagination) &&
        Objects.equals(this.links, documentRetrievalResponse.links);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentList, pagination, links);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentRetrievalResponse {\n");
    sb.append("    documentList: ").append(toIndentedString(documentList)).append("\n");
    sb.append("    pagination: ").append(toIndentedString(pagination)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
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

