package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * LinksPagination
 */

@JsonTypeName("Links_pagination")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class LinksPagination {

  private String href;

  private String type;

  /**
   * Link relation type: - self: Current page - next: Next page - prev: Previous page 
   */
  public enum RelEnum {
    SELF("self"),
    
    NEXT("next"),
    
    PREV("prev");

    private String value;

    RelEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static RelEnum fromValue(String value) {
      for (RelEnum b : RelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private RelEnum rel;

  private String title;

  @Valid
  private List<String> responseTypes;

  public LinksPagination href(String href) {
    this.href = href;
    return this;
  }

  /**
   * URL for the paginated document list. For next/prev links, includes only pageNumber and pageSize parameters. The original search criteria (filters, sorting) are maintained by the server. 
   * @return href
  */
  
  @Schema(name = "href", example = "{\"self\":\"/documents-enquiry?pageNumber=2&pageSize=10\",\"next\":\"/documents-enquiry?pageNumber=3&pageSize=10\",\"prev\":\"/documents-enquiry?pageNumber=1&pageSize=10\"}", description = "URL for the paginated document list. For next/prev links, includes only pageNumber and pageSize parameters. The original search criteria (filters, sorting) are maintained by the server. ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("href")
  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  public LinksPagination type(String type) {
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

  public LinksPagination rel(RelEnum rel) {
    this.rel = rel;
    return this;
  }

  /**
   * Link relation type: - self: Current page - next: Next page - prev: Previous page 
   * @return rel
  */
  
  @Schema(name = "rel", example = "{\"self\":\"self\",\"next\":\"next\",\"prev\":\"prev\"}", description = "Link relation type: - self: Current page - next: Next page - prev: Previous page ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("rel")
  public RelEnum getRel() {
    return rel;
  }

  public void setRel(RelEnum rel) {
    this.rel = rel;
  }

  public LinksPagination title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Human-readable title for the link. - self: \"Current page of documents\" - next: \"Next page of documents\" - prev: \"Previous page of documents\" 
   * @return title
  */
  
  @Schema(name = "title", example = "{\"self\":\"Current page of documents\",\"next\":\"Next page of documents\",\"prev\":\"Previous page of documents\"}", description = "Human-readable title for the link. - self: \"Current page of documents\" - next: \"Next page of documents\" - prev: \"Previous page of documents\" ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LinksPagination responseTypes(List<String> responseTypes) {
    this.responseTypes = responseTypes;
    return this;
  }

  public LinksPagination addResponseTypesItem(String responseTypesItem) {
    if (this.responseTypes == null) {
      this.responseTypes = new ArrayList<>();
    }
    this.responseTypes.add(responseTypesItem);
    return this;
  }

  /**
   * Supported response content types for this link. Always includes application/json for pagination links. 
   * @return responseTypes
  */
  
  @Schema(name = "responseTypes", example = "[\"application/json\"]", description = "Supported response content types for this link. Always includes application/json for pagination links. ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    LinksPagination linksPagination = (LinksPagination) o;
    return Objects.equals(this.href, linksPagination.href) &&
        Objects.equals(this.type, linksPagination.type) &&
        Objects.equals(this.rel, linksPagination.rel) &&
        Objects.equals(this.title, linksPagination.title) &&
        Objects.equals(this.responseTypes, linksPagination.responseTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(href, type, rel, title, responseTypes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LinksPagination {\n");
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

