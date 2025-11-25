package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * LinksPagination
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class LinksPagination   {
  @JsonProperty("href")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String href = null;

  @JsonProperty("type")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String type = null;

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

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static RelEnum fromValue(String text) {
      for (RelEnum b : RelEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
  @JsonProperty("rel")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private RelEnum rel = null;

  @JsonProperty("title")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String title = null;

  @JsonProperty("responseTypes")
  @Valid
  private List<String> responseTypes = null;

  public LinksPagination href(String href) { 

    this.href = href;
    return this;
  }

  /**
   * URL for the paginated document list. For next/prev links, includes only pageNumber and pageSize parameters. The original search criteria (filters, sorting) are maintained by the server. 
   * @return href
   **/
  
  @Schema(example = "{\"self\":\"/documents-enquiry?pageNumber=2&pageSize=10\",\"next\":\"/documents-enquiry?pageNumber=3&pageSize=10\",\"prev\":\"/documents-enquiry?pageNumber=1&pageSize=10\"}", description = "URL for the paginated document list. For next/prev links, includes only pageNumber and pageSize parameters. The original search criteria (filters, sorting) are maintained by the server. ")
  
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
   **/
  
  @Schema(example = "GET", description = "")
  
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
   **/
  
  @Schema(example = "{\"self\":\"self\",\"next\":\"next\",\"prev\":\"prev\"}", description = "Link relation type: - self: Current page - next: Next page - prev: Previous page ")
  
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
   **/
  
  @Schema(example = "{\"self\":\"Current page of documents\",\"next\":\"Next page of documents\",\"prev\":\"Previous page of documents\"}", description = "Human-readable title for the link. - self: \"Current page of documents\" - next: \"Next page of documents\" - prev: \"Previous page of documents\" ")
  
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
      this.responseTypes = new ArrayList<String>();
    }
    this.responseTypes.add(responseTypesItem);
    return this;
  }

  /**
   * Supported response content types for this link. Always includes application/json for pagination links. 
   * @return responseTypes
   **/
  
  @Schema(example = "[\"application/json\"]", description = "Supported response content types for this link. Always includes application/json for pagination links. ")
  
  public List<String> getResponseTypes() {  
    return responseTypes;
  }



  public void setResponseTypes(List<String> responseTypes) { 
    this.responseTypes = responseTypes;
  }

  @Override
  public boolean equals(java.lang.Object o) {
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
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
