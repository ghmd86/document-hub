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
 * LinksDownload
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class LinksDownload   {
  @JsonProperty("href")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String href = null;

  @JsonProperty("type")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String type = null;

  @JsonProperty("rel")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String rel = null;

  @JsonProperty("title")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String title = null;

  @JsonProperty("responseTypes")
  @Valid
  private List<String> responseTypes = null;

  public LinksDownload href(String href) { 

    this.href = href;
    return this;
  }

  /**
   * Returns the binary content of the document. Client must set Accept: application/pdf or application/octet-stream to trigger file download. 
   * @return href
   **/
  
  @Schema(example = "/documents/eyJ21pbWFnZVBh2dGgiOi4IiL", description = "Returns the binary content of the document. Client must set Accept: application/pdf or application/octet-stream to trigger file download. ")
  
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
   **/
  
  @Schema(example = "GET", description = "")
  
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
   **/
  
  @Schema(example = "download", description = "")
  
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
   **/
  
  @Schema(example = "Download this document", description = "")
  
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
      this.responseTypes = new ArrayList<String>();
    }
    this.responseTypes.add(responseTypesItem);
    return this;
  }

  /**
   * Get responseTypes
   * @return responseTypes
   **/
  
  @Schema(example = "[\"application/pdf\",\"application/octet-stream\"]", description = "")
  
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
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
