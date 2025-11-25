package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.DocumentCategoryGroup;
import org.openapitools.model.SortAndOrder;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * DocumentListRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class DocumentListRequest {

  private UUID customerId;

  @Valid
  private List<String> accountId;

  private String referenceKey;

  private String referenceKeyType;

  @Valid
  private List<@Valid DocumentCategoryGroup> documentTypeCategoryGroup;

  private Long postedFromDate;

  private Long postedToDate;

  private BigDecimal pageNumber;

  private BigDecimal pageSize;

  @Valid
  private List<@Valid SortAndOrder> sortOrder;

  public DocumentListRequest customerId(UUID customerId) {
    this.customerId = customerId;
    return this;
  }

  /**
   * Filter documents by customer ID. Combined with AND logic if accountId is provided.
   * @return customerId
  */
  @Valid 
  @Schema(name = "customerId", description = "Filter documents by customer ID. Combined with AND logic if accountId is provided.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("customerId")
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
      this.accountId = new ArrayList<>();
    }
    this.accountId.add(accountIdItem);
    return this;
  }

  /**
   * Filter documents by account ID.  Combined with AND logic if customerId is provided. If the accountId is not provided, documents which belongs to all the accounts of the given customerId will be retrieved.
   * @return accountId
  */
  
  @Schema(name = "accountId", example = "[\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"]", description = "Filter documents by account ID.  Combined with AND logic if customerId is provided. If the accountId is not provided, documents which belongs to all the accounts of the given customerId will be retrieved.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("accountId")
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
  */
  
  @Schema(name = "referenceKey", example = "D164", description = "Filter documents based on reference key associated with the document", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("referenceKey")
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
  */
  
  @Schema(name = "referenceKeyType", example = "Disclosure_Code", description = "Indicates the type of reference key being used", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("referenceKeyType")
  public String getReferenceKeyType() {
    return referenceKeyType;
  }

  public void setReferenceKeyType(String referenceKeyType) {
    this.referenceKeyType = referenceKeyType;
  }

  public DocumentListRequest documentTypeCategoryGroup(List<@Valid DocumentCategoryGroup> documentTypeCategoryGroup) {
    this.documentTypeCategoryGroup = documentTypeCategoryGroup;
    return this;
  }

  public DocumentListRequest addDocumentTypeCategoryGroupItem(DocumentCategoryGroup documentTypeCategoryGroupItem) {
    if (this.documentTypeCategoryGroup == null) {
      this.documentTypeCategoryGroup = new ArrayList<>();
    }
    this.documentTypeCategoryGroup.add(documentTypeCategoryGroupItem);
    return this;
  }

  /**
   * Get documentTypeCategoryGroup
   * @return documentTypeCategoryGroup
  */
  @Valid 
  @Schema(name = "documentTypeCategoryGroup", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("documentTypeCategoryGroup")
  public List<@Valid DocumentCategoryGroup> getDocumentTypeCategoryGroup() {
    return documentTypeCategoryGroup;
  }

  public void setDocumentTypeCategoryGroup(List<@Valid DocumentCategoryGroup> documentTypeCategoryGroup) {
    this.documentTypeCategoryGroup = documentTypeCategoryGroup;
  }

  public DocumentListRequest postedFromDate(Long postedFromDate) {
    this.postedFromDate = postedFromDate;
    return this;
  }

  /**
   * Epoch time in seconds 
   * @return postedFromDate
  */
  
  @Schema(name = "postedFromDate", example = "1740523843", description = "Epoch time in seconds ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("postedFromDate")
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
  */
  
  @Schema(name = "postedToDate", example = "1740523843", description = "Epoch time in seconds ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("postedToDate")
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
  */
  @Valid 
  @Schema(name = "pageNumber", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pageNumber")
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
  */
  @Valid 
  @Schema(name = "pageSize", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pageSize")
  public BigDecimal getPageSize() {
    return pageSize;
  }

  public void setPageSize(BigDecimal pageSize) {
    this.pageSize = pageSize;
  }

  public DocumentListRequest sortOrder(List<@Valid SortAndOrder> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  public DocumentListRequest addSortOrderItem(SortAndOrder sortOrderItem) {
    if (this.sortOrder == null) {
      this.sortOrder = new ArrayList<>();
    }
    this.sortOrder.add(sortOrderItem);
    return this;
  }

  /**
   * Get sortOrder
   * @return sortOrder
  */
  @Valid 
  @Schema(name = "sortOrder", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sortOrder")
  public List<@Valid SortAndOrder> getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(List<@Valid SortAndOrder> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

