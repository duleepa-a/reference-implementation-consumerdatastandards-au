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


@JsonTypeName("BusinessStakeholderPermissionItem")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-03-10T11:22:16.010207+05:30[Asia/Colombo]", comments = "Generator version: 7.19.0")
public class BusinessStakeholderPermissionItem   {
  private String accountId;
  private String userId;
  private String permission;

  public BusinessStakeholderPermissionItem() {
  }

  @JsonCreator
  public BusinessStakeholderPermissionItem(
    @JsonProperty(required = true, value = "accountId") String accountId,
    @JsonProperty(required = true, value = "userId") String userId,
    @JsonProperty(required = true, value = "permission") String permission
  ) {
    this.accountId = accountId;
    this.userId = userId;
    this.permission = permission;
  }

  /**
   * Account ID
   **/
  public BusinessStakeholderPermissionItem accountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  
  @ApiModelProperty(example = "586-522-B0025", required = true, value = "Account ID")
  @JsonProperty(required = true, value = "accountId")
  @NotNull public String getAccountId() {
    return accountId;
  }

  @JsonProperty(required = true, value = "accountId")
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * User identifier
   **/
  public BusinessStakeholderPermissionItem userId(String userId) {
    this.userId = userId;
    return this;
  }

  
  @ApiModelProperty(example = "nominatedUser1@wso2.com@carbon.super", required = true, value = "User identifier")
  @JsonProperty(required = true, value = "userId")
  @NotNull public String getUserId() {
    return userId;
  }

  @JsonProperty(required = true, value = "userId")
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Permission assigned to the user for the account
   **/
  public BusinessStakeholderPermissionItem permission(String permission) {
    this.permission = permission;
    return this;
  }

  
  @ApiModelProperty(example = "AUTHORIZE", required = true, value = "Permission assigned to the user for the account")
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
    BusinessStakeholderPermissionItem businessStakeholderPermissionItem = (BusinessStakeholderPermissionItem) o;
    return Objects.equals(this.accountId, businessStakeholderPermissionItem.accountId) &&
        Objects.equals(this.userId, businessStakeholderPermissionItem.userId) &&
        Objects.equals(this.permission, businessStakeholderPermissionItem.permission);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, userId, permission);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BusinessStakeholderPermissionItem {\n");
    
    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

