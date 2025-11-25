package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * LinksDownload
 */

@JsonTypeName("Links_download")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class LinksDownload {

  private String href;

  private String type;

  private String rel;

  private String title;

  @Valid
  private List<String> responseTypes;

  public LinksDownload href(String href) {
    this.href = href;
    return this;
  }

  /**
   * Returns the binary content of the document. Client must set Accept: application/pdf or application/octet-stream to trigger file download. 
   * @return href
  */
  
  @Schema(name = "href", example = "/documents/eyJ21pbWFnZVBh2dGgiOi4IiL", description = "Returns the binary content of the document. Client must set Accept: application/pdf or application/octet-stream to trigger file download. ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("href")
  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public LinksDownload type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  
  @Schema(name = "type", example = "GET", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LinksDownload rel(String rel) {
    this.rel = rel;
    return this;
  }

  /**
   * Get rel
   * @return rel
  */
  
  @Schema(name = "rel", example = "download", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("rel")
  public String getRel() {
    return rel;
  }

  public void setRel(String rel) {
    this.rel = rel;
  }

  public LinksDownload title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
  */
  
  @Schema(name = "title", example = "Download this document", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LinksDownload responseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
    return this;
  }

  public LinksDownload addResponseTypesItem(String responseTypesItem) {
    if (this.responseTypes == null) {
      this.responseTypes = new ArrayList<>();
    }
    this.responseTypes.add(responseTypesItem);
    return this;
  }

  /**
   * Get responseTypes
   * @return responseTypes
  */
  
  @Schema(name = "responseTypes", example = "[\"application/pdf\",\"application/octet-stream\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("responseTypes")
  public List<String> getResponseTypes() {
    return responseTypes;
  }

  public void setResponseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LinksDownload linksDownload = (LinksDownload) o;
    return Objects.equals(this.href, linksDownload.href) &&
        Objects.equals(this.type, linksDownload.type) &&
        Objects.equals(this.rel, linksDownload.rel) &&
        Objects.equals(this.title, linksDownload.title) &&
        Objects.equals(this.responseTypes, linksDownload.responseTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(href, type, rel, title, responseTypes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LinksDownload {\n");
    sb.append("    href: ").append(toIndentedString(href)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    rel: ").append(toIndentedString(rel)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    responseTypes: ").append(toIndentedString(responseTypes)).append("\n");
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

