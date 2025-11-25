package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.LinksDelete;
import io.swagger.model.LinksDownload;
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
 * Links
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class Links   {
  @JsonProperty("download")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private LinksDownload download = null;

  @JsonProperty("delete")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private LinksDelete delete = null;


  public Links download(LinksDownload download) { 

    this.download = download;
    return this;
  }

  /**
   * Get download
   * @return download
   **/
  
  @Schema(description = "")
  
@Valid
  public LinksDownload getDownload() {  
    return download;
  }



  public void setDownload(LinksDownload download) { 
    this.download = download;
  }

  public Links delete(LinksDelete delete) { 

    this.delete = delete;
    return this;
  }

  /**
   * Get delete
   * @return delete
   **/
  
  @Schema(description = "")
  
@Valid
  public LinksDelete getDelete() {  
    return delete;
  }



  public void setDelete(LinksDelete delete) { 
    this.delete = delete;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Links links = (Links) o;
    return Objects.equals(this.download, links.download) &&
        Objects.equals(this.delete, links.delete);
  }

  @Override
  public int hashCode() {
    return Objects.hash(download, delete);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Links {\n");
    
    sb.append("    download: ").append(toIndentedString(download)).append("\n");
    sb.append("    delete: ").append(toIndentedString(delete)).append("\n");
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
