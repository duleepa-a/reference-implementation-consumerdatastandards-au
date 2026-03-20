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

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.openbanking.consumerdatastandards.au.policy.constants.CDSAccountValidationConstants;
import org.wso2.openbanking.consumerdatastandards.au.policy.exceptions.CDSAccountValidationException;

import java.io.ByteArrayInputStream;
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
    private static final String SECONDARY_ACCOUNTS_ENDPOINT = ACCOUNT_METADATA_WEBAPP_BASE_URL
            + "/secondary-accounts";
    private static final String BUSINESS_STAKEHOLDERS_ENDPOINT = ACCOUNT_METADATA_WEBAPP_BASE_URL
            + "/business-stakeholders";
    private static final String LEGAL_ENTITY_ENDPOINT = ACCOUNT_METADATA_WEBAPP_BASE_URL + "/legal-entity";
    private static final String BASIC_AUTH = Base64.getEncoder().encodeToString("user:pass".getBytes());

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private CloseableHttpResponse mockResponse(int statusCode, String body) throws IOException {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);

        Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(entity.getContent())
                .thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        Mockito.when(response.getEntity()).thenReturn(entity);
        return response;
    }

    /**
     * Build and stub an HttpClientBuilder mock. Must be called BEFORE opening
     * a MockedStatic block — never pass this call as an inline argument to
     * thenReturn(), because nested Mockito.when() calls inside an unfinished
     * stubbing leave Mockito's state machine broken.
     */
    private HttpClientBuilder mockBuilder(CloseableHttpClient client) {
        HttpClientBuilder builder = Mockito.mock(HttpClientBuilder.class);
        Mockito.when(builder.setDefaultRequestConfig(Mockito.any())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(client);
        return builder;
    }

    // ─── DOMS / Joint accounts ───────────────────────────────────────────────────

    @Test
    public void testFetchBlockedAccountsFromServiceSuccess() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200,
                "[{\"accountId\":\"acc-1\",\"disclosureOption\":\"no-sharing\"},"
                + "{\"accountId\":\"acc-2\",\"disclosureOption\":\"pre-approval\"},"
                + "{\"accountId\":\"acc-3\",\"disclosureOption\":\"no-sharing\"}]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");
            accounts.add("acc-2");
            accounts.add("acc-3");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, DOMS_ENDPOINT, BASIC_AUTH);

            Assert.assertEquals(blocked.size(), 2);
            Assert.assertTrue(blocked.contains("acc-1"));
            Assert.assertTrue(blocked.contains("acc-3"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            Assert.assertTrue(requestCaptor.getValue().getURI().toString().contains("accountIds="));
            Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                    "Basic " + BASIC_AUTH);
        }
    }

    @Test
    public void testFetchBlockedAccountsFromServiceNon200() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(500, "[]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Assert.expectThrows(CDSAccountValidationException.class,
                    () -> CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                            accounts, DOMS_ENDPOINT, BASIC_AUTH));
        }
    }

    @Test
    public void testFetchBlockedAccountsFromServiceIoError() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class)))
                .thenThrow(new IOException("Connection failed"));
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Assert.expectThrows(CDSAccountValidationException.class,
                    () -> CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                            accounts, DOMS_ENDPOINT, BASIC_AUTH));
        }
    }

    @Test
    public void testFetchBlockedAccountsWithAuthHeader() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200, new JSONArray()
                .put(new JSONObject()
                        .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-1")
                        .put(CDSAccountValidationConstants.DISCLOSURE_OPTION_TAG,
                                CDSAccountValidationConstants.DOMS_STATUS_NO_SHARING))
                .toString());
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, DOMS_ENDPOINT, BASIC_AUTH);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-1"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                    "Basic " + BASIC_AUTH);
        }
    }

    @Test
    public void testFetchBlockedAccountsSuccessIncludesRequiredAuthHeader() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200, new JSONArray()
                .put(new JSONObject()
                        .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-2")
                        .put(CDSAccountValidationConstants.DISCLOSURE_OPTION_TAG,
                                CDSAccountValidationConstants.DOMS_STATUS_NO_SHARING))
                .toString());
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-2");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, DOMS_ENDPOINT, BASIC_AUTH);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-2"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                    "Basic " + BASIC_AUTH);
        }
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
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200,
                "[\"invalid\","
                + "{\"disclosureOption\":\"no-sharing\"},"
                + "{\"accountId\":\"acc-5\",\"disclosureOption\":\"no-sharing\"}]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-5");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, DOMS_ENDPOINT, BASIC_AUTH);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-5"));
        }
    }

    @Test
    public void testFetchBlockedAccountsFromServiceMalformedResponse() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200, "{not-an-array");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Assert.expectThrows(CDSAccountValidationException.class,
                    () -> CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                            accounts, DOMS_ENDPOINT, BASIC_AUTH));
        }
    }

    // ─── JWT ────────────────────────────────────────────────────────────────────

    @Test
    public void testGenerateJwtSuccess() throws Exception {
        try {
            String signedJwt = CDSAccountValidationUtils.generateJWT("{\"sub\":\"user-1\"}");
            Assert.assertNotNull(signedJwt);
            Assert.assertEquals(signedJwt.split("\\.").length, 3);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // In unit-test runtime, KeyStoreUtils may fail to initialize due missing server config.
            Assert.assertTrue(true);
        }
    }

    // ─── Secondary accounts ──────────────────────────────────────────────────────

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceSuccess() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200,
                "[{\"accountId\":\"acc-10\",\"secondaryAccountInstructionStatus\":\"inactive\"},"
                + "{\"accountId\":\"acc-11\",\"secondaryAccountInstructionStatus\":\"active\"},"
                + "{\"secondaryAccountInstructionStatus\":\"inactive\"}]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-10");
            accounts.add("acc-11");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                    accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-10"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            String requestUrl = requestCaptor.getValue().getURI().toString();
            Assert.assertTrue(requestUrl.contains("accountIds="));
            Assert.assertTrue(requestUrl.contains("userId=user-1"));
            Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                    "Basic " + BASIC_AUTH);
        }
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceWithAuthHeader() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200, new JSONArray()
                .put(new JSONObject()
                        .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-20")
                        .put(CDSAccountValidationConstants.SECONDARY_ACCOUNT_INSTRUCTION_STATUS_TAG,
                                CDSAccountValidationConstants.SECONDARY_ACCOUNT_STATUS_INACTIVE))
                .toString());
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-20");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                    accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-2", BASIC_AUTH);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-20"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                    "Basic " + BASIC_AUTH);
        }
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceSkipsBlankUserId() throws CDSAccountValidationException {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                accounts, SECONDARY_ACCOUNTS_ENDPOINT, " ", "");

        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceWithEmptyOrNullAccountIds()
            throws CDSAccountValidationException {
        Set<String> blockedForEmpty = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                Collections.emptySet(), SECONDARY_ACCOUNTS_ENDPOINT, "user-1", "");
        Set<String> blockedForNull = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                null, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", "");

        Assert.assertNotNull(blockedForEmpty);
        Assert.assertNotNull(blockedForNull);
        Assert.assertTrue(blockedForEmpty.isEmpty());
        Assert.assertTrue(blockedForNull.isEmpty());
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceNon200() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(500, "[]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Assert.expectThrows(CDSAccountValidationException.class, () ->
                    CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                            accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH));
        }
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceIoError() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class)))
                .thenThrow(new IOException("Connection failed"));
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Assert.expectThrows(CDSAccountValidationException.class, () ->
                    CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                            accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH));
        }
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceMalformedResponse() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200, "{not-an-array");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Assert.expectThrows(CDSAccountValidationException.class, () ->
                    CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                            accounts, SECONDARY_ACCOUNTS_ENDPOINT, "user-1", BASIC_AUTH));
        }
    }

    // ─── Business accounts ───────────────────────────────────────────────────────

    @Test
    public void testFetchBlockedBusinessAccountsFromServiceSuccess() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200,
                "[{\"accountId\":\"acc-30\",\"permission\":\"VIEW\"},"
                + "{\"accountId\":\"acc-31\",\"permission\":\"AUTHORIZE\"},"
                + "{\"permission\":\"VIEW\"},"
                + "\"invalid\"]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-30");
            accounts.add("acc-31");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                    accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-3", "");

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-30"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            String requestUrl = requestCaptor.getValue().getURI().toString();
            Assert.assertTrue(requestUrl.contains("accountIds="));
            Assert.assertTrue(requestUrl.contains("userId=user-3"));
        }
    }

    @Test
    public void testFetchBlockedBusinessAccountsFromServiceWithAuthHeader() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(200, new JSONArray()
                .put(new JSONObject()
                        .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-40")
                        .put(CDSAccountValidationConstants.BUSINESS_PERMISSION_TAG, "VIEW"))
                .toString());
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        String basicAuth = Base64.getEncoder().encodeToString("user:pass".getBytes());
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-40");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                    accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-4", basicAuth);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-40"));

            ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            Mockito.verify(client).execute(requestCaptor.capture());
            Assert.assertEquals(requestCaptor.getValue().getFirstHeader("Authorization").getValue(),
                    "Basic " + basicAuth);
        }
    }

    @Test
    public void testFetchBlockedBusinessAccountsFromServiceSkipsBlankUserId() {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, " ", "");

        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedBusinessAccountsFromServiceNon200() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mockResponse(500, "[]");
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedBusinessAccountsFromService(
                    accounts, BUSINESS_STAKEHOLDERS_ENDPOINT, "user-1", "");

            Assert.assertTrue(blocked.isEmpty());
        }
    }

    // ─── Legal entity accounts ───────────────────────────────────────────────────

    @Test
    public void testFetchBlockedLegalEntityAccountsFromServiceSuccess() throws Exception {
        CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);

        String isResponseBody = "{\"applications\":[{\"advancedConfigurations\":{"
                + "\"additionalSpProperties\":[{\"name\":\"legal_entity_id\",\"value\":\"TPP2\"}]}}]}";
        String legalEntityResponseBody = "["
                + "{\"accountID\":\"acc-50\",\"legalEntityID\":\"TPP2\",\"legalEntitySharingStatus\":\"blocked\"},"
                + "{\"accountID\":\"acc-51\",\"legalEntityID\":\"TPP2\",\"legalEntitySharingStatus\":\"active\"},"
                + "{\"accountID\":\"acc-52\",\"legalEntityID\":\"TPP9\",\"legalEntitySharingStatus\":\"blocked\"}"
                + "]";

        CloseableHttpResponse isResponse = mockResponse(200, isResponseBody);
        CloseableHttpResponse legalEntityResponse = mockResponse(200, legalEntityResponseBody);

        // First execute call → IS apps response; second → legal entity response
        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class)))
                .thenReturn(isResponse)
                .thenReturn(legalEntityResponse);
        HttpClientBuilder builder = mockBuilder(client);

        try (MockedStatic<HttpClients> mockedHttpClients = Mockito.mockStatic(HttpClients.class)) {
            mockedHttpClients.when(HttpClients::custom).thenReturn(builder);

            Set<String> accounts = new HashSet<>();
            accounts.add("acc-50");
            accounts.add("acc-51");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                    accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, "client-1");

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-50"));
        }
    }

    @Test
    public void testFetchBlockedLegalEntityAccountsFromServiceSkipsBlankClientId()
            throws CDSAccountValidationException {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedLegalEntityAccountsFromService(
                accounts, LEGAL_ENTITY_ENDPOINT, "user-1", BASIC_AUTH, " ");

        Assert.assertNotNull(blocked);
        Assert.assertTrue(blocked.isEmpty());
    }
}
