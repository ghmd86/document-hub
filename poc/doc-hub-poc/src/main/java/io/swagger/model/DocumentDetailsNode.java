package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;

import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import javax.validation.Valid;

/**
 * DocumentDetailsNode
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class DocumentDetailsNode   {
  @JsonProperty("documentId")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String documentId = null;

  @JsonProperty("sizeInMb")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long sizeInMb = null;

  @JsonProperty("languageCode")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private LanguageCode languageCode = null;

  @JsonProperty("displayName")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String displayName = null;

  @JsonProperty("mimeType")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String mimeType = null;

  @JsonProperty("description")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String description = null;

  @JsonProperty("lineOfBusiness")
  @Valid
  private String lineOfBusiness = null;
  @JsonProperty("category")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String category = null;

  @JsonProperty("documentType")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String documentType = null;

  @JsonProperty("datePosted")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long datePosted = null;

  @JsonProperty("lastDownloaded")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long lastDownloaded = null;

  @JsonProperty("lastClientDownload")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID lastClientDownload = null;

  @JsonProperty("_links")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Links _links = null;

  @JsonProperty("metadata")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Metadata metadata = null;


  public DocumentDetailsNode documentId(String documentId) { 

    this.documentId = documentId;
    return this;
  }

  /**
   * Get documentId
   * @return documentId
   **/
  
  @Schema(description = "")
  
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
   **/
  
  @Schema(example = "1", description = "")
  
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
   **/
  
  @Schema(description = "")
  
@Valid
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
   **/
  
  @Schema(example = "2024_PRIVACY_POLICY", description = "")
  
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
   **/
  
  @Schema(example = "application/pdf", description = "")
  
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
   **/
  
  @Schema(example = "2024_PRIVACY_POLICY", description = "")
  
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
   **/
  
  @Schema(example = "CREDIT_CARD", description = "")
  
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
   **/
  
  @Schema(example = "PaymentConfirmationNotice", description = "")
  
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
   **/
  
  @Schema(example = "PaymentLetter", description = "")
  
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
   **/
  
  @Schema(example = "1740523843", description = "Epoch time in seconds ")
  
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
   **/
  
  @Schema(example = "1740523843", description = "Epoch time in seconds ")
  
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
   **/
  
  @Schema(description = "")
  
@Valid
  public UUID getLastClientDownload() {  
    return lastClientDownload;
  }



  public void setLastClientDownload(UUID lastClientDownload) { 
    this.lastClientDownload = lastClientDownload;
  }

  public DocumentDetailsNode _links(Links _links) { 

    this._links = _links;
    return this;
  }

  /**
   * Get _links
   * @return _links
   **/
  
  @Schema(description = "")
  
@Valid
  public Links getLinks() {  
    return _links;
  }



  public void setLinks(Links _links) { 
    this._links = _links;
  }

  public DocumentDetailsNode metadata(Metadata metadata) { 

    this.metadata = metadata;
    return this;
  }

  /**
   * Get metadata
   * @return metadata
   **/
  
  @Schema(description = "")
  
@Valid
  public Metadata getMetadata() {  
    return metadata;
  }



  public void setMetadata(Metadata metadata) { 
    this.metadata = metadata;
  }

  @Override
  public boolean equals(java.lang.Object o) {
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
        Objects.equals(this._links, documentDetailsNode._links) &&
        Objects.equals(this.metadata, documentDetailsNode.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentId, sizeInMb, languageCode, displayName, mimeType, description, lineOfBusiness, category, documentType, datePosted, lastDownloaded, lastClientDownload, _links, metadata);
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
    sb.append("    _links: ").append(toIndentedString(_links)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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
