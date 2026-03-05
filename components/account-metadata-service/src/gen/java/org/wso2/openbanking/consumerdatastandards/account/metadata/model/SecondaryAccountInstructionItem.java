package org.wso2.openbanking.consumerdatastandards.account.metadata.model;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SecondaryAccountInstructionItem")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-02-22T21:56:16.859163500+05:30[Asia/Colombo]", comments = "Generator version: 7.19.0")
public class SecondaryAccountInstructionItem   {
  private String accountId;
  private String secondaryUserId;
  private Boolean otherAccountsAvailability;
  private String secondaryAccountInstructionStatus;

  public SecondaryAccountInstructionItem() {
  }

  @JsonCreator
  public SecondaryAccountInstructionItem(
          @JsonProperty(required = true, value = "accountId") String accountId,
          @JsonProperty(required = true, value = "secondaryUserId") String secondaryUserId,
          @JsonProperty(required = true, value = "otherAccountsAvailability") Boolean otherAccountsAvailability,
          @JsonProperty(required = true, value = "secondaryAccountInstructionStatus") String secondaryAccountInstructionStatus
  ) {
    this.accountId = accountId;
    this.secondaryUserId = secondaryUserId;
    this.otherAccountsAvailability = otherAccountsAvailability;
    this.secondaryAccountInstructionStatus = secondaryAccountInstructionStatus;
  }

  /**
   * Account ID
   **/
  public SecondaryAccountInstructionItem accountId(String accountId) {
    this.accountId = accountId;
    return this;
  }


  @ApiModelProperty(example = "7500101541", required = true, value = "Account ID")
  @JsonProperty(required = true, value = "accountId")
  @NotNull public String getAccountId() {
    return accountId;
  }

  @JsonProperty(required = true, value = "accountId")
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Secondary user identifier
   **/
  public SecondaryAccountInstructionItem secondaryUserId(String secondaryUserId) {
    this.secondaryUserId = secondaryUserId;
    return this;
  }


  @ApiModelProperty(example = "user-1", required = true, value = "Secondary user identifier")
  @JsonProperty(required = true, value = "secondaryUserId")
  @NotNull public String getSecondaryUserId() {
    return secondaryUserId;
  }

  @JsonProperty(required = true, value = "secondaryUserId")
  public void setSecondaryUserId(String secondaryUserId) {
    this.secondaryUserId = secondaryUserId;
  }

  /**
   * Indicates whether the secondary user has other accounts available in the bank
   **/
  public SecondaryAccountInstructionItem otherAccountsAvailability(Boolean otherAccountsAvailability) {
    this.otherAccountsAvailability = otherAccountsAvailability;
    return this;
  }


  @ApiModelProperty(example = "true", required = true, value = "Indicates whether the secondary user has other accounts available in the bank")
  @JsonProperty(required = true, value = "otherAccountsAvailability")
  @NotNull public Boolean getOtherAccountsAvailability() {
    return otherAccountsAvailability;
  }

  @JsonProperty(required = true, value = "otherAccountsAvailability")
  public void setOtherAccountsAvailability(Boolean otherAccountsAvailability) {
    this.otherAccountsAvailability = otherAccountsAvailability;
  }

  /**
   * Secondary account instruction status
   **/
  public SecondaryAccountInstructionItem secondaryAccountInstructionStatus(String secondaryAccountInstructionStatus) {
    this.secondaryAccountInstructionStatus = secondaryAccountInstructionStatus;
    return this;
  }


  @ApiModelProperty(example = "inactive", required = true, value = "Secondary account instruction status")
  @JsonProperty(required = true, value = "secondaryAccountInstructionStatus")
  @NotNull public String getSecondaryAccountInstructionStatus() {
    return secondaryAccountInstructionStatus;
  }

  @JsonProperty(required = true, value = "secondaryAccountInstructionStatus")
  public void setSecondaryAccountInstructionStatus(String secondaryAccountInstructionStatus) {
    this.secondaryAccountInstructionStatus = secondaryAccountInstructionStatus;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SecondaryAccountInstructionItem secondaryAccountInstructionItem = (SecondaryAccountInstructionItem) o;
    return Objects.equals(this.accountId, secondaryAccountInstructionItem.accountId) &&
            Objects.equals(this.secondaryUserId, secondaryAccountInstructionItem.secondaryUserId) &&
            Objects.equals(this.otherAccountsAvailability, secondaryAccountInstructionItem.otherAccountsAvailability) &&
            Objects.equals(this.secondaryAccountInstructionStatus, secondaryAccountInstructionItem.secondaryAccountInstructionStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, secondaryUserId, otherAccountsAvailability, secondaryAccountInstructionStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SecondaryAccountInstructionItem {\n");

    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    secondaryUserId: ").append(toIndentedString(secondaryUserId)).append("\n");
    sb.append("    otherAccountsAvailability: ").append(toIndentedString(otherAccountsAvailability)).append("\n");
    sb.append("    secondaryAccountInstructionStatus: ").append(toIndentedString(secondaryAccountInstructionStatus)).append("\n");
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
