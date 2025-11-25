package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * MetadataNode
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-24T18:34:04.945806200-08:00[America/Los_Angeles]")
public class MetadataNode {

  private String key;

  private String value;

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

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DataTypeEnum fromValue(String value) {
      for (DataTypeEnum b : DataTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private DataTypeEnum dataType = DataTypeEnum.STRING;

  /**
   * Default constructor
   * @deprecated Use {@link MetadataNode#MetadataNode(String, String)}
   */
  @Deprecated
  public MetadataNode() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MetadataNode(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public MetadataNode key(String key) {
    this.key = key;
    return this;
  }

  /**
   * Alphanumeric key with underscores only
   * @return key
  */
  @NotNull @Pattern(regexp = "^[a-zA-Z0-9_]+") @Size(min = 1, max = 64) 
  @Schema(name = "key", example = "threadId", description = "Alphanumeric key with underscores only", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("key")
  public String getKey() {
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
  */
  @NotNull @Size(min = 1, max = 1024) 
  @Schema(name = "value", example = "fa85f64-5717-4562-b3fc-2c963f66a78", description = "String value, maximum 1024 characters", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("value")
  public String getValue() {
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
  */
  
  @Schema(name = "dataType", description = "Optional type hint for proper indexing and querying", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dataType")
  public DataTypeEnum getDataType() {
    return dataType;
  }

  public void setDataType(DataTypeEnum dataType) {
    this.dataType = dataType;
  }

  @Override
  public boolean equals(Object o) {
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

