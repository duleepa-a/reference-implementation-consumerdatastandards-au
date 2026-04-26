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

package org.wso2.openbanking.consumerdatastandards.account.metadata.constants;

/**
 * This class holds common constants for the CDS Open Banking implementation.
 */
public class CommonConstants {

    // Constants Related to Joint Accounts
    public static final String DOMS_STATUS_PRE_APPROVAL = "pre-approval";
    public static final String DOMS_STATUS_NO_SHARING = "no-sharing";

    // Constants Related to Secondary Accounts
    public static final String SUI_ACTIVE_STATUS = "active";
    public static final String SUI_INACTIVE_STATUS = "inactive";
    public static final String LEGAL_ENTITY_SHARING_STATUS_BLOCKED = "blocked";
    public static final String LEGAL_ENTITY_SHARING_STATUS_ACTIVE = "active";

    // Constants Related to Business Stakeholders
    public static final String BNR_PERMISSION_AUTHORIZE = "AUTHORIZE";
    public static final String BNR_PERMISSION_REVOKE = "REVOKE";
    public static final String BNR_PERMISSION_VIEW = "VIEW";

    // IS Applications endpoint constants
    public static final String IS_APPLICATIONS_ENDPOINT = "https://localhost:9446/api/server/v1/applications";
    public static final String FILTER_TAG = "filter";
    public static final String ATTRIBUTES_TAG = "attributes";
    public static final String CLIENT_ID_FILTER_PREFIX = "clientId eq ";
    public static final String ADVANCED_CONFIGURATIONS_TAG = "advancedConfigurations";
    public static final String APPLICATIONS_TAG = "applications";
    public static final String ADDITIONAL_SP_PROPERTIES_TAG = "additionalSpProperties";
    public static final String PROPERTY_NAME_TAG = "name";
    public static final String PROPERTY_VALUE_TAG = "value";
    public static final String LEGAL_ENTITY_ID_PROPERTY_NAME = "legal_entity_id";

    // HTTP constants
    public static final String AUTH_HEADER = "Authorization";
    public static final String BASIC_TAG = "Basic ";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String ACCEPT_TAG = "Accept";

}
