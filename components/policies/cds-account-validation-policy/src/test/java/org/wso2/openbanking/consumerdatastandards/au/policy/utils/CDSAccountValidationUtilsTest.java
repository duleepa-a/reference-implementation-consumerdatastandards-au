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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.openbanking.consumerdatastandards.au.policy.constants.CDSAccountValidationConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link CDSAccountValidationUtils}.
 */
public class CDSAccountValidationUtilsTest {

    @Test
    public void testFetchBlockedAccountsFromServiceSuccess() throws Exception {
        HttpServer server = startBlockedAccountsServer(
                "["
                        + "{\"accountId\":\"acc-1\",\"disclosureOption\":\"no-sharing\"},"
                        + "{\"accountId\":\"acc-2\",\"disclosureOption\":\"pre-approval\"},"
                        + "{\"accountId\":\"acc-3\",\"disclosureOption\":\"no-sharing\"}"
                        + "]");
        try {
            String serverUrl = "http://localhost:" + server.getAddress().getPort() + "/blocked";
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");
            accounts.add("acc-2");
            accounts.add("acc-3");

                Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, serverUrl, "");

            Assert.assertEquals(blocked.size(), 2);
            Assert.assertTrue(blocked.contains("acc-1"));
            Assert.assertTrue(blocked.contains("acc-3"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchBlockedAccountsFromServiceNon200() throws Exception {
        HttpServer server = startBlockedAccountsServerWithStatus(
                "[]", 500);
        try {
            String serverUrl = "http://localhost:" + server.getAddress().getPort() + "/blocked";
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

                Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, serverUrl, "");

            Assert.assertTrue(blocked.isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchBlockedAccountsFromServiceIoError() {
        Set<String> accounts = new HashSet<>();
        accounts.add("acc-1");

        Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                accounts, "http://localhost:1/blocked", "");

        Assert.assertTrue(blocked.isEmpty());
    }

    @Test
    public void testFetchBlockedAccountsWithAuthHeader() throws Exception {
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes());
        HttpServer server = startAuthVerifyingServer(expectedAuth);
        try {
            String serverUrl = "http://localhost:" + server.getAddress().getPort() + "/blocked";
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            String basicAuth = Base64.getEncoder().encodeToString("user:pass".getBytes());
                Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, serverUrl, basicAuth);

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-1"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchBlockedAccountsWithoutAuthHeader() throws Exception {
        HttpServer server = startAuthVerifyingServer(null);
        try {
            String serverUrl = "http://localhost:" + server.getAddress().getPort() + "/blocked";
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-2");

                Set<String> blocked = CDSAccountValidationUtils.fetchBlockedJointAccountsFromService(
                    accounts, serverUrl, "");

            Assert.assertEquals(blocked.size(), 1);
            Assert.assertTrue(blocked.contains("acc-2"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchBlockedSecondaryAccountsFromServiceSuccess() throws Exception {
        String expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        HttpServer server = startSecondaryAccountsServer(expectedAuth, "user-123", null);
        try {
            String serverUrl = "http://localhost:" + server.getAddress().getPort() + "/secondary-accounts";
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");
            accounts.add("acc-2");
            accounts.add("acc-3");

            String basicAuth = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                    accounts, serverUrl, "user-123", basicAuth);

            Assert.assertEquals(blocked.size(), 2);
            Assert.assertTrue(blocked.contains("acc-1"));
            Assert.assertTrue(blocked.contains("acc-3"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchBlockedSecondaryAccountsSkipsWhenUserIdBlank() throws Exception {
        AtomicInteger callCounter = new AtomicInteger(0);
        HttpServer server = startSecondaryAccountsServer(null, null, callCounter);
        try {
            String serverUrl = "http://localhost:" + server.getAddress().getPort() + "/secondary-accounts";
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");

            Set<String> blocked = CDSAccountValidationUtils.fetchBlockedSecondaryAccountsFromService(
                    accounts, serverUrl, "", "");

            Assert.assertTrue(blocked.isEmpty());
            Assert.assertEquals(callCounter.get(), 0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchAllBlockedAccountsCombinesJointAndSecondary() throws Exception {
        String expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        HttpServer server = startCombinedMetadataServer(expectedAuth, "user-123");
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            Set<String> accounts = new HashSet<>();
            accounts.add("acc-1");
            accounts.add("acc-2");
            accounts.add("acc-3");

            String basicAuth = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
            Set<String> blocked = CDSAccountValidationUtils.fetchAllBlockedAccounts(
                    accounts, baseUrl, "user-123", basicAuth);

            Assert.assertEquals(blocked.size(), 2);
            Assert.assertTrue(blocked.contains("acc-1"));
            Assert.assertTrue(blocked.contains("acc-3"));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startAuthVerifyingServer(String expectedAuthHeader) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/blocked", new AuthVerifyingHandler(expectedAuthHeader));
        server.setExecutor(null);
        server.start();
        return server;
    }

    private static HttpServer startBlockedAccountsServer(String responseBody) throws IOException {
        return startBlockedAccountsServerWithStatus(responseBody, 200);
    }

    private static HttpServer startBlockedAccountsServerWithStatus(String responseBody, int statusCode)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/blocked", new BlockedAccountsHandler(responseBody, statusCode));
        server.setExecutor(null);
        server.start();
        return server;
    }

        private static HttpServer startSecondaryAccountsServer(String expectedAuthHeader, String expectedUserId,
                                   AtomicInteger callCounter) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/secondary-accounts",
            new SecondaryAccountsHandler(expectedAuthHeader, expectedUserId, callCounter));
        server.setExecutor(null);
        server.start();
        return server;
        }

        private static HttpServer startCombinedMetadataServer(String expectedAuthHeader, String expectedUserId)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/disclosure-options", new BlockedAccountsHandler(
            "[{\"accountId\":\"acc-1\",\"disclosureOption\":\"no-sharing\"},"
                + "{\"accountId\":\"acc-2\",\"disclosureOption\":\"pre-approval\"}]", 200));
        server.createContext("/secondary-accounts",
            new SecondaryAccountsHandler(expectedAuthHeader, expectedUserId, null));
        server.setExecutor(null);
        server.start();
        return server;
        }

    private static class BlockedAccountsHandler implements HttpHandler {
        private final String responseBody;
        private final int statusCode;

        private BlockedAccountsHandler(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Assert.assertEquals(exchange.getRequestMethod(), "GET");
            String query = exchange.getRequestURI().getRawQuery();
            Assert.assertNotNull(query);
            Assert.assertTrue(query.contains(CDSAccountValidationConstants.ACCOUNT_IDS_TAG + "="));

            try (InputStream requestBody = exchange.getRequestBody()) {
                while (requestBody.read() != -1) {
                    // Consume request body to avoid client-side issues.
                }
            }
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream responseStream = exchange.getResponseBody()) {
                responseStream.write(responseBytes);
            }
        }
    }

    private static class AuthVerifyingHandler implements HttpHandler {
        private final String expectedAuthHeader;

        private AuthVerifyingHandler(String expectedAuthHeader) {
            this.expectedAuthHeader = expectedAuthHeader;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Assert.assertEquals(exchange.getRequestMethod(), "GET");
            String query = exchange.getRequestURI().getRawQuery();
            Assert.assertNotNull(query);
            Assert.assertTrue(query.contains(CDSAccountValidationConstants.ACCOUNT_IDS_TAG + "="));

            try (InputStream requestBody = exchange.getRequestBody()) {
                while (requestBody.read() != -1) {
                    // Consume request body
                }
            }

            // Verify auth header is set correctly
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (expectedAuthHeader != null) {
                Assert.assertEquals(authHeader, expectedAuthHeader, "Authorization header mismatch");
            } else {
                Assert.assertNull(authHeader, "Authorization header should not be present");
            }

            JSONArray response = new JSONArray()
                    .put(new JSONObject()
                        .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG,
                                    expectedAuthHeader != null ? "acc-1" : "acc-2")
                        .put(CDSAccountValidationConstants.DISCLOSURE_OPTION_TAG,
                            CDSAccountValidationConstants.DOMS_STATUS_NO_SHARING));
            byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream responseStream = exchange.getResponseBody()) {
                responseStream.write(responseBytes);
            }
        }
    }

    private static class SecondaryAccountsHandler implements HttpHandler {
        private final String expectedAuthHeader;
        private final String expectedUserId;
        private final AtomicInteger callCounter;

        private SecondaryAccountsHandler(String expectedAuthHeader, String expectedUserId, AtomicInteger callCounter) {
            this.expectedAuthHeader = expectedAuthHeader;
            this.expectedUserId = expectedUserId;
            this.callCounter = callCounter;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (callCounter != null) {
                callCounter.incrementAndGet();
            }

            Assert.assertEquals(exchange.getRequestMethod(), "GET");
            String query = exchange.getRequestURI().getRawQuery();
            Assert.assertNotNull(query);
            Assert.assertTrue(query.contains(CDSAccountValidationConstants.ACCOUNT_IDS_TAG + "="));

            if (expectedUserId != null) {
                Assert.assertTrue(query.contains(CDSAccountValidationConstants.USER_ID_TAG + "="));
                Assert.assertTrue(query.contains(expectedUserId));
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (expectedAuthHeader != null) {
                Assert.assertEquals(authHeader, expectedAuthHeader, "Authorization header mismatch");
            } else {
                Assert.assertNull(authHeader, "Authorization header should not be present");
            }

            JSONArray response = new JSONArray()
                    .put(new JSONObject()
                            .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-1")
                            .put(CDSAccountValidationConstants.SECONDARY_ACCOUNT_INSTRUCTION_STATUS_TAG,
                                    CDSAccountValidationConstants.SECONDARY_ACCOUNT_STATUS_INACTIVE))
                    .put(new JSONObject()
                            .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-2")
                            .put(CDSAccountValidationConstants.SECONDARY_ACCOUNT_INSTRUCTION_STATUS_TAG, "active"))
                    .put(new JSONObject()
                            .put(CDSAccountValidationConstants.CDS_ACCOUNT_ID_TAG, "acc-3")
                            .put(CDSAccountValidationConstants.SECONDARY_ACCOUNT_INSTRUCTION_STATUS_TAG,
                                    CDSAccountValidationConstants.SECONDARY_ACCOUNT_STATUS_INACTIVE));

            byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream responseStream = exchange.getResponseBody()) {
                responseStream.write(responseBytes);
            }
        }
    }
}
