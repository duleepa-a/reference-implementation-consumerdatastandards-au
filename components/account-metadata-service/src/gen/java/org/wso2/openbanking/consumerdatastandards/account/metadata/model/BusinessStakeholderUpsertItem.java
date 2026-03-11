package org.wso2.openbanking.consumerdatastandards.account.metadata.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderRepresentative;
import javax.validation.constraints.*;
import javax.validation.Valid;

import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonTypeName;



@JsonTypeName("BusinessStakeholderUpsertItem")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-03-09T16:20:09.461136400+05:30[Asia/Colombo]", comments = "Generator version: 7.19.0")
public class BusinessStakeholderUpsertItem   {
  private String accountID;
  private @Valid List<String> accountOwners = new ArrayList<>();
  private @Valid List<@Valid BusinessStakeholderRepresentative> nominatedRepresentatives = new ArrayList<>();

  public BusinessStakeholderUpsertItem() {
  }

  @JsonCreator
  public BusinessStakeholderUpsertItem(
    @JsonProperty(required = true, value = "accountID") String accountID,
    @JsonProperty(required = true, value = "accountOwners") List<String> accountOwners,
    @JsonProperty(required = true, value = "nominatedRepresentatives") List<@Valid BusinessStakeholderRepresentative> nominatedRepresentatives
  ) {
    this.accountID = accountID;
    this.accountOwners = accountOwners;
    this.nominatedRepresentatives = nominatedRepresentatives;
  }

  /**
   * Account ID
   **/
  public BusinessStakeholderUpsertItem accountID(String accountID) {
    this.accountID = accountID;
    return this;
  }

  
  @ApiModelProperty(example = "586-522-B0025", required = true, value = "Account ID")
  @JsonProperty(required = true, value = "accountID")
  @NotNull public String getAccountID() {
    return accountID;
  }

  @JsonProperty(required = true, value = "accountID")
  public void setAccountID(String accountID) {
    this.accountID = accountID;
  }

  /**
   * List of account owner user identifiers
   **/
  public BusinessStakeholderUpsertItem accountOwners(List<String> accountOwners) {
    this.accountOwners = accountOwners;
    return this;
  }

  
  @ApiModelProperty(example = "[\"nominatedUser3@wso2.com@carbon.super\",\"user2@wso2.com@carbon.super\"]", required = true, value = "List of account owner user identifiers")
  @JsonProperty(required = true, value = "accountOwners")
  @NotNull public List<String> getAccountOwners() {
    return accountOwners;
  }

  @JsonProperty(required = true, value = "accountOwners")
  public void setAccountOwners(List<String> accountOwners) {
    this.accountOwners = accountOwners;
  }

  public BusinessStakeholderUpsertItem addAccountOwnersItem(String accountOwnersItem) {
    if (this.accountOwners == null) {
      this.accountOwners = new ArrayList<>();
    }

    this.accountOwners.add(accountOwnersItem);
    return this;
  }

  public BusinessStakeholderUpsertItem removeAccountOwnersItem(String accountOwnersItem) {
    if (accountOwnersItem != null && this.accountOwners != null) {
      this.accountOwners.remove(accountOwnersItem);
    }

    return this;
  }
  /**
   * List of nominated representatives with permissions
   **/
  public BusinessStakeholderUpsertItem nominatedRepresentatives(List<@Valid BusinessStakeholderRepresentative> nominatedRepresentatives) {
    this.nominatedRepresentatives = nominatedRepresentatives;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "List of nominated representatives with permissions")
  @JsonProperty(required = true, value = "nominatedRepresentatives")
  @NotNull @Valid public List<@Valid BusinessStakeholderRepresentative> getNominatedRepresentatives() {
    return nominatedRepresentatives;
  }

  @JsonProperty(required = true, value = "nominatedRepresentatives")
  public void setNominatedRepresentatives(List<@Valid BusinessStakeholderRepresentative> nominatedRepresentatives) {
    this.nominatedRepresentatives = nominatedRepresentatives;
  }

  public BusinessStakeholderUpsertItem addNominatedRepresentativesItem(BusinessStakeholderRepresentative nominatedRepresentativesItem) {
    if (this.nominatedRepresentatives == null) {
      this.nominatedRepresentatives = new ArrayList<>();
    }

    this.nominatedRepresentatives.add(nominatedRepresentativesItem);
    return this;
  }

  public BusinessStakeholderUpsertItem removeNominatedRepresentativesItem(BusinessStakeholderRepresentative nominatedRepresentativesItem) {
    if (nominatedRepresentativesItem != null && this.nominatedRepresentatives != null) {
      this.nominatedRepresentatives.remove(nominatedRepresentativesItem);
    }

    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BusinessStakeholderUpsertItem businessStakeholderUpsertItem = (BusinessStakeholderUpsertItem) o;
    return Objects.equals(this.accountID, businessStakeholderUpsertItem.accountID) &&
        Objects.equals(this.accountOwners, businessStakeholderUpsertItem.accountOwners) &&
        Objects.equals(this.nominatedRepresentatives, businessStakeholderUpsertItem.nominatedRepresentatives);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountID, accountOwners, nominatedRepresentatives);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BusinessStakeholderUpsertItem {\n");
    
    sb.append("    accountID: ").append(toIndentedString(accountID)).append("\n");
    sb.append("    accountOwners: ").append(toIndentedString(accountOwners)).append("\n");
    sb.append("    nominatedRepresentatives: ").append(toIndentedString(nominatedRepresentatives)).append("\n");
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

