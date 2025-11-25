package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * DocumentCategoryGroup
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class DocumentCategoryGroup {

  private String category;

  @Valid
  private List<String> documentTypes;

  public DocumentCategoryGroup category(String category) {
    this.category = category;
    return this;
  }

  /**
   * - \"PaymentConfirmationNotice\" - \"CreditLineIncrease\" - \"ChangeInTerms\" - \"AnnualFee\" - \"PrivacyPolicy\" - \"ElectronicDisclosure\" - \"CardholderAgreement\" - \"CreditProtection\" - \"Statement\" - \"BalanceTransfer\" - \"SavingsStatement\" - \"SavingsTaxStatement\" 
   * @return category
  */
  
  @Schema(name = "category", example = "PaymentConfirmationNotice", description = "- \"PaymentConfirmationNotice\" - \"CreditLineIncrease\" - \"ChangeInTerms\" - \"AnnualFee\" - \"PrivacyPolicy\" - \"ElectronicDisclosure\" - \"CardholderAgreement\" - \"CreditProtection\" - \"Statement\" - \"BalanceTransfer\" - \"SavingsStatement\" - \"SavingsTaxStatement\" ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public DocumentCategoryGroup documentTypes(List<String> documentTypes) {
    this.documentTypes = documentTypes;
    return this;
  }

  public DocumentCategoryGroup addDocumentTypesItem(String documentTypesItem) {
    if (this.documentTypes == null) {
      this.documentTypes = new ArrayList<>();
    }
    this.documentTypes.add(documentTypesItem);
    return this;
  }

  /**
   * Get documentTypes
   * @return documentTypes
  */
  
  @Schema(name = "documentTypes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("documentTypes")
  public List<String> getDocumentTypes() {
    return documentTypes;
  }

  public void setDocumentTypes(List<String> documentTypes) {
    this.documentTypes = documentTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DocumentCategoryGroup documentCategoryGroup = (DocumentCategoryGroup) o;
    return Objects.equals(this.category, documentCategoryGroup.category) &&
        Objects.equals(this.documentTypes, documentCategoryGroup.documentTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, documentTypes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DocumentCategoryGroup {\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    documentTypes: ").append(toIndentedString(documentTypes)).append("\n");
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

