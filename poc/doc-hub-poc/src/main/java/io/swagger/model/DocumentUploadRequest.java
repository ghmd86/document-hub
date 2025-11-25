package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.Metadata;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DocumentUploadRequest
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class DocumentUploadRequest   {
  @JsonProperty("documentType")

  private String documentType = null;

  @JsonProperty("createdBy")

  private UUID createdBy = null;

  @JsonProperty("templateId")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID templateId = null;

  @JsonProperty("referenceKey")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String referenceKey = null;

  @JsonProperty("referenceKeyType")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String referenceKeyType = null;

  @JsonProperty("accountKey")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID accountKey = null;

  @JsonProperty("customerKey")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID customerKey = null;

  @JsonProperty("category")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String category = null;

  @JsonProperty("fileName")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String fileName = null;

  @JsonProperty("activeStartDate")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long activeStartDate = null;

  @JsonProperty("activeEndDate")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long activeEndDate = null;

  @JsonProperty("threadId")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID threadId = null;

  @JsonProperty("correlationId")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID correlationId = null;

  @JsonProperty("content")

  private Resource content = null;

  @JsonProperty("metadata")

  private Metadata metadata = null;


  public DocumentUploadRequest documentType(String documentType) { 

    this.documentType = documentType;
    return this;
  }

  /**
   * - \"PaymentLetter\" - \"StatementInsert\" - \"AdverseActionNotice\" - \"ChangeInTermsNotice\" - \"AnnualFeeNotice\" - \"PrivacyNotice\" - \"NachaLetter\" - \"DelinquencyNotice\" - \"EsignInformation\" - \"CustomerLetter\" - \"FraudLetter\" - \"Statement\" - \"ReturnedPaymentLetter\" - \"FreeCLILetter\" - \"PaidCLILetter\" - \"CardholderAgreement\" - \"CreditProtectionLetter\" - \"BalanceTransferLetter\" - \"MiscellaneousLetter\" - \"AutopayLetter\" 
   * @return documentType
   **/
  
  @Schema(example = "PaymentLetter", required = true, description = "- \"PaymentLetter\" - \"StatementInsert\" - \"AdverseActionNotice\" - \"ChangeInTermsNotice\" - \"AnnualFeeNotice\" - \"PrivacyNotice\" - \"NachaLetter\" - \"DelinquencyNotice\" - \"EsignInformation\" - \"CustomerLetter\" - \"FraudLetter\" - \"Statement\" - \"ReturnedPaymentLetter\" - \"FreeCLILetter\" - \"PaidCLILetter\" - \"CardholderAgreement\" - \"CreditProtectionLetter\" - \"BalanceTransferLetter\" - \"MiscellaneousLetter\" - \"AutopayLetter\" ")
  
  @NotNull
  public String getDocumentType() {  
    return documentType;
  }



  public void setDocumentType(String documentType) { 

    this.documentType = documentType;
  }

  public DocumentUploadRequest createdBy(UUID createdBy) { 

    this.createdBy = createdBy;
    return this;
  }

  /**
   * Identifies the user or system that created the document entry.
   * @return createdBy
   **/
  
  @Schema(required = true, description = "Identifies the user or system that created the document entry.")
  
@Valid
  @NotNull
  public UUID getCreatedBy() {  
    return createdBy;
  }



  public void setCreatedBy(UUID createdBy) { 

    this.createdBy = createdBy;
  }

  public DocumentUploadRequest templateId(UUID templateId) { 

    this.templateId = templateId;
    return this;
  }

  /**
   * Unique identifier referencing the document generation template used.
   * @return templateId
   **/
  
  @Schema(description = "Unique identifier referencing the document generation template used.")
  
@Valid
  public UUID getTemplateId() {  
    return templateId;
  }



  public void setTemplateId(UUID templateId) { 
    this.templateId = templateId;
  }

  public DocumentUploadRequest referenceKey(String referenceKey) { 

    this.referenceKey = referenceKey;
    return this;
  }

  /**
   * External identifier linking the document to a specific business entity or transaction.
   * @return referenceKey
   **/
  
  @Schema(description = "External identifier linking the document to a specific business entity or transaction.")
  
  public String getReferenceKey() {  
    return referenceKey;
  }



  public void setReferenceKey(String referenceKey) { 
    this.referenceKey = referenceKey;
  }

  public DocumentUploadRequest referenceKeyType(String referenceKeyType) { 

    this.referenceKeyType = referenceKeyType;
    return this;
  }

  /**
   * Specifies what the reference key represents (e.g., statement, customer, account).
   * @return referenceKeyType
   **/
  
  @Schema(example = "[\"STATEMENT_ID\",\"DISCLOSURE_CODE\",\"ACCOUNT_ID\",\"LETTER_NAME\"]", description = "Specifies what the reference key represents (e.g., statement, customer, account).")
  
  public String getReferenceKeyType() {  
    return referenceKeyType;
  }



  public void setReferenceKeyType(String referenceKeyType) { 
    this.referenceKeyType = referenceKeyType;
  }

  public DocumentUploadRequest accountKey(UUID accountKey) { 

    this.accountKey = accountKey;
    return this;
  }

  /**
   * Unique key representing the account associated with the document.
   * @return accountKey
   **/
  
  @Schema(description = "Unique key representing the account associated with the document.")
  
@Valid
  public UUID getAccountKey() {  
    return accountKey;
  }



  public void setAccountKey(UUID accountKey) { 
    this.accountKey = accountKey;
  }

  public DocumentUploadRequest customerKey(UUID customerKey) { 

    this.customerKey = customerKey;
    return this;
  }

  /**
   * Unique identifier for the customer related to the document.
   * @return customerKey
   **/
  
  @Schema(description = "Unique identifier for the customer related to the document.")
  
@Valid
  public UUID getCustomerKey() {  
    return customerKey;
  }



  public void setCustomerKey(UUID customerKey) { 
    this.customerKey = customerKey;
  }

  public DocumentUploadRequest category(String category) { 

    this.category = category;
    return this;
  }

  /**
   * Defines the grouping or classification of the document for organizational purposes.
   * @return category
   **/
  
  @Schema(description = "Defines the grouping or classification of the document for organizational purposes.")
  
  public String getCategory() {  
    return category;
  }



  public void setCategory(String category) { 
    this.category = category;
  }

  public DocumentUploadRequest fileName(String fileName) { 

    this.fileName = fileName;
    return this;
  }

  /**
   * Name of the document file stored in the system.
   * @return fileName
   **/
  
  @Schema(description = "Name of the document file stored in the system.")
  
  public String getFileName() {  
    return fileName;
  }



  public void setFileName(String fileName) { 
    this.fileName = fileName;
  }

  public DocumentUploadRequest activeStartDate(Long activeStartDate) { 

    this.activeStartDate = activeStartDate;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return activeStartDate
   **/
  
  @Schema(example = "1740523843", description = "Epoch time in seconds ")
  
  public Long getActiveStartDate() {  
    return activeStartDate;
  }



  public void setActiveStartDate(Long activeStartDate) { 
    this.activeStartDate = activeStartDate;
  }

  public DocumentUploadRequest activeEndDate(Long activeEndDate) { 

    this.activeEndDate = activeEndDate;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return activeEndDate
   **/
  
  @Schema(example = "1740523843", description = "Epoch time in seconds ")
  
  public Long getActiveEndDate() {  
    return activeEndDate;
  }



  public void setActiveEndDate(Long activeEndDate) { 
    this.activeEndDate = activeEndDate;
  }

  public DocumentUploadRequest threadId(UUID threadId) { 

    this.threadId = threadId;
    return this;
  }

  /**
   * Identifier used to trace or group related document processing threads or events.
   * @return threadId
   **/
  
  @Schema(description = "Identifier used to trace or group related document processing threads or events.")
  
@Valid
  public UUID getThreadId() {  
    return threadId;
  }



  public void setThreadId(UUID threadId) { 
    this.threadId = threadId;
  }

  public DocumentUploadRequest correlationId(UUID correlationId) { 

    this.correlationId = correlationId;
    return this;
  }

  /**
   * ID used to track and correlate document-related requests or operations across systems.
   * @return correlationId
   **/
  
  @Schema(description = "ID used to track and correlate document-related requests or operations across systems.")
  
@Valid
  public UUID getCorrelationId() {  
    return correlationId;
  }



  public void setCorrelationId(UUID correlationId) { 
    this.correlationId = correlationId;
  }

  public DocumentUploadRequest content(Resource content) { 

    this.content = content;
    return this;
  }

  /**
   * The actual body or data of the document (e.g., PDF, text, or JSON).
   * @return content
   **/
  
  @Schema(required = true, description = "The actual body or data of the document (e.g., PDF, text, or JSON).")
  
@Valid
  @NotNull
  public Resource getContent() {  
    return content;
  }



  public void setContent(Resource content) { 

    this.content = content;
  }

  public DocumentUploadRequest metadata(Metadata metadata) { 

    this.metadata = metadata;
    return this;
  }

  /**
   * Get metadata
   * @return metadata
   **/
  
  @Schema(required = true, description = "")
  
@Valid
  @NotNull
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
    DocumentUploadRequest documentUploadRequest = (DocumentUploadRequest) o;
    return Objects.equals(this.documentType, documentUploadRequest.documentType) &&
        Objects.equals(this.createdBy, documentUploadRequest.createdBy) &&
        Objects.equals(this.templateId, documentUploadRequest.templateId) &&
        Objects.equals(this.referenceKey, documentUploadRequest.referenceKey) &&
        Objects.equals(this.referenceKeyType, documentUploadRequest.referenceKeyType) &&
        Objects.equals(this.accountKey, documentUploadRequest.accountKey) &&
        Objects.equals(this.customerKey, documentUploadRequest.customerKey) &&
        Objects.equals(this.category, documentUploadRequest.category) &&
        Objects.equals(this.fileName, documentUploadRequest.fileName) &&
        Objects.equals(this.activeStartDate, documentUploadRequest.activeStartDate) &&
        Objects.equals(this.activeEndDate, documentUploadRequest.activeEndDate) &&
        Objects.equals(this.threadId, documentUploadRequest.threadId) &&
        Objects.equals(this.correlationId, documentUploadRequest.correlationId) &&
        Objects.equals(this.content, documentUploadRequest.content) &&
        Objects.equals(this.metadata, documentUploadRequest.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentType, createdBy, templateId, referenceKey, referenceKeyType, accountKey, customerKey, category, fileName, activeStartDate, activeEndDate, threadId, correlationId, content, metadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentUploadRequest {\n");
    
    sb.append("    documentType: ").append(toIndentedString(documentType)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    referenceKey: ").append(toIndentedString(referenceKey)).append("\n");
    sb.append("    referenceKeyType: ").append(toIndentedString(referenceKeyType)).append("\n");
    sb.append("    accountKey: ").append(toIndentedString(accountKey)).append("\n");
    sb.append("    customerKey: ").append(toIndentedString(customerKey)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    activeStartDate: ").append(toIndentedString(activeStartDate)).append("\n");
    sb.append("    activeEndDate: ").append(toIndentedString(activeEndDate)).append("\n");
    sb.append("    threadId: ").append(toIndentedString(threadId)).append("\n");
    sb.append("    correlationId: ").append(toIndentedString(correlationId)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
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
