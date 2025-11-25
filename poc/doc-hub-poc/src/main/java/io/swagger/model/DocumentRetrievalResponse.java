package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.DocumentDetailsNode;
import io.swagger.model.DocumentRetrievalResponseLinks;
import io.swagger.model.PaginationResponse;
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
 * DocumentRetrievalResponse
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class DocumentRetrievalResponse   {
  @JsonProperty("documentList")
  @Valid
  private List<DocumentDetailsNode> documentList = new ArrayList<DocumentDetailsNode>();
  @JsonProperty("pagination")

  private PaginationResponse pagination = null;

  @JsonProperty("_links")

  private DocumentRetrievalResponseLinks _links = null;


  public DocumentRetrievalResponse documentList(List<DocumentDetailsNode> documentList) { 

    this.documentList = documentList;
    return this;
  }

  public DocumentRetrievalResponse addDocumentListItem(DocumentDetailsNode documentListItem) {
    this.documentList.add(documentListItem);
    return this;
  }

  /**
   * Get documentList
   * @return documentList
   **/
  
  @Schema(required = true, description = "")
  
@Valid
  @NotNull
  public List<DocumentDetailsNode> getDocumentList() {  
    return documentList;
  }



  public void setDocumentList(List<DocumentDetailsNode> documentList) { 

    this.documentList = documentList;
  }

  public DocumentRetrievalResponse pagination(PaginationResponse pagination) { 

    this.pagination = pagination;
    return this;
  }

  /**
   * Get pagination
   * @return pagination
   **/
  
  @Schema(required = true, description = "")
  
@Valid
  @NotNull
  public PaginationResponse getPagination() {  
    return pagination;
  }



  public void setPagination(PaginationResponse pagination) { 

    this.pagination = pagination;
  }

  public DocumentRetrievalResponse _links(DocumentRetrievalResponseLinks _links) { 

    this._links = _links;
    return this;
  }

  /**
   * Get _links
   * @return _links
   **/
  
  @Schema(required = true, description = "")
  
@Valid
  @NotNull
  public DocumentRetrievalResponseLinks getLinks() {  
    return _links;
  }



  public void setLinks(DocumentRetrievalResponseLinks _links) { 

    this._links = _links;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocumentRetrievalResponse documentRetrievalResponse = (DocumentRetrievalResponse) o;
    return Objects.equals(this.documentList, documentRetrievalResponse.documentList) &&
        Objects.equals(this.pagination, documentRetrievalResponse.pagination) &&
        Objects.equals(this._links, documentRetrievalResponse._links);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentList, pagination, _links);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentRetrievalResponse {\n");
    
    sb.append("    documentList: ").append(toIndentedString(documentList)).append("\n");
    sb.append("    pagination: ").append(toIndentedString(pagination)).append("\n");
    sb.append("    _links: ").append(toIndentedString(_links)).append("\n");
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
