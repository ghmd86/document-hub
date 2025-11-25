package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.model.DocumentCategoryGroup;
import io.swagger.model.SortOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DocumentListRequest
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class DocumentListRequest   {
  @JsonProperty("customerId")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private UUID customerId = null;

  @JsonProperty("accountId")
  @Valid
  private List<String> accountId = null;
  @JsonProperty("referenceKey")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String referenceKey = null;

  @JsonProperty("referenceKeyType")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String referenceKeyType = null;

  @JsonProperty("documentTypeCategoryGroup")
  @Valid
  private List<DocumentCategoryGroup> documentTypeCategoryGroup = null;
  @JsonProperty("postedFromDate")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long postedFromDate = null;

  @JsonProperty("postedToDate")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private Long postedToDate = null;

  @JsonProperty("pageNumber")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private BigDecimal pageNumber = null;

  @JsonProperty("pageSize")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private BigDecimal pageSize = null;

  @JsonProperty("sortOrder")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private SortOrder sortOrder = null;


  public DocumentListRequest customerId(UUID customerId) { 

    this.customerId = customerId;
    return this;
  }

  /**
   * Filter documents by customer ID. Combined with AND logic if accountId is provided.
   * @return customerId
   **/
  
  @Schema(description = "Filter documents by customer ID. Combined with AND logic if accountId is provided.")
  
@Valid
  public UUID getCustomerId() {  
    return customerId;
  }



  public void setCustomerId(UUID customerId) { 
    this.customerId = customerId;
  }

  public DocumentListRequest accountId(List<String> accountId) { 

    this.accountId = accountId;
    return this;
  }

  public DocumentListRequest addAccountIdItem(String accountIdItem) {
    if (this.accountId == null) {
      this.accountId = new ArrayList<String>();
    }
    this.accountId.add(accountIdItem);
    return this;
  }

  /**
   * Filter documents by account ID.  Combined with AND logic if customerId is provided. If the accountId is not provided, documents which belongs to all the accounts of the given customerId will be retrieved.
   * @return accountId
   **/
  
  @Schema(example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"]", description = "Filter documents by account ID.  Combined with AND logic if customerId is provided. If the accountId is not provided, documents which belongs to all the accounts of the given customerId will be retrieved.")
  
  public List<String> getAccountId() {  
    return accountId;
  }



  public void setAccountId(List<String> accountId) { 
    this.accountId = accountId;
  }

  public DocumentListRequest referenceKey(String referenceKey) { 

    this.referenceKey = referenceKey;
    return this;
  }

  /**
   * Filter documents based on reference key associated with the document
   * @return referenceKey
   **/
  
  @Schema(example = "D164", description = "Filter documents based on reference key associated with the document")
  
  public String getReferenceKey() {  
    return referenceKey;
  }



  public void setReferenceKey(String referenceKey) { 
    this.referenceKey = referenceKey;
  }

  public DocumentListRequest referenceKeyType(String referenceKeyType) { 

    this.referenceKeyType = referenceKeyType;
    return this;
  }

  /**
   * Indicates the type of reference key being used
   * @return referenceKeyType
   **/
  
  @Schema(example = "Disclosure_Code", description = "Indicates the type of reference key being used")
  
  public String getReferenceKeyType() {  
    return referenceKeyType;
  }



  public void setReferenceKeyType(String referenceKeyType) { 
    this.referenceKeyType = referenceKeyType;
  }

  public DocumentListRequest documentTypeCategoryGroup(List<DocumentCategoryGroup> documentTypeCategoryGroup) { 

    this.documentTypeCategoryGroup = documentTypeCategoryGroup;
    return this;
  }

  public DocumentListRequest addDocumentTypeCategoryGroupItem(DocumentCategoryGroup documentTypeCategoryGroupItem) {
    if (this.documentTypeCategoryGroup == null) {
      this.documentTypeCategoryGroup = new ArrayList<DocumentCategoryGroup>();
    }
    this.documentTypeCategoryGroup.add(documentTypeCategoryGroupItem);
    return this;
  }

  /**
   * Get documentTypeCategoryGroup
   * @return documentTypeCategoryGroup
   **/
  
  @Schema(description = "")
  @Valid
  public List<DocumentCategoryGroup> getDocumentTypeCategoryGroup() {  
    return documentTypeCategoryGroup;
  }



  public void setDocumentTypeCategoryGroup(List<DocumentCategoryGroup> documentTypeCategoryGroup) { 
    this.documentTypeCategoryGroup = documentTypeCategoryGroup;
  }

  public DocumentListRequest postedFromDate(Long postedFromDate) { 

    this.postedFromDate = postedFromDate;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return postedFromDate
   **/
  
  @Schema(example = "1740523843", description = "Epoch time in seconds ")
  
  public Long getPostedFromDate() {  
    return postedFromDate;
  }



  public void setPostedFromDate(Long postedFromDate) { 
    this.postedFromDate = postedFromDate;
  }

  public DocumentListRequest postedToDate(Long postedToDate) { 

    this.postedToDate = postedToDate;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return postedToDate
   **/
  
  @Schema(example = "1740523843", description = "Epoch time in seconds ")
  
  public Long getPostedToDate() {  
    return postedToDate;
  }



  public void setPostedToDate(Long postedToDate) { 
    this.postedToDate = postedToDate;
  }

  public DocumentListRequest pageNumber(BigDecimal pageNumber) { 

    this.pageNumber = pageNumber;
    return this;
  }

  /**
   * Get pageNumber
   * @return pageNumber
   **/
  
  @Schema(description = "")
  
@Valid
  public BigDecimal getPageNumber() {  
    return pageNumber;
  }



  public void setPageNumber(BigDecimal pageNumber) { 
    this.pageNumber = pageNumber;
  }

  public DocumentListRequest pageSize(BigDecimal pageSize) { 

    this.pageSize = pageSize;
    return this;
  }

  /**
   * Get pageSize
   * @return pageSize
   **/
  
  @Schema(description = "")
  
@Valid
  public BigDecimal getPageSize() {  
    return pageSize;
  }



  public void setPageSize(BigDecimal pageSize) { 
    this.pageSize = pageSize;
  }

  public DocumentListRequest sortOrder(SortOrder sortOrder) { 

    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Get sortOrder
   * @return sortOrder
   **/
  
  @Schema(description = "")
  
@Valid
  public SortOrder getSortOrder() {  
    return sortOrder;
  }



  public void setSortOrder(SortOrder sortOrder) { 
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocumentListRequest documentListRequest = (DocumentListRequest) o;
    return Objects.equals(this.customerId, documentListRequest.customerId) &&
        Objects.equals(this.accountId, documentListRequest.accountId) &&
        Objects.equals(this.referenceKey, documentListRequest.referenceKey) &&
        Objects.equals(this.referenceKeyType, documentListRequest.referenceKeyType) &&
        Objects.equals(this.documentTypeCategoryGroup, documentListRequest.documentTypeCategoryGroup) &&
        Objects.equals(this.postedFromDate, documentListRequest.postedFromDate) &&
        Objects.equals(this.postedToDate, documentListRequest.postedToDate) &&
        Objects.equals(this.pageNumber, documentListRequest.pageNumber) &&
        Objects.equals(this.pageSize, documentListRequest.pageSize) &&
        Objects.equals(this.sortOrder, documentListRequest.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerId, accountId, referenceKey, referenceKeyType, documentTypeCategoryGroup, postedFromDate, postedToDate, pageNumber, pageSize, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentListRequest {\n");
    
    sb.append("    customerId: ").append(toIndentedString(customerId)).append("\n");
    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    referenceKey: ").append(toIndentedString(referenceKey)).append("\n");
    sb.append("    referenceKeyType: ").append(toIndentedString(referenceKeyType)).append("\n");
    sb.append("    documentTypeCategoryGroup: ").append(toIndentedString(documentTypeCategoryGroup)).append("\n");
    sb.append("    postedFromDate: ").append(toIndentedString(postedFromDate)).append("\n");
    sb.append("    postedToDate: ").append(toIndentedString(postedToDate)).append("\n");
    sb.append("    pageNumber: ").append(toIndentedString(pageNumber)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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
