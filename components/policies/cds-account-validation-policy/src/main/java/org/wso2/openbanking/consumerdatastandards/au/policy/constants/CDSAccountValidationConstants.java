/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.openbanking.consumerdatastandards.au.policy.constants;

/**
 * Constants class for the Consent Enforcement Policy.
 */
public class CDSAccountValidationConstants {

    public static final String AUTH_HEADER = "Authorization";
    public static final String BASIC_TAG = "Basic ";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String INFO_HEADER_TAG = "Account-Request-Information";
    public static final String ACCEPT_TAG = "Accept";

    // Configs
    public static final String KEYSTORE_LOCATION_TAG = "Security.InternalKeyStore.Location";
    public static final String KEYSTORE_PASSWORD_TAG = "Security.InternalKeyStore.Password";
    public static final String SIGNING_ALIAS_TAG = "Security.InternalKeyStore.KeyAlias";
    public static final String SIGNING_KEY_PASSWORD = "Security.InternalKeyStore.KeyPassword";

    // Additional param keys
    public static final String USER_ID_TAG = "userId";
    public static final String CLIENT_ID_TAG = "clientId";
    public static final String CLIENT_ID_SNAKE_CASE_TAG = "client_id";
    public static final String AUTH_RESOURCES_TAG = "authorizationResources";
    public static final String CONSENT_MAPPING_RESOURCES_TAG = "consentMappingResources";
    public static final String AUTH_TYPE_TAG = "authorizationType";
    public static final String AUTH_ID_TAG = "authorizationId";
    public static final String ACCELERATOR_ACCOUNT_ID_TAG = "account_id";
    public static final String CDS_ACCOUNT_ID_TAG = "accountId";
    public static final String PRIMARY_AUTH_TYPE_TAG = "primary_member";

    // Error constants
    public static final String ERROR_CODE = "ERROR_CODE";
    public static final String ERROR_TITLE = "ERROR_TITLE";
    public static final String ERROR_DESCRIPTION = "ERROR_DESCRIPTION";
    public static final String CUSTOM_HTTP_SC = "CUSTOM_HTTP_SC";


    // Constants related to DOMS
    public static final String ACCOUNT_IDS_TAG = "accountIds";
    public static final String LINKED_MEMBER_TAG = "linked_member";
    public static final String DISCLOSURE_OPTION_TAG = "disclosureOption";
    public static final String DOMS_STATUS_NO_SHARING = "no-sharing";

    // Account-metadata webapp API paths
    public static final String DISCLOSURE_OPTIONS_PATH = "/disclosure-options";
    public static final String SECONDARY_ACCOUNTS_PATH = "/secondary-accounts";
    public static final String BUSINESS_STAKEHOLDERS_PATH = "/business-stakeholders";
    public static final String LEGAL_ENTITY_SHARING_PATH = "/legal-entity";
    public static final String IS_APPLICATIONS_ENDPOINT = "https://localhost:9446/api/server/v1/applications";

    // IS applications API query and response fields
    public static final String FILTER_TAG = "filter";
    public static final String ATTRIBUTES_TAG = "attributes";
    public static final String CLIENT_ID_FILTER_PREFIX = "clientId eq ";
    public static final String ADVANCED_CONFIGURATIONS_TAG = "advancedConfigurations";
    public static final String APPLICATIONS_TAG = "applications";
    public static final String ADDITIONAL_SP_PROPERTIES_TAG = "additionalSpProperties";
    public static final String NAME_TAG = "name";
    public static final String VALUE_TAG = "value";
    public static final String LEGAL_ENTITY_ID_PROPERTY_NAME = "legal_entity_id";

    // Legal entity sharing response fields
    public static final String LEGAL_ENTITY_ID_TAG = "legalEntityID";
    public static final String LEGAL_ENTITY_ID_CAMEL_CASE_TAG = "legalEntityId";
    public static final String LEGAL_ENTITY_SHARING_STATUS_TAG = "legalEntitySharingStatus";
    public static final String LEGAL_ENTITY_SHARING_STATUS_BLOCKED = "blocked";
    public static final String ACCOUNT_ID_UPPER_CASE_TAG = "accountID";

    // Constants related to Secondary Accounts
    public static final String SECONDARY_ACCOUNT_INSTRUCTION_STATUS_TAG = "secondaryAccountInstructionStatus";
    public static final String SECONDARY_ACCOUNT_STATUS_INACTIVE = "inactive";
    public static final String SECONDARY_INDIVIDUAL_ACCOUNT_OWNER_TAG = "secondary_individual_account_owner";
    public static final String SECONDARY_JOINT_ACCOUNT_OWNER_TAG = "secondary_joint_account_owner";

    // Constants related to Business Accounts
    public static final String NOMINATED_REPRESENTATIVE_TAG = "nominated_representative";
    public static final String BUSINESS_ACCOUNT_OWNER_TAG = "business_account_owner";
    public static final String BUSINESS_PERMISSION_TAG = "permission";
    public static final String BUSINESS_PERMISSION_AUTHORIZE = "AUTHORIZE";

    // Timeouts
    public static final int HTTP_CLIENT_CONNECT_TIMEOUT_MILLIS = 5000;
    public static final int HTTP_REQUEST_TIMEOUT_MILLIS = 10000;

}
