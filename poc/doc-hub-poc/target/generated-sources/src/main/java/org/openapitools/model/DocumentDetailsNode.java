package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.LanguageCode;
import org.openapitools.model.Links;
import org.openapitools.model.MetadataNode;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * DocumentDetailsNode
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class DocumentDetailsNode {

  private String documentId;

  private Long sizeInMb;

  private LanguageCode languageCode;

  private String displayName;

  private String mimeType;

  private String description;

  private String lineOfBusiness;

  private String category;

  private String documentType;

  private Long datePosted;

  private Long lastDownloaded;

  private UUID lastClientDownload;

  private Links links;

  @Valid
  private List<@Valid MetadataNode> metadata;

  public DocumentDetailsNode documentId(String documentId) {
    this.documentId = documentId;
    return this;
  }

  /**
   * Get documentId
   * @return documentId
  */
  
  @Schema(name = "documentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("documentId")
  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public DocumentDetailsNode sizeInMb(Long sizeInMb) {
    this.sizeInMb = sizeInMb;
    return this;
  }

  /**
   * Get sizeInMb
   * @return sizeInMb
  */
  
  @Schema(name = "sizeInMb", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sizeInMb")
  public Long getSizeInMb() {
    return sizeInMb;
  }

  public void setSizeInMb(Long sizeInMb) {
    this.sizeInMb = sizeInMb;
  }

  public DocumentDetailsNode languageCode(LanguageCode languageCode) {
    this.languageCode = languageCode;
    return this;
  }

  /**
   * Get languageCode
   * @return languageCode
  */
  @Valid 
  @Schema(name = "languageCode", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("languageCode")
  public LanguageCode getLanguageCode() {
    return languageCode;
  }

  public void setLanguageCode(LanguageCode languageCode) {
    this.languageCode = languageCode;
  }

  public DocumentDetailsNode displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Get displayName
   * @return displayName
  */
  
  @Schema(name = "displayName", example = "2024_PRIVACY_POLICY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public DocumentDetailsNode mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  /**
   * Get mimeType
   * @return mimeType
  */
  
  @Schema(name = "mimeType", example = "application/pdf", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("mimeType")
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public DocumentDetailsNode description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  
  @Schema(name = "description", example = "2024_PRIVACY_POLICY", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DocumentDetailsNode lineOfBusiness(String lineOfBusiness) {
    this.lineOfBusiness = lineOfBusiness;
    return this;
  }

  /**
   * Get lineOfBusiness
   * @return lineOfBusiness
  */
  
  @Schema(name = "lineOfBusiness", example = "CREDIT_CARD", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lineOfBusiness")
  public String getLineOfBusiness() {
    return lineOfBusiness;
  }

  public void setLineOfBusiness(String lineOfBusiness) {
    this.lineOfBusiness = lineOfBusiness;
  }

  public DocumentDetailsNode category(String category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
  */
  
  @Schema(name = "category", example = "PaymentConfirmationNotice", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public DocumentDetailsNode documentType(String documentType) {
    this.documentType = documentType;
    return this;
  }

  /**
   * Get documentType
   * @return documentType
  */
  
  @Schema(name = "documentType", example = "PaymentLetter", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("documentType")
  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public DocumentDetailsNode datePosted(Long datePosted) {
    this.datePosted = datePosted;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return datePosted
  */
  
  @Schema(name = "datePosted", example = "1740523843", description = "Epoch time in seconds ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("datePosted")
  public Long getDatePosted() {
    return datePosted;
  }

  public void setDatePosted(Long datePosted) {
    this.datePosted = datePosted;
  }

  public DocumentDetailsNode lastDownloaded(Long lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return lastDownloaded
  */
  
  @Schema(name = "lastDownloaded", example = "1740523843", description = "Epoch time in seconds ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastDownloaded")
  public Long getLastDownloaded() {
    return lastDownloaded;
  }

  public void setLastDownloaded(Long lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
  }

  public DocumentDetailsNode lastClientDownload(UUID lastClientDownload) {
    this.lastClientDownload = lastClientDownload;
    return this;
  }

  /**
   * Get lastClientDownload
   * @return lastClientDownload
  */
  @Valid 
  @Schema(name = "lastClientDownload", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastClientDownload")
  public UUID getLastClientDownload() {
    return lastClientDownload;
  }

  public void setLastClientDownload(UUID lastClientDownload) {
    this.lastClientDownload = lastClientDownload;
  }

  public DocumentDetailsNode links(Links links) {
    this.links = links;
    return this;
  }

  /**
   * Get links
   * @return links
  */
  @Valid 
  @Schema(name = "_links", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("_links")
  public Links getLinks() {
    return links;
  }

  public void setLinks(Links links) {
    this.links = links;
  }

  public DocumentDetailsNode metadata(List<@Valid MetadataNode> metadata) {
    this.metadata = metadata;
    return this;
  }

  public DocumentDetailsNode addMetadataItem(MetadataNode metadataItem) {
    if (this.metadata == null) {
      this.metadata = new ArrayList<>();
    }
    this.metadata.add(metadataItem);
    return this;
  }

  /**
   * Each document type has its own set of required metadata so it can be properly indexed. Required fields and expected format can be retrieved using GET /templates.
   * @return metadata
  */
  @Valid 
  @Schema(name = "metadata", example = "[{\"key\":\"accountId\",\"value\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"},{\"key\":\"issueDate\",\"value\":\"2025-01-01\"},{\"key\":\"customerId\",\"value\":\"3fa85f64-5717-4562-b3fc-2c963f66a78\"},{\"key\":\"htmlLettername\",\"value\":\"FF083\"}]", description = "Each document type has its own set of required metadata so it can be properly indexed. Required fields and expected format can be retrieved using GET /templates.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public List<@Valid MetadataNode> getMetadata() {
    return metadata;
  }

  public void setMetadata(List<@Valid MetadataNode> metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocumentDetailsNode documentDetailsNode = (DocumentDetailsNode) o;
    return Objects.equals(this.documentId, documentDetailsNode.documentId) &&
        Objects.equals(this.sizeInMb, documentDetailsNode.sizeInMb) &&
        Objects.equals(this.languageCode, documentDetailsNode.languageCode) &&
        Objects.equals(this.displayName, documentDetailsNode.displayName) &&
        Objects.equals(this.mimeType, documentDetailsNode.mimeType) &&
        Objects.equals(this.description, documentDetailsNode.description) &&
        Objects.equals(this.lineOfBusiness, documentDetailsNode.lineOfBusiness) &&
        Objects.equals(this.category, documentDetailsNode.category) &&
        Objects.equals(this.documentType, documentDetailsNode.documentType) &&
        Objects.equals(this.datePosted, documentDetailsNode.datePosted) &&
        Objects.equals(this.lastDownloaded, documentDetailsNode.lastDownloaded) &&
        Objects.equals(this.lastClientDownload, documentDetailsNode.lastClientDownload) &&
        Objects.equals(this.links, documentDetailsNode.links) &&
        Objects.equals(this.metadata, documentDetailsNode.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentId, sizeInMb, languageCode, displayName, mimeType, description, lineOfBusiness, category, documentType, datePosted, lastDownloaded, lastClientDownload, links, metadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentDetailsNode {\n");
    sb.append("    documentId: ").append(toIndentedString(documentId)).append("\n");
    sb.append("    sizeInMb: ").append(toIndentedString(sizeInMb)).append("\n");
    sb.append("    languageCode: ").append(toIndentedString(languageCode)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    lineOfBusiness: ").append(toIndentedString(lineOfBusiness)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    documentType: ").append(toIndentedString(documentType)).append("\n");
    sb.append("    datePosted: ").append(toIndentedString(datePosted)).append("\n");
    sb.append("    lastDownloaded: ").append(toIndentedString(lastDownloaded)).append("\n");
    sb.append("    lastClientDownload: ").append(toIndentedString(lastClientDownload)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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

