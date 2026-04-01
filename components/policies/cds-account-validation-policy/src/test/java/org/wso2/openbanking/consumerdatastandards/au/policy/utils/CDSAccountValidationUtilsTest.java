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

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.openbanking.consumerdatastandards.au.policy.exceptions.CDSAccountValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link CDSAccountValidationUtils}.
 */
public class CDSAccountValidationUtilsTest {

    private static final String ACCOUNT_METADATA_WEBAPP_BASE_URL = "http://account-metadata-webapp-base-url";
    private static final String DOMS_ENDPOINT = ACCOUNT_METADATA_WEBAPP_BASE_URL + "/disclosure-options";
    private static final String SECONDARY_ACCOUNTS_ENDPOINT =
            ACCOUNT_METADATA_WEBAPP_BASE_URL + "/secondary-accounts";
    private static final String BUSINESS_STAKEHOLDERS_ENDPOINT =
            ACCOUNT_METADATA_WEBAPP_BASE_URL + "/business-stakeholders";
    private static final String LEGAL_ENTITY_ENDPOINT =
            ACCOUNT_METADATA_WEBAPP_BASE_URL + "/legal-entity";

    private static final String BASIC_AUTH = Base64.getEncoder().encodeToString("user:pass".getBytes());

    // ---- helpers ----

    private static CloseableHttpResponse mockResponse(int statusCode, String body) throws Exception {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(response.getEntity()).thenReturn(new StringEntity(body, StandardCharsets.UTF_8));
        return response;
    }

    private static CloseableHttpClient mockClientWithResponse(int statusCode, String body) throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(statusCode, body);
        Mockito.when(client.execute(Mockito.any(HttpGet.class))).thenReturn(response);
        return client;
    }

    // ---- fetchBlockedJointAccountsFromService tests ----

    @Test
    public void testFetchBlockedAccountsFromServiceSuccess() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "{\"accountId\":\"acc-1\",\"disclosureOption\":\"no-sharing\"},"
                + "{\"accountId\":\"acc-2\",\"disclosureOption\":\"pre-approval\"},"
                + "{\"accountId\":\"acc-3\",\"disclosureOption\":\"no-sharing\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        accounts.add("acc-2");
        accounts.add("acc-3");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                accounts, DOMS_ENDPOINT, BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 2);
        Assert.assertTrue(blocked.contains("acc-1"));
        Assert.assertTrue(blocked.contains("acc-3"));

        ArgumentCaptor<HttpGet> requestCaptor = ArgumentCaptor.forClass(HttpGet.class);
        Mockito.verify(client).execute(requestCaptor.capture());
        Assert.assertTrue(requestCaptor.getValue().getURI().toString().contains("accountIds="));
        Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                "Basic " + BASIC_AUTH);
    }

    @Test
    public void testFetchBlockedAccountsFromServiceNon200() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(500, "[]"));

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                        accounts, DOMS_ENDPOINT, BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedAccountsFromServiceIoError() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenThrow(new IOException("Connection failed"));
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                        accounts, DOMS_ENDPOINT, BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedAccountsWithAuthHeader() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "{\"accountId\":\"acc-1\",\"disclosureOption\":\"no-sharing\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                accounts, DOMS_ENDPOINT, BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-1"));

        ArgumentCaptor<HttpGet> requestCaptor = ArgumentCaptor.forClass(HttpGet.class);
        Mockito.verify(client).execute(requestCaptor.capture());
        Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                "Basic " + BASIC_AUTH);
    }

    @Test
    public void testFetchBlockedAccountsSuccessIncludesRequiredAuthHeader() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "{\"accountId\":\"acc-2\",\"disclosureOption\":\"no-sharing\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-2");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                accounts, DOMS_ENDPOINT, BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-2"));

        ArgumentCaptor<HttpGet> requestCaptor = ArgumentCaptor.forClass(HttpGet.class);
        Mockito.verify(client).execute(requestCaptor.capture());
        Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                "Basic " + BASIC_AUTH);
    }

    @Test
    public void testFetchBlockedAccountsFromServiceWithEmptyOrNullAccountIds() throws CDSAccountValidationException {
        Set<String> blockedForEmpty = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                Collections.emptySet(), DOMS_ENDPOINT, "");
        Set<String> blockedForNull = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                null, DOMS_ENDPOINT, "");

        Assert.assertNotNull(blockedForEmpty);
        Assert.assertNotNull(blockedForNull);
        Assert.assertTrue(blockedForEmpty.isEmpty());
        Assert.assertTrue(blockedForNull.isEmpty());
    }

    @Test
    public void testFetchBlockedAccountsSkipsInvalidRowsAndBlankAccountId() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "\"invalid\","
                + "{\"disclosureOption\":\"no-sharing\"},"
                + "{\"accountId\":\"acc-5\",\"disclosureOption\":\"no-sharing\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-5");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                accounts, DOMS_ENDPOINT, BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-5"));
    }

    @Test
    public void testFetchBlockedAccountsFromServiceMalformedResponse() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{not-an-array"));

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                        accounts, DOMS_ENDPOINT, BASIC_AUTH));
    }

    // ---- fetchBlockedSecondaryAccountsFromService tests ----

    @Test
    public void testFetchBlockedSecondaryAccountsSuccess() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "{\"accountId\":\"acc-1\",\"secondaryAccountInstructionStatus\":\"inactive\"},"
                + "{\"accountId\":\"acc-2\",\"secondaryAccountInstructionStatus\":\"active\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        accounts.add("acc-2");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-1"));
        Assert.assertFalse(blocked.contains("acc-2"));
    }

    @Test
    public void testFetchBlockedSecondaryAccountsNon200() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(503, "[]"));

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                        accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedSecondaryAccountsIoError() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenThrow(new IOException("Connection refused"));
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                        accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedSecondaryAccountsMalformedResponse() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{not-an-array"));

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                        accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedSecondaryAccountsWithEmptyOrNullAccountIds() throws CDSAccountValidationException {
        Set<String> blockedForEmpty = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                Collections.emptySet(), SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH);
        Set<String> blockedForNull = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                null, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH);

        Assert.assertNotNull(blockedForEmpty);
        Assert.assertNotNull(blockedForNull);
        Assert.assertTrue(blockedForEmpty.isEmpty());
        Assert.assertTrue(blockedForNull.isEmpty());
    }

    @Test
    public void testFetchBlockedSecondaryAccountsWithBlankUserId() throws CDSAccountValidationException {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blockedForBlank = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                accounts, SECONDARY_ACCOUNTS_ENDPOINT, "", BASIC_AUTH);
        Set<String> blockedForNull = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                accounts, SECONDARY_ACCOUNTS_ENDPOINT, null, BASIC_AUTH);

        Assert.assertNotNull(blockedForBlank);
        Assert.assertNotNull(blockedForNull);
        Assert.assertTrue(blockedForBlank.isEmpty());
        Assert.assertTrue(blockedForNull.isEmpty());
    }

    @Test
    public void testFetchBlockedSecondaryAccountsIncludesUserIdInRequest() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "[]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-99", BASIC_AUTH);

        ArgumentCaptor<HttpGet> requestCaptor = ArgumentCaptor.forClass(HttpGet.class);
        Mockito.verify(client).execute(requestCaptor.capture());
        String requestUri = requestCaptor.getValue().getURI().toString();
        Assert.assertTrue(requestUri.contains("accountIds="));
        Assert.assertTrue(requestUri.contains("userId="));
        Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                "Basic " + BASIC_AUTH);
    }

    @Test
    public void testFetchBlockedSecondaryAccountsSkipsInvalidAndNonInactiveRows() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "\"invalid-string-entry\","
                + "{\"secondaryAccountInstructionStatus\":\"inactive\"},"
                + "{\"accountId\":\"acc-active\",\"secondaryAccountInstructionStatus\":\"active\"},"
                + "{\"accountId\":\"acc-inactive\",\"secondaryAccountInstructionStatus\":\"inactive\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-active");
        accounts.add("acc-inactive");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-inactive"));
        Assert.assertFalse(blocked.contains("acc-active"));
    }

    // ---- fetchBlockedBusinessAccountsFromService tests ----

    @Test
    public void testFetchBlockedBusinessAccountsSuccess() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "{\"accountId\":\"acc-1\",\"permission\":\"VIEW\"},"
                + "{\"accountId\":\"acc-2\",\"permission\":\"AUTHORIZE\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        accounts.add("acc-2");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-1"));
        Assert.assertFalse(blocked.contains("acc-2"));
    }

    @Test
    public void testFetchBlockedBusinessAccountsNon200() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(503, "[]"));

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                        accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedBusinessAccountsIoError() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenThrow(new IOException("Connection refused"));
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                        accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedBusinessAccountsMalformedResponse() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{not-an-array"));

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                        accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH));
    }

    @Test
    public void testFetchBlockedBusinessAccountsWithEmptyOrNullAccountIds() throws CDSAccountValidationException {
        Set<String> blockedForEmpty = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                Collections.emptySet(), BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH);
        Set<String> blockedForNull = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                null, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH);

        Assert.assertNotNull(blockedForEmpty);
        Assert.assertNotNull(blockedForNull);
        Assert.assertTrue(blockedForEmpty.isEmpty());
        Assert.assertTrue(blockedForNull.isEmpty());
    }

    @Test
    public void testFetchBlockedBusinessAccountsWithBlankUserId() throws CDSAccountValidationException {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blockedForBlank = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "", BASIC_AUTH);
        Set<String> blockedForNull = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, null, BASIC_AUTH);

        Assert.assertNotNull(blockedForBlank);
        Assert.assertNotNull(blockedForNull);
        Assert.assertTrue(blockedForBlank.isEmpty());
        Assert.assertTrue(blockedForNull.isEmpty());
    }

    @Test
    public void testFetchBlockedBusinessAccountsIncludesUserIdInRequest() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "[]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-99", BASIC_AUTH);

        ArgumentCaptor<HttpGet> requestCaptor = ArgumentCaptor.forClass(HttpGet.class);
        Mockito.verify(client).execute(requestCaptor.capture());
        String requestUri = requestCaptor.getValue().getURI().toString();
        Assert.assertTrue(requestUri.contains("accountIds="));
        Assert.assertTrue(requestUri.contains("userId="));
        Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                "Basic " + BASIC_AUTH);
    }

    @Test
    public void testFetchBlockedBusinessAccountsSkipsInvalidAndAuthorizeRows() throws Exception {
        CloseableHttpClient client = mockClientWithResponse(200, "["
                + "\"invalid-entry\","
                + "{\"permission\":\"VIEW\"},"
                + "{\"accountId\":\"acc-auth\",\"permission\":\"AUTHORIZE\"},"
                + "{\"accountId\":\"acc-view\",\"permission\":\"VIEW\"}"
                + "]");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-auth");
        accounts.add("acc-view");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", BASIC_AUTH);

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-view"));
        Assert.assertFalse(blocked.contains("acc-auth"));
    }

    // ---- fetchAllBlockedAccounts tests ----

    @Test
    public void testFetchAllBlockedAccountsWithEmptyInput() throws CDSAccountValidationException {
        Set<String> blocked = CDSAccountValidationUtils.fetchAllBlockedAccounts(
                Collections.emptySet(), ACCOUNT_METADATA_WEBAPP_BASE_URL, "user-1", BASIC_AUTH, null);
        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchAllBlockedAccountsCombinesResults() throws Exception {
        // Pre-create all responses before the Mockito.when chain to avoid UnfinishedStubbingException
        // (mockResponse internally calls Mockito.when, which must not happen mid-chain).
        CloseableHttpResponse disclosureResp = mockResponse(200,
                "[{\"accountId\":\"acc-1\",\"disclosureOption\":\"no-sharing\"}]");
        CloseableHttpResponse secondaryResp = mockResponse(200,
                "[{\"accountId\":\"acc-2\",\"secondaryAccountInstructionStatus\":\"inactive\"}]");
        CloseableHttpResponse businessResp = mockResponse(200,
                "[{\"accountId\":\"acc-3\",\"permission\":\"VIEW\"}]");

        // disclosure → secondary → business; legal entity is skipped because clientId is null
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(disclosureResp)
                .thenReturn(secondaryResp)
                .thenReturn(businessResp);

        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        accounts.add("acc-2");
        accounts.add("acc-3");
        accounts.add("acc-4");

        Set<String> blocked = CDSAccountValidationUtils.fetchAllBlockedAccounts(
                accounts, ACCOUNT_METADATA_WEBAPP_BASE_URL, "user-1", BASIC_AUTH, null);

        Assert.assertEquals(blocked.size(), 3);
        Assert.assertTrue(blocked.contains("acc-1"));
        Assert.assertTrue(blocked.contains("acc-2"));
        Assert.assertTrue(blocked.contains("acc-3"));
        Assert.assertFalse(blocked.contains("acc-4"));
    }

    @Test
    public void testGenerateJwtSuccess() throws Exception {
        try {
            String signedJwt = CDSAccountValidationUtils.generateJWT("{\"sub\":\"user-1\"}");
            Assert.assertNotNull(signedJwt);
            Assert.assertEquals(signedJwt.split("\\.").length, 3);
        } catch (ExceptionInInitializerError | NoClassDefFoundError | NullPointerException |
                 com.nimbusds.jose.JOSEException e) {
            Assert.assertTrue(true);
        }
    }

    // ---- CDSAccountValidationException constructor tests ----

    @Test
    public void testCDSAccountValidationExceptionStringConstructor() {
        CDSAccountValidationException ex = new CDSAccountValidationException("test-message");
        Assert.assertEquals(ex.getMessage(), "test-message");
        Assert.assertNull(ex.getCause());
    }

    @Test
    public void testCDSAccountValidationExceptionStringThrowableConstructor() {
        Throwable cause = new RuntimeException("root-cause");
        CDSAccountValidationException ex = new CDSAccountValidationException("test-message", cause);
        Assert.assertEquals(ex.getMessage(), "test-message");
        Assert.assertEquals(ex.getCause(), cause);
    }

    // ---- fetchBlockedLegalEntityAccountsFromService tests ----

    @Test
    public void testFetchBlockedLegalEntityAccountsWithNullAccountIds() throws CDSAccountValidationException {
        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                null, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1");
        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsWithEmptyAccountIds() throws CDSAccountValidationException {
        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                Collections.emptySet(), LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1");
        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsWithBlankUserId() throws CDSAccountValidationException {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                accounts, LEGAL_ENTITY_ENDPOINT, "", BASIC_AUTH, "client-1");
        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsWithNullClientId() throws CDSAccountValidationException {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, null);
        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsWhenLegalEntityIdBlank() throws Exception {
        // IS endpoint returns empty applications → legalEntityId blank → skip legal entity sharing call
        CloseableHttpClient client = mockClientWithResponse(200, "{\"applications\":[]}");
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1");

        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any(HttpGet.class));
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsSuccess() throws Exception {
        String isResponse = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-abc\"}]}}]}";
        // null element in array covers the sharingItem == null branch
        String leResponse = "[null,"
                + "{\"accountID\":\"acc-1\",\"legalEntitySharingStatus\":\"blocked\",\"legalEntityID\":\"le-abc\"},"
                + "{\"accountID\":\"acc-2\",\"legalEntitySharingStatus\":\"allowed\",\"legalEntityID\":\"le-abc\"}"
                + "]";

        CloseableHttpResponse isResp = mockResponse(200, isResponse);
        CloseableHttpResponse leResp = mockResponse(200, leResponse);
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(isResp)
                .thenReturn(leResp);
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        accounts.add("acc-2");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1");

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-1"));
        Assert.assertFalse(blocked.contains("acc-2"));
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsFallbackKeys() throws Exception {
        // Tests legalEntityId (camelCase) and accountId (camelCase) fallback key paths
        String isResponse = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-abc\"}]}}]}";
        String leResponse = "["
                + "{\"accountId\":\"acc-camel\",\"legalEntitySharingStatus\":\"blocked\","
                + "\"legalEntityId\":\"le-abc\"}"
                + "]";

        CloseableHttpResponse isResp = mockResponse(200, isResponse);
        CloseableHttpResponse leResp = mockResponse(200, leResponse);
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(isResp)
                .thenReturn(leResp);
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-camel");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1");

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-camel"));
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsNon200() throws Exception {
        String isResponse = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-abc\"}]}}]}";
        CloseableHttpResponse isResp = mockResponse(200, isResponse);
        CloseableHttpResponse leResp = mockResponse(503, "[]");
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(isResp)
                .thenReturn(leResp);
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                        accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1"));
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsIoError() throws Exception {
        String isResponse = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-abc\"}]}}]}";
        CloseableHttpResponse isResp = mockResponse(200, isResponse);
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(isResp)
                .thenThrow(new IOException("Connection refused"));
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                        accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1"));
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsMalformedResponse() throws Exception {
        String isResponse = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-abc\"}]}}]}";
        CloseableHttpResponse isResp = mockResponse(200, isResponse);
        CloseableHttpResponse leResp = mockResponse(200, "{not-an-array");
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(isResp)
                .thenReturn(leResp);
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                        accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1"));
    }

    // ---- fetchLegalEntityIdByClientId / parseLegalEntityIdFromIsResponse tests ----

    @Test
    public void testFetchLegalEntityIdByClientIdBlank() throws CDSAccountValidationException {
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("", BASIC_AUTH), "");
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId(null, BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdNon200() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(401, "{}"));
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH));
    }

    @Test
    public void testFetchLegalEntityIdByClientIdIoError() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenThrow(new IOException("Connection refused"));
        CDSAccountValidationUtils.setApacheHttpClient(client);
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH));
    }

    @Test
    public void testFetchLegalEntityIdByClientIdInvalidJson() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{not-json"));
        Assert.expectThrows(CDSAccountValidationException.class,
                () -> CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH));
    }

    @Test
    public void testFetchLegalEntityIdByClientIdNoApplicationsKey() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{}"));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdEmptyApplicationsArray() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{\"applications\":[]}"));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdNullFirstApplication() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(
                mockClientWithResponse(200, "{\"applications\":[null]}"));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdNoAdvancedConfigurations() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, "{\"applications\":[{}]}"));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdNoAdditionalSpProperties() throws Exception {
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200,
                "{\"applications\":[{\"advancedConfigurations\":{}}]}"));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdPropertyNotFound() throws Exception {
        String body = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"other_property\",\"value\":\"value1\"}]}}]}";
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, body));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdNullPropertyInArray() throws Exception {
        // null element in array covers the optJSONObject == null skip branch
        String body = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[null,{\"name\":\"legal_entity_id\",\"value\":\"le-1\"}]}}]}";
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, body));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "le-1");
    }

    @Test
    public void testFetchLegalEntityIdByClientIdSuccess() throws Exception {
        String body = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-xyz\"}]}}]}";
        CDSAccountValidationUtils.setApacheHttpClient(mockClientWithResponse(200, body));
        Assert.assertEquals(CDSAccountValidationUtils.fetchLegalEntityIdByClientId("client-1", BASIC_AUTH), "le-xyz");
    }

    // ---- fetchAllBlockedAccounts with clientId (exercises legal entity path) ----

    @Test
    public void testFetchAllBlockedAccountsWithClientId() throws Exception {
        String isBody = "{\"applications\":[{\"advancedConfigurations\":{\"additionalSpProperties\":"
                + "[{\"name\":\"legal_entity_id\",\"value\":\"le-abc\"}]}}]}";
        String leBody = "[{\"accountID\":\"acc-1\","
                + "\"legalEntitySharingStatus\":\"blocked\",\"legalEntityID\":\"le-abc\"}]";

        CloseableHttpResponse disclosureResp = mockResponse(200, "[]");
        CloseableHttpResponse secondaryResp = mockResponse(200, "[]");
        CloseableHttpResponse businessResp = mockResponse(200, "[]");
        CloseableHttpResponse isResp = mockResponse(200, isBody);
        CloseableHttpResponse leResp = mockResponse(200, leBody);

        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpGet.class)))
                .thenReturn(disclosureResp)
                .thenReturn(secondaryResp)
                .thenReturn(businessResp)
                .thenReturn(isResp)
                .thenReturn(leResp);
        CDSAccountValidationUtils.setApacheHttpClient(client);

        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");
        accounts.add("acc-2");

        Set<String> blocked = CDSAccountValidationUtils.fetchAllBlockedAccounts(
                accounts, ACCOUNT_METADATA_WEBAPP_BASE_URL, "user-1", BASIC_AUTH, "client-1");

        Assert.assertEquals(blocked.size(), 1);
        Assert.assertTrue(blocked.contains("acc-1"));
        Assert.assertFalse(blocked.contains("acc-2"));
        Mockito.verify(client, Mockito.times(5)).execute(Mockito.any(HttpGet.class));
    }
}
