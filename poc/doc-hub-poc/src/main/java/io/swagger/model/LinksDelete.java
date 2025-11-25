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
 * LinksDelete
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class LinksDelete   {
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


  public LinksDelete href(String href) { 

    this.href = href;
    return this;
  }

  /**
   * Deletes the document given by the documentId 
   * @return href
   **/
  
  @Schema(example = "/documents/eyJ21pbWFnZVBh2dGgiOi4IiL", description = "Deletes the document given by the documentId ")
  
  public String getHref() {  
    return href;
  }



  public void setHref(String href) { 
    this.href = href;
  }

  public LinksDelete type(String type) { 

    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   **/
  
  @Schema(example = "DELETE", description = "")
  
  public String getType() {  
    return type;
  }



  public void setType(String type) { 
    this.type = type;
  }

  public LinksDelete rel(String rel) { 

    this.rel = rel;
    return this;
  }

  /**
   * Get rel
   * @return rel
   **/
  
  @Schema(example = "delete", description = "")
  
  public String getRel() {  
    return rel;
  }



  public void setRel(String rel) { 
    this.rel = rel;
  }

  public LinksDelete title(String title) { 

    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   **/
  
  @Schema(example = "Delete this document", description = "")
  
  public String getTitle() {  
    return title;
  }



  public void setTitle(String title) { 
    this.title = title;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LinksDelete linksDelete = (LinksDelete) o;
    return Objects.equals(this.href, linksDelete.href) &&
        Objects.equals(this.type, linksDelete.type) &&
        Objects.equals(this.rel, linksDelete.rel) &&
        Objects.equals(this.title, linksDelete.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(href, type, rel, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LinksDelete {\n");
    
    sb.append("    href: ").append(toIndentedString(href)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    rel: ").append(toIndentedString(rel)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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
