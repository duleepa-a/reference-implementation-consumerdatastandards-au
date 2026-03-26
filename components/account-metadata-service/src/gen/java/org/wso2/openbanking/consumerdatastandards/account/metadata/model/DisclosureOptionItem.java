package org.wso2.openbanking.consumerdatastandards.account.metadata.model;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DisclosureOptionItem")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-03-26T15:09:37.936151200+05:30[Asia/Colombo]", comments = "Generator version: 7.21.0")
public class DisclosureOptionItem   {
    private String accountId;
    public enum DisclosureOptionEnum {

        NO_SHARING(String.valueOf("no-sharing")), PRE_APPROVAL(String.valueOf("pre-approval"));


        private String value;

        DisclosureOptionEnum (String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        /**
         * Convert a String into String, as specified in the
         * <a href="https://download.oracle.com/otndocs/jcp/jaxrs-2_0-fr-eval-spec/index.html">See JAX RS 2.0 Specification, section 3.2, p. 12</a>
         */
        public static DisclosureOptionEnum fromString(String s) {
            for (DisclosureOptionEnum b : DisclosureOptionEnum.values()) {
                // using Objects.toString() to be safe if value type non-object type
                // because types like 'int' etc. will be auto-boxed
                if (java.util.Objects.toString(b.value).equals(s)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected string value '" + s + "'");
        }

        @JsonCreator
        public static DisclosureOptionEnum fromValue(String value) {
            for (DisclosureOptionEnum b : DisclosureOptionEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private DisclosureOptionEnum disclosureOption;

    public DisclosureOptionItem() {
    }

    @JsonCreator
    public DisclosureOptionItem(
            @JsonProperty(required = true, value = "accountId") String accountId,
            @JsonProperty(required = true, value = "disclosureOption") DisclosureOptionEnum disclosureOption
    ) {
        this.accountId = accountId;
        this.disclosureOption = disclosureOption;
    }

    /**
     * Account ID
     **/
    public DisclosureOptionItem accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }


    @ApiModelProperty(example = "143-000-B1234", required = true, value = "Account ID")
    @JsonProperty(required = true, value = "accountId")
    @NotNull public String getAccountId() {
        return accountId;
    }

    @JsonProperty(required = true, value = "accountId")
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Disclosure option status
     **/
    public DisclosureOptionItem disclosureOption(DisclosureOptionEnum disclosureOption) {
        this.disclosureOption = disclosureOption;
        return this;
    }


    @ApiModelProperty(example = "no-sharing", required = true, value = "Disclosure option status")
    @JsonProperty(required = true, value = "disclosureOption")
    @NotNull public DisclosureOptionEnum getDisclosureOption() {
        return disclosureOption;
    }

    @JsonProperty(required = true, value = "disclosureOption")
    public void setDisclosureOption(DisclosureOptionEnum disclosureOption) {
        this.disclosureOption = disclosureOption;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DisclosureOptionItem disclosureOptionItem = (DisclosureOptionItem) o;
        return Objects.equals(this.accountId, disclosureOptionItem.accountId) &&
                Objects.equals(this.disclosureOption, disclosureOptionItem.disclosureOption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, disclosureOption);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DisclosureOptionItem {\n");

        sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
        sb.append("    disclosureOption: ").append(toIndentedString(disclosureOption)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }


}
