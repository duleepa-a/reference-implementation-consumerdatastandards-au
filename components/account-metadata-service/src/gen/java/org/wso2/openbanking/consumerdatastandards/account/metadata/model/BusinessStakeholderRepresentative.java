package org.wso2.openbanking.consumerdatastandards.account.metadata.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;
import javax.validation.Valid;

import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonTypeName;



@JsonTypeName("BusinessStakeholderRepresentative")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-03-09T16:20:09.461136400+05:30[Asia/Colombo]", comments = "Generator version: 7.19.0")
public class BusinessStakeholderRepresentative   {
  private String name;
  private String permission;

  public BusinessStakeholderRepresentative() {
  }

  @JsonCreator
  public BusinessStakeholderRepresentative(
    @JsonProperty(required = true, value = "name") String name,
    @JsonProperty(required = true, value = "permission") String permission
  ) {
    this.name = name;
    this.permission = permission;
  }

  /**
   * Nominated representative user identifier
   **/
  public BusinessStakeholderRepresentative name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(example = "nominatedUser1@wso2.com@carbon.super", required = true, value = "Nominated representative user identifier")
  @JsonProperty(required = true, value = "name")
  @NotNull public String getName() {
    return name;
  }

  @JsonProperty(required = true, value = "name")
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Permission assigned to the nominated representative
   **/
  public BusinessStakeholderRepresentative permission(String permission) {
    this.permission = permission;
    return this;
  }

  
  @ApiModelProperty(example = "AUTHORIZE", required = true, value = "Permission assigned to the nominated representative")
  @JsonProperty(required = true, value = "permission")
  @NotNull public String getPermission() {
    return permission;
  }

  @JsonProperty(required = true, value = "permission")
  public void setPermission(String permission) {
    this.permission = permission;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BusinessStakeholderRepresentative businessStakeholderRepresentative = (BusinessStakeholderRepresentative) o;
    return Objects.equals(this.name, businessStakeholderRepresentative.name) &&
        Objects.equals(this.permission, businessStakeholderRepresentative.permission);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, permission);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BusinessStakeholderRepresentative {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    permission: ").append(toIndentedString(permission)).append("\n");
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

