package org.wso2.openbanking.consumerdatastandards.account.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@JsonTypeName("LegalEntitySharingItem")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-03-19T12:29:00.058048100+05:30[Asia/Colombo]", comments = "Generator version: 7.19.0")
public class LegalEntitySharingItem   {
  private String secondaryUserID;
  private String accountID;
  private String legalEntityID;
  private String legalEntitySharingStatus;

  public LegalEntitySharingItem() {
  }

  @JsonCreator
  public LegalEntitySharingItem(
          @JsonProperty(required = true, value = "secondaryUserID") String secondaryUserID,
          @JsonProperty(required = true, value = "accountID") String accountID,
          @JsonProperty(required = true, value = "legalEntityID") String legalEntityID,
          @JsonProperty(required = true, value = "legalEntitySharingStatus") String legalEntitySharingStatus
  ) {
    this.secondaryUserID = secondaryUserID;
    this.accountID = accountID;
    this.legalEntityID = legalEntityID;
    this.legalEntitySharingStatus = legalEntitySharingStatus;
  }

  /**
   * Secondary user identifier. Together with accountID, this uniquely identifies a record in fs_secondary_user.
   **/
  public LegalEntitySharingItem secondaryUserID(String secondaryUserID) {
    this.secondaryUserID = secondaryUserID;
    return this;
  }


  @ApiModelProperty(example = "secondary.user@example.com", required = true, value = "Secondary user identifier. Together with accountID, this uniquely identifies a record in fs_secondary_user.")
  @JsonProperty(required = true, value = "secondaryUserID")
  @NotNull public String getSecondaryUserID() {
    return secondaryUserID;
  }

  @JsonProperty(required = true, value = "secondaryUserID")
  public void setSecondaryUserID(String secondaryUserID) {
    this.secondaryUserID = secondaryUserID;
  }

  /**
   * Account ID
   **/
  public LegalEntitySharingItem accountID(String accountID) {
    this.accountID = accountID;
    return this;
  }


  @ApiModelProperty(example = "acc-12345", required = true, value = "Account ID")
  @JsonProperty(required = true, value = "accountID")
  @NotNull public String getAccountID() {
    return accountID;
  }

  @JsonProperty(required = true, value = "accountID")
  public void setAccountID(String accountID) {
    this.accountID = accountID;
  }

  /**
   * Legal entity identifier
   **/
  public LegalEntitySharingItem legalEntityID(String legalEntityID) {
    this.legalEntityID = legalEntityID;
    return this;
  }


  @ApiModelProperty(example = "le-001", required = true, value = "Legal entity identifier")
  @JsonProperty(required = true, value = "legalEntityID")
  @NotNull public String getLegalEntityID() {
    return legalEntityID;
  }

  @JsonProperty(required = true, value = "legalEntityID")
  public void setLegalEntityID(String legalEntityID) {
    this.legalEntityID = legalEntityID;
  }

  /**
   * Legal entity sharing status. blocked adds legalEntityID to BLOCKED_ENTITIES, active removes legalEntityID from BLOCKED_ENTITIES.
   **/
  public LegalEntitySharingItem legalEntitySharingStatus(String legalEntitySharingStatus) {
    this.legalEntitySharingStatus = legalEntitySharingStatus;
    return this;
  }


  @ApiModelProperty(example = "blocked", required = true, value = "Legal entity sharing status. blocked adds legalEntityID to BLOCKED_ENTITIES, active removes legalEntityID from BLOCKED_ENTITIES.")
  @JsonProperty(required = true, value = "legalEntitySharingStatus")
  @NotNull public String getLegalEntitySharingStatus() {
    return legalEntitySharingStatus;
  }

  @JsonProperty(required = true, value = "legalEntitySharingStatus")
  public void setLegalEntitySharingStatus(String legalEntitySharingStatus) {
    this.legalEntitySharingStatus = legalEntitySharingStatus;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LegalEntitySharingItem legalEntitySharingItem = (LegalEntitySharingItem) o;
    return Objects.equals(this.secondaryUserID, legalEntitySharingItem.secondaryUserID) &&
            Objects.equals(this.accountID, legalEntitySharingItem.accountID) &&
            Objects.equals(this.legalEntityID, legalEntitySharingItem.legalEntityID) &&
            Objects.equals(this.legalEntitySharingStatus, legalEntitySharingItem.legalEntitySharingStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secondaryUserID, accountID, legalEntityID, legalEntitySharingStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LegalEntitySharingItem {\n");

    sb.append("    secondaryUserID: ").append(toIndentedString(secondaryUserID)).append("\n");
    sb.append("    accountID: ").append(toIndentedString(accountID)).append("\n");
    sb.append("    legalEntityID: ").append(toIndentedString(legalEntityID)).append("\n");
    sb.append("    legalEntitySharingStatus: ").append(toIndentedString(legalEntitySharingStatus)).append("\n");
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

