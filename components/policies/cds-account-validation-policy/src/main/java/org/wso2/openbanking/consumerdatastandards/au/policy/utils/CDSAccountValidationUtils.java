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

package org.wso2.openbanking.consumerdatastandards.au.policy.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.openbanking.consumerdatastandards.au.policy.constants.CDSAccountValidationConstants;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.text.ParseException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for the Consent Enforcement Policy.
 */
public class CDSAccountValidationUtils {

    private static final Log log = LogFactory.getLog(CDSAccountValidationUtils.class);

    /**
     * Method to generate JWT with the given payload.
     *
     * @param payload JSON payload as a string to be included in the JWT claims
     * @return Serialized JWT as a string
     */
    public static String generateJWT(String payload) throws ParseException, JOSEException {

        log.debug("Generating JWT with provided payload");
        RSASSASigner signer = new RSASSASigner((PrivateKey) KeyStoreUtils.getSigningKey());
        JWTClaimsSet claimsSet = JWTClaimsSet.parse(payload);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(signer);
        log.info("JWT generated successfully");
        return signedJWT.serialize();
    }

    /**
     * Fetch all blocked account IDs by checking both the disclosure options
     * and secondary accounts services.
     *
     * @param accountIds set of account IDs to check
     * @param baseUrl base URL of the account metadata webapp
     * @param userId user ID for the secondary accounts query
     * @param basicAuthBase64 Base64-encoded Basic Auth credentials
     * @return combined set of blocked account IDs from both services
     */
    public static Set<String> fetchAllBlockedAccounts(
            Set<String> accountIds, String baseUrl, String userId, String basicAuthBase64) {

        String disclosureOptionsApi = baseUrl + CDSAccountValidationConstants.DISCLOSURE_OPTIONS_PATH;
        String secondaryAccountsApi = baseUrl + CDSAccountValidationConstants.SECONDARY_ACCOUNTS_PATH;
        String businessStakeholdersApi = baseUrl + CDSAccountValidationConstants.BUSINESS_STAKEHOLDERS_PATH;

        Set<String> blockedAccounts = fetchBlockedJointAccountsFromService(accountIds, disclosureOptionsApi,
                basicAuthBase64);
        Set<String> blockedSecondaryAccounts = fetchBlockedSecondaryAccountsFromService(accountIds,
                secondaryAccountsApi, userId, basicAuthBase64);
        Set<String> blockedBusinessAccounts = fetchBlockedBusinessAccountsFromService(accountIds,
                businessStakeholdersApi, userId, basicAuthBase64);

        blockedAccounts.addAll(blockedSecondaryAccounts);
        blockedAccounts.addAll(blockedBusinessAccounts);

        return blockedAccounts;
    }

    /**
     * Call disclosure options GET endpoint and return blocked account IDs.
     *
     * @param accountIds set of account IDs to check
     * @param blockedAccountsApi disclosure options API endpoint
     * @param basicAuthBase64 Base64-encoded Basic Auth credentials
     * @return set of blocked account IDs
     */
    static Set<String> fetchBlockedJointAccountsFromService(
            Set<String> accountIds, String blockedAccountsApi, String basicAuthBase64) {

        Set<String> blockedAccounts = new HashSet<>();

        if (accountIds == null || accountIds.isEmpty()) {
            return blockedAccounts;
        }

        try {
            String accountIdsParam = URLEncoder.encode(String.join(",", accountIds), StandardCharsets.UTF_8);
            String requestUrl = blockedAccountsApi + "?" + CDSAccountValidationConstants.ACCOUNT_IDS_TAG + "="
                    + accountIdsParam;
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(CDSAccountValidationConstants.HTTP_CLIENT_CONNECT_TIMEOUT_MILLIS))
                    .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestUrl))
                    .timeout(Duration.ofMillis(CDSAccountValidationConstants.HTTP_REQUEST_TIMEOUT_MILLIS))
                    .header(CDSAccountValidationConstants.ACCEPT_TAG, CDSAccountValidationConstants.JSON_CONTENT_TYPE)
                    .GET();

            if (StringUtils.isNotBlank(basicAuthBase64)) {
                requestBuilder.header(CDSAccountValidationConstants.AUTH_HEADER,
                        CDSAccountValidationConstants.BASIC_TAG + basicAuthBase64);
            } else {
                log.warn("[DOMS] Basic Auth property not set, request for fetching blocked accounts may fail");
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray disclosureOptions = new JSONArray(response.body());

                for (int i = 0; i < disclosureOptions.length(); i++) {
                    JSONObject accountDisclosure = disclosureOptions.optJSONObject(i);
                    if (accountDisclosure == null) {
                        continue;
                    }

                    String disclosureOption =
                            accountDisclosure.optString(CDSAccountValidationConstants.DISCLOSURE_OPTION_TAG, null);
                    if (CDSAccountValidationConstants.DOMS_STATUS_NO_SHARING.equalsIgnoreCase(disclosureOption)) {
                        String accountId = accountDisclosure.optString(
                                CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, null);
                        if (StringUtils.isNotBlank(accountId)) {
                            blockedAccounts.add(accountId);
                        }
                    }
                }
            } else {
                log.warn("Disclosure options service returned HTTP " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            log.error("[DOMS] Error calling disclosure options service", e);
        }

        return blockedAccounts;
    }

    /**
     * Call secondary accounts GET endpoint and return blocked account IDs.
     * An account is considered blocked if its secondaryAccountInstructionStatus is "inactive".
     *
     * @param accountIds set of account IDs to check
     * @param secondaryAccountsApi secondary accounts API endpoint
     * @param userId user ID for the secondary accounts query
     * @param basicAuthBase64 Base64-encoded Basic Auth credentials
     * @return set of blocked account IDs
     */
    static Set<String> fetchBlockedSecondaryAccountsFromService(
            Set<String> accountIds, String secondaryAccountsApi, String userId, String basicAuthBase64) {

        Set<String> blockedAccounts = new HashSet<>();

        if (accountIds == null || accountIds.isEmpty()) {
            return blockedAccounts;
        }

        if (StringUtils.isBlank(userId)) {
            log.warn("[SecondaryAccounts] userId is blank, skipping secondary accounts check");
            return blockedAccounts;
        }

        try {
            String accountIdsParam = URLEncoder.encode(String.join(",", accountIds), StandardCharsets.UTF_8);
            String userIdParam = URLEncoder.encode(userId, StandardCharsets.UTF_8);
            String requestUrl = secondaryAccountsApi + "?" + CDSAccountValidationConstants.ACCOUNT_IDS_TAG + "="
                    + accountIdsParam + "&" + CDSAccountValidationConstants.USER_ID_TAG + "=" + userIdParam;

            HttpClient client = HttpClient.newBuilder().connectTimeout(
                    Duration.ofMillis(CDSAccountValidationConstants.HTTP_CLIENT_CONNECT_TIMEOUT_MILLIS)).build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestUrl))
                    .timeout(Duration.ofMillis(CDSAccountValidationConstants.HTTP_REQUEST_TIMEOUT_MILLIS))
                    .header(CDSAccountValidationConstants.ACCEPT_TAG,
                            CDSAccountValidationConstants.JSON_CONTENT_TYPE).GET();

            if (StringUtils.isNotBlank(basicAuthBase64)) {
                requestBuilder.header(CDSAccountValidationConstants.AUTH_HEADER,
                        CDSAccountValidationConstants.BASIC_TAG + basicAuthBase64);
            } else {
                log.warn("[SecondaryAccounts] Basic Auth property not set, request may fail");
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray secondaryAccounts = new JSONArray(response.body());

                for (int i = 0; i < secondaryAccounts.length(); i++) {
                    JSONObject accountInstruction = secondaryAccounts.optJSONObject(i);
                    if (accountInstruction == null) {
                        continue;
                    }
                    String instructionStatus = accountInstruction.optString(
                            CDSAccountValidationConstants.SECONDARY_ACCOUNT_INSTRUCTION_STATUS_TAG, null);

                    if (CDSAccountValidationConstants.SECONDARY_ACCOUNT_STATUS_INACTIVE
                            .equalsIgnoreCase(instructionStatus)) {
                        String accountId = accountInstruction.optString(
                                CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, null);
                        if (StringUtils.isNotBlank(accountId)) {
                            blockedAccounts.add(accountId);
                        }
                    }
                }
            } else {
                log.warn("Secondary accounts service returned HTTP " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            log.error("[SecondaryAccounts] Error calling secondary accounts service", e);
        }
        return blockedAccounts;
    }

    /**
     * Call business stakeholders GET endpoint and return blocked account IDs.
     * An account is considered blocked when business permission is not AUTHORIZE.
     *
     * @param accountIds set of account IDs to check
     * @param businessStakeholdersApi business stakeholders API endpoint
     * @param userId user ID for the business stakeholders query
     * @param basicAuthBase64 Base64-encoded Basic Auth credentials
     * @return set of blocked account IDs
     */
    static Set<String> fetchBlockedBusinessAccountsFromService(Set<String> accountIds, String businessStakeholdersApi,
                                                               String userId, String basicAuthBase64) {

        Set<String> blockedAccounts = new HashSet<>();

        if (accountIds == null || accountIds.isEmpty()) {
            return blockedAccounts;
        }

        if (StringUtils.isBlank(userId)) {
            log.warn("[BusinessStakeholders] userId is blank, skipping business stakeholders check");
            return blockedAccounts;
        }

        try {
            String accountIdsParam = URLEncoder.encode(String.join(",", accountIds), StandardCharsets.UTF_8);
            String userIdParam = URLEncoder.encode(userId, StandardCharsets.UTF_8);
            String requestUrl = businessStakeholdersApi + "?" + CDSAccountValidationConstants.ACCOUNT_IDS_TAG + "="
                    + accountIdsParam + "&" + CDSAccountValidationConstants.USER_ID_TAG + "=" + userIdParam;

            HttpClient client = HttpClient.newBuilder().connectTimeout(
                    Duration.ofMillis(CDSAccountValidationConstants.HTTP_CLIENT_CONNECT_TIMEOUT_MILLIS)).build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestUrl))
                    .timeout(Duration.ofMillis(CDSAccountValidationConstants.HTTP_REQUEST_TIMEOUT_MILLIS))
                    .header(CDSAccountValidationConstants.ACCEPT_TAG,
                            CDSAccountValidationConstants.JSON_CONTENT_TYPE).GET();

            if (StringUtils.isNotBlank(basicAuthBase64)) {
                requestBuilder.header(CDSAccountValidationConstants.AUTH_HEADER,
                        CDSAccountValidationConstants.BASIC_TAG + basicAuthBase64);
            } else {
                log.warn("[BusinessStakeholders] Basic Auth property not set, request may fail");
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray businessStakeholders = new JSONArray(response.body());

                for (int i = 0; i < businessStakeholders.length(); i++) {
                    JSONObject permissionItem = businessStakeholders.optJSONObject(i);
                    if (permissionItem == null) {
                        continue;
                    }

                    String permission = permissionItem.optString(
                            CDSAccountValidationConstants.BUSINESS_PERMISSION_TAG, null);
                    if (!CDSAccountValidationConstants.BUSINESS_PERMISSION_AUTHORIZE.equalsIgnoreCase(permission)) {
                        String accountId = permissionItem.optString(
                                CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, null);
                        if (StringUtils.isNotBlank(accountId)) {
                            blockedAccounts.add(accountId);
                        }
                    }
                }
            } else {
                log.warn("Business stakeholders service returned HTTP " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            log.error("[BusinessStakeholders] Error calling business stakeholders service", e);
        }
        return blockedAccounts;
    }
}
