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
 * DocumentCategoryGroup
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class DocumentCategoryGroup   {
  @JsonProperty("category")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private String category = null;

  @JsonProperty("documentTypes")
  @Valid
  private List<String> documentTypes = null;

  public DocumentCategoryGroup category(String category) { 

    this.category = category;
    return this;
  }

  /**
   * - \"PaymentConfirmationNotice\" - \"CreditLineIncrease\" - \"ChangeInTerms\" - \"AnnualFee\" - \"PrivacyPolicy\" - \"ElectronicDisclosure\" - \"CardholderAgreement\" - \"CreditProtection\" - \"Statement\" - \"BalanceTransfer\" - \"SavingsStatement\" - \"SavingsTaxStatement\" 
   * @return category
   **/
  
  @Schema(example = "PaymentConfirmationNotice", description = "- \"PaymentConfirmationNotice\" - \"CreditLineIncrease\" - \"ChangeInTerms\" - \"AnnualFee\" - \"PrivacyPolicy\" - \"ElectronicDisclosure\" - \"CardholderAgreement\" - \"CreditProtection\" - \"Statement\" - \"BalanceTransfer\" - \"SavingsStatement\" - \"SavingsTaxStatement\" ")
  
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
      this.documentTypes = new ArrayList<String>();
    }
    this.documentTypes.add(documentTypesItem);
    return this;
  }

  /**
   * Get documentTypes
   * @return documentTypes
   **/
  
  @Schema(description = "")
  
  public List<String> getDocumentTypes() {  
    return documentTypes;
  }



  public void setDocumentTypes(List<String> documentTypes) { 
    this.documentTypes = documentTypes;
  }

  @Override
  public boolean equals(java.lang.Object o) {
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
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
