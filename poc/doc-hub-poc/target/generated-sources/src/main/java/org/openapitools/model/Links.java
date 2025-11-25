package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.model.LinksDelete;
import org.openapitools.model.LinksDownload;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * Links
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class Links {

  private LinksDownload download;

  private LinksDelete delete;

  public Links download(LinksDownload download) {
    this.download = download;
    return this;
  }

  /**
   * Get download
   * @return download
  */
  @Valid 
  @Schema(name = "download", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("download")
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
  */
  @Valid 
  @Schema(name = "delete", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("delete")
  public LinksDelete getDelete() {
    return delete;
  }

  public void setDelete(LinksDelete delete) {
    this.delete = delete;
  }

  @Override
  public boolean equals(Object o) {
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

