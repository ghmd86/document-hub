package io.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * MetadataNode
 */
@Validated
@NotUndefined
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")


public class MetadataNode   {
  @JsonProperty("key")

  private String key = null;

  @JsonProperty("value")

  private String value = null;

  /**
   * Optional type hint for proper indexing and querying
   */
  public enum DataTypeEnum {
    STRING("STRING"),
    
    NUMBER("NUMBER"),
    
    BOOLEAN("BOOLEAN"),
    
    DATE("DATE");

    private String value;

    DataTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DataTypeEnum fromValue(String text) {
      for (DataTypeEnum b : DataTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
  @JsonProperty("dataType")

  @JsonInclude(JsonInclude.Include.NON_ABSENT)  // Exclude from JSON if absent
  @JsonSetter(nulls = Nulls.FAIL)    // FAIL setting if the value is null
  private DataTypeEnum dataType = DataTypeEnum.STRING;


  public MetadataNode key(String key) { 

    this.key = key;
    return this;
  }

  /**
   * Alphanumeric key with underscores only
   * @return key
   **/
  
  @Schema(example = "threadId", required = true, description = "Alphanumeric key with underscores only")
  
  @NotNull
@Pattern(regexp="^[a-zA-Z0-9_]+") @Size(min=1,max=64)   public String getKey() {  
    return key;
  }



  public void setKey(String key) { 

    this.key = key;
  }

  public MetadataNode value(String value) { 

    this.value = value;
    return this;
  }

  /**
   * String value, maximum 1024 characters
   * @return value
   **/
  
  @Schema(example = "fa85f64-5717-4562-b3fc-2c963f66a78", required = true, description = "String value, maximum 1024 characters")
  
  @NotNull
@Size(min=1,max=1024)   public String getValue() {  
    return value;
  }



  public void setValue(String value) { 

    this.value = value;
  }

  public MetadataNode dataType(DataTypeEnum dataType) { 

    this.dataType = dataType;
    return this;
  }

  /**
   * Optional type hint for proper indexing and querying
   * @return dataType
   **/
  
  @Schema(description = "Optional type hint for proper indexing and querying")
  
  public DataTypeEnum getDataType() {  
    return dataType;
  }



  public void setDataType(DataTypeEnum dataType) { 
    this.dataType = dataType;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataNode metadataNode = (MetadataNode) o;
    return Objects.equals(this.key, metadataNode.key) &&
        Objects.equals(this.value, metadataNode.value) &&
        Objects.equals(this.dataType, metadataNode.dataType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value, dataType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MetadataNode {\n");
    
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    dataType: ").append(toIndentedString(dataType)).append("\n");
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
