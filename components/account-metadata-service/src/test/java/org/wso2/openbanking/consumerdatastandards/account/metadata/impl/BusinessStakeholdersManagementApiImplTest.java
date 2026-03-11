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

package org.wso2.openbanking.consumerdatastandards.account.metadata.impl;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.openbanking.consumerdatastandards.account.metadata.exceptions.AccountMetadataException;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderDeleteItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderPermissionItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderRepresentative;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderUpsertItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.ErrorResponse;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataServiceImpl;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.dao.AccountMetadataDAO;
import org.wso2.openbanking.consumerdatastandards.account.metadata.utils.connection.provider.ConnectionProvider;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

/**
 * Unit tests for {@link BusinessStakeholdersManagementApiImpl}.
 */
public class BusinessStakeholdersManagementApiImplTest {

    private AccountMetadataDAO metadataDAO;
    private ConnectionProvider connectionProvider;
    private Connection connection;

    /**
     * Initializes static service dependencies for API tests.
     *
     * @throws Exception if class loading or singleton reset fails
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        metadataDAO = Mockito.mock(AccountMetadataDAO.class);
        connectionProvider = Mockito.mock(ConnectionProvider.class);
        connection = Mockito.mock(Connection.class);
        Mockito.when(connectionProvider.getConnection()).thenReturn(connection);

        resetSingleton();
        AccountMetadataServiceImpl.getInstance(metadataDAO, connectionProvider);
        Class.forName(BusinessStakeholdersManagementApiImpl.class.getName(), true,
                BusinessStakeholdersManagementApiImpl.class.getClassLoader());
    }

    /**
     * Resets mocks before each test.
     *
     * @throws Exception if mock setup fails
     */
    @BeforeMethod
    public void setUp() throws Exception {
        Mockito.reset(metadataDAO, connectionProvider, connection);
        Mockito.when(connectionProvider.getConnection()).thenReturn(connection);
    }

        /**
         * Verifies bad request response when add payload is null.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnNull() {
        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(null);

        assertBadRequest(response, "No business stakeholder items provided");
        }

        /**
         * Verifies bad request when add request contains null item.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnNullRequestItem() {
        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(null));

        assertBadRequest(response, "Request contains null business stakeholder item");
        }

        /**
         * Verifies bad request when account id is missing.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnMissingAccountId() {
        BusinessStakeholderUpsertItem item = buildUpsertItem(" ",
            buildRepresentative("user-1", "AUTHORIZE"));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "accountID is required");
        }

        /**
         * Verifies bad request when nominated representatives list is null.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnNullRepresentatives() {
        BusinessStakeholderUpsertItem item = new BusinessStakeholderUpsertItem();
        item.setAccountID("acc-1");
        item.setNominatedRepresentatives(null);

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "nominatedRepresentatives is required for accountID acc-1");
        }

        /**
         * Verifies bad request when representative entry is null.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnNullRepresentativeItem() {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1");
        item.setNominatedRepresentatives(Collections.singletonList(null));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "nominatedRepresentatives contains null item for accountID acc-1");
        }

        /**
         * Verifies bad request when representative name is missing.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnMissingRepresentativeName() {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative(" ", "AUTHORIZE"));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "Representative name is required for accountID acc-1");
        }

        /**
         * Verifies bad request when representative permission is missing.
         */
        @Test
        public void testAddBusinessStakeholdersBadRequestOnMissingRepresentativePermission() {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", " "));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "Representative permission is required for accountID acc-1 and user user-1");
        }

        /**
         * Verifies add returns OK with empty list when request has no representatives.
         */
        @Test
        public void testAddBusinessStakeholdersOkOnEmptyRepresentatives() throws AccountMetadataException {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1");

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertEquals(asStringList(response).size(), 0);
        Mockito.verify(metadataDAO, Mockito.never())
            .getBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        Mockito.verify(metadataDAO, Mockito.never())
            .addBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        }

        /**
         * Verifies create response when all requested business stakeholder records are new.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersCreatedWhenAllNew() throws Exception {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"),
            buildRepresentative("user-2", "VIEW"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        List<String> body = asStringList(response);
        Assert.assertEquals(body.size(), 1);
        Assert.assertEquals(body.get(0), "acc-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> captor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).addBatchBusinessStakeholderPermissions(Mockito.eq(connection), captor.capture());
        Assert.assertEquals(captor.getValue().size(), 2);
        }

        /**
         * Verifies add persists account owners with VIEW permission.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersCreatedWithAccountOwners() throws Exception {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1", Arrays.asList("owner-1", "owner-2"),
            buildRepresentative("user-1", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> captor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).addBatchBusinessStakeholderPermissions(Mockito.eq(connection), captor.capture());
        Assert.assertEquals(captor.getValue().size(), 3);
        Assert.assertTrue(captor.getValue().stream().anyMatch(itemToAdd ->
            "owner-1".equals(itemToAdd.getUserId()) && "VIEW".equals(itemToAdd.getPermission())));
        Assert.assertTrue(captor.getValue().stream().anyMatch(itemToAdd ->
            "owner-2".equals(itemToAdd.getUserId()) && "VIEW".equals(itemToAdd.getPermission())));
        Assert.assertTrue(captor.getValue().stream().anyMatch(itemToAdd ->
            "user-1".equals(itemToAdd.getUserId()) && "AUTHORIZE".equals(itemToAdd.getPermission())));
        }

        /**
         * Verifies duplicate account-user pairs in add payload are de-duplicated.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersDeduplicatesAccountUserPairs() throws Exception {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"),
            buildRepresentative("user-1", "VIEW"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> captor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).addBatchBusinessStakeholderPermissions(Mockito.eq(connection), captor.capture());
        Assert.assertEquals(captor.getValue().size(), 1);
        Assert.assertEquals(captor.getValue().get(0).getPermission(), "VIEW");
        }

        /**
         * Verifies ok response when all requested add records already exist.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersOkWhenAllExisting() throws Exception {
        BusinessStakeholderPermissionItem existing = buildItem("acc-1", "user-1", "AUTHORIZE");
        BusinessStakeholderUpsertItem requestItem = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertEquals(asStringList(response).size(), 0);
        Mockito.verify(metadataDAO, Mockito.never())
            .addBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        }

        /**
         * Verifies partial add behavior when some records already exist.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersCreatedWhenPartialExisting() throws Exception {
        BusinessStakeholderPermissionItem existing = buildItem("acc-1", "user-1", "AUTHORIZE");
        BusinessStakeholderUpsertItem requestItem = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"),
            buildRepresentative("user-2", "VIEW"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        List<String> body = asStringList(response);
        Assert.assertEquals(body.size(), 1);
        Assert.assertEquals(body.get(0), "acc-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> addCaptor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).addBatchBusinessStakeholderPermissions(Mockito.eq(connection), addCaptor.capture());
        Assert.assertEquals(addCaptor.getValue().size(), 1);
        Assert.assertEquals(addCaptor.getValue().get(0).getUserId(), "user-2");
        }

        /**
         * Verifies internal server error when add retrieval fails.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersServiceErrorOnGetBatch() throws Exception {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenThrow(new AccountMetadataException("fail"));

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to add business stakeholder records:"));
        }

        /**
         * Verifies internal server error when add persistence fails.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testAddBusinessStakeholdersServiceErrorOnAddBatch() throws Exception {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());
        Mockito.doThrow(new AccountMetadataException("fail"))
            .when(metadataDAO)
            .addBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList());

        Response response = BusinessStakeholdersManagementApiImpl.addBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to add business stakeholder records:"));
        }

    /**
     * Verifies bad request response when account and user ids are empty.
     */
    @Test
    public void testGetBusinessStakeholdersBadRequestOnEmpty() {
        Response response = BusinessStakeholdersManagementApiImpl.getBusinessStakeholders("", "");
        assertBadRequest(response, "At least one accountId and userId are required");
    }

    /**
     * Verifies bad request response when account and user ids are blank.
     */
    @Test
    public void testGetBusinessStakeholdersBadRequestOnBlank() {
        Response response = BusinessStakeholdersManagementApiImpl.getBusinessStakeholders("   ", "   ");
        assertBadRequest(response, "At least one accountId and userId are required");
    }

    /**
     * Verifies bad request response when account ids parse to an empty set.
     */
    @Test
    public void testGetBusinessStakeholdersBadRequestOnOnlyCommas() {
        Response response = BusinessStakeholdersManagementApiImpl.getBusinessStakeholders(" , , ", "user-1");
        assertBadRequest(response, "At least one accountId and userId are required");
    }

    /**
     * Verifies bad request response when user id is missing.
     */
    @Test
    public void testGetBusinessStakeholdersBadRequestOnMissingUserId() {
        Response response = BusinessStakeholdersManagementApiImpl
                .getBusinessStakeholders("acc-1,acc-2", " ");
        assertBadRequest(response, "At least one accountId and userId are required");
    }

    /**
     * Verifies successful retrieval of business stakeholder permissions.
     *
     * @throws Exception if setup or invocation fails
     */
    @Test
    public void testGetBusinessStakeholdersSuccess() throws Exception {
        List<BusinessStakeholderPermissionItem> batchResult = Arrays.asList(
                buildItem("acc-1", "user-1", "AUTHORIZE"),
                buildItem("acc-2", "user-1", "VIEW"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
                .thenReturn(batchResult);

        Response response = BusinessStakeholdersManagementApiImpl
                .getBusinessStakeholders("acc-1,acc-2", "user-1");

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        List<BusinessStakeholderPermissionItem> body = (List<BusinessStakeholderPermissionItem>) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertEquals(body.size(), 2);
        Assert.assertEquals(body.get(0).getAccountId(), "acc-1");
        Assert.assertEquals(body.get(0).getUserId(), "user-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> captor =
                (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                        ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).getBatchBusinessStakeholderPermissions(Mockito.eq(connection), captor.capture());
        Assert.assertEquals(captor.getValue().size(), 2);
        Assert.assertEquals(captor.getValue().get(0).getAccountId(), "acc-1");
        Assert.assertEquals(captor.getValue().get(0).getUserId(), "user-1");
        Assert.assertEquals(captor.getValue().get(1).getAccountId(), "acc-2");
        Assert.assertEquals(captor.getValue().get(1).getUserId(), "user-1");
    }

        /**
         * Verifies account ids are trimmed and blanks are ignored for get flow.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testGetBusinessStakeholdersTrimsAndFiltersAccountIds() throws Exception {
        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());

        Response response = BusinessStakeholdersManagementApiImpl
            .getBusinessStakeholders(" acc-1 , , acc-2 ", " user-1 ");

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> captor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).getBatchBusinessStakeholderPermissions(Mockito.eq(connection), captor.capture());
        Assert.assertEquals(captor.getValue().size(), 2);
        Assert.assertEquals(captor.getValue().get(0).getAccountId(), "acc-1");
        Assert.assertEquals(captor.getValue().get(1).getAccountId(), "acc-2");
        Assert.assertEquals(captor.getValue().get(0).getUserId(), "user-1");
        Assert.assertEquals(captor.getValue().get(1).getUserId(), "user-1");
        }

    /**
     * Verifies internal server error response when retrieval fails.
     *
     * @throws Exception if setup or invocation fails
     */
    @Test
    public void testGetBusinessStakeholdersServiceError() throws Exception {
        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(
                Mockito.eq(connection), Mockito.anyList()))
                .thenThrow(new AccountMetadataException("fail"));

        Response response = BusinessStakeholdersManagementApiImpl.getBusinessStakeholders(
                "acc-1", "user-1");

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to retrieve business stakeholder " +
                "permissions:"));
    }

        /**
         * Verifies bad request response when update payload is null.
         */
        @Test
        public void testUpdateBusinessStakeholdersBadRequestOnNull() {
        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(null);

        assertBadRequest(response, "No business stakeholder items provided");
        }

        /**
         * Verifies bad request when update payload has invalid representative data.
         */
        @Test
        public void testUpdateBusinessStakeholdersBadRequestOnMissingRepresentativeName() {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative(" ", "AUTHORIZE"));

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "Representative name is required for accountID acc-1");
        }

        /**
         * Verifies update returns OK with empty body when payload has no representatives.
         */
        @Test
        public void testUpdateBusinessStakeholdersOkOnEmptyRepresentatives() throws AccountMetadataException {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1");

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertEquals(asStringList(response).size(), 0);
        Mockito.verify(metadataDAO, Mockito.never())
            .updateBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        }

        /**
         * Verifies successful update when all requested records exist.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testUpdateBusinessStakeholdersSuccess() throws Exception {
        BusinessStakeholderPermissionItem existing = buildItem("acc-2", "user-2", "VIEW");
        BusinessStakeholderUpsertItem requestItem = buildUpsertItem("acc-2",
            buildRepresentative("user-2", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<String> body = asStringList(response);
        Assert.assertEquals(body.size(), 1);
        Assert.assertEquals(body.get(0), "acc-2");
        Mockito.verify(metadataDAO).updateBatchBusinessStakeholderPermissions(Mockito.eq(connection),
                Mockito.anyList());
        }

        /**
         * Verifies update returns OK without persistence when no records exist.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testUpdateBusinessStakeholdersOkWhenNoneExist() throws Exception {
        BusinessStakeholderUpsertItem requestItem = buildUpsertItem("acc-2",
            buildRepresentative("user-2", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertEquals(asStringList(response).size(), 0);
        Mockito.verify(metadataDAO, Mockito.never())
            .updateBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        }

        /**
         * Verifies update only persists existing subset in partial-existing scenario.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testUpdateBusinessStakeholdersOkWhenPartialExisting() throws Exception {
        BusinessStakeholderPermissionItem existing = buildItem("acc-2", "user-2", "VIEW");
        BusinessStakeholderUpsertItem requestItem = buildUpsertItem("acc-2",
            buildRepresentative("user-2", "AUTHORIZE"),
            buildRepresentative("user-3", "VIEW"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<String> body = asStringList(response);
        Assert.assertEquals(body.size(), 1);
        Assert.assertEquals(body.get(0), "acc-2");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> updateCaptor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).updateBatchBusinessStakeholderPermissions(Mockito.eq(connection),
            updateCaptor.capture());
        Assert.assertEquals(updateCaptor.getValue().size(), 1);
        Assert.assertEquals(updateCaptor.getValue().get(0).getUserId(), "user-2");
        }

        /**
         * Verifies internal server error when update retrieval fails.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testUpdateBusinessStakeholdersServiceErrorOnGetBatch() throws Exception {
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenThrow(new AccountMetadataException("fail"));

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to update business stakeholder records:"));
        }

        /**
         * Verifies internal server error when update persistence fails.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testUpdateBusinessStakeholdersServiceErrorOnUpdateBatch() throws Exception {
        BusinessStakeholderPermissionItem existing = buildItem("acc-1", "user-1", "VIEW");
        BusinessStakeholderUpsertItem item = buildUpsertItem("acc-1",
            buildRepresentative("user-1", "AUTHORIZE"));

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));
        Mockito.doThrow(new AccountMetadataException("fail"))
            .when(metadataDAO)
            .updateBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList());

        Response response = BusinessStakeholdersManagementApiImpl.updateBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to update business stakeholder records:"));
        }

        /**
         * Verifies bad request response when delete payload is null.
         */
        @Test
        public void testDeleteBusinessStakeholdersBadRequestOnNull() {
        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(null);

        assertBadRequest(response, "No business stakeholder items provided");
        }

        /**
         * Verifies bad request when delete request contains null item.
         */
        @Test
        public void testDeleteBusinessStakeholdersBadRequestOnNullRequestItem() {
        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(null));

        assertBadRequest(response, "Request contains null business stakeholder item");
        }

        /**
         * Verifies bad request when delete account id is missing.
         */
        @Test
        public void testDeleteBusinessStakeholdersBadRequestOnMissingAccountId() {
        BusinessStakeholderDeleteItem item = buildDeleteItem(" ", "user-1");

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "accountID is required");
        }

        /**
         * Verifies bad request when delete nominated representatives list is null.
         */
        @Test
        public void testDeleteBusinessStakeholdersBadRequestOnNullRepresentatives() {
        BusinessStakeholderDeleteItem item = new BusinessStakeholderDeleteItem();
        item.setAccountID("acc-1");
        item.setNominatedRepresentatives(null);

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "nominatedRepresentatives is required for accountID acc-1");
        }

        /**
         * Verifies bad request when delete nominated representative is blank.
         */
        @Test
        public void testDeleteBusinessStakeholdersBadRequestOnBlankRepresentative() {
        BusinessStakeholderDeleteItem item = buildDeleteItem("acc-1", " ");

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(item));

        assertBadRequest(response, "Representative name is required for accountID acc-1");
        }

        /**
         * Verifies delete returns OK and empty body when request has no representatives.
         */
        @Test
        public void testDeleteBusinessStakeholdersOkOnEmptyRepresentatives() throws AccountMetadataException {
        BusinessStakeholderDeleteItem item = buildDeleteItem("acc-1");

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertEquals(asStringList(response).size(), 0);
        Mockito.verify(metadataDAO, Mockito.never())
            .deleteBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        }

        /**
         * Verifies successful delete when all requested records exist.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testDeleteBusinessStakeholdersSuccess() throws Exception {
        BusinessStakeholderDeleteItem requestItem = buildDeleteItem("acc-3", "user-3");
        BusinessStakeholderPermissionItem existing = buildItem("acc-3", "user-3", "VIEW");

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<String> body = asStringList(response);
        Assert.assertEquals(body.size(), 1);
        Assert.assertEquals(body.get(0), "acc-3");
        Mockito.verify(metadataDAO).deleteBatchBusinessStakeholderPermissions(Mockito.eq(connection),
                Mockito.anyList());
        }

        /**
         * Verifies delete returns OK and does not persist when no records exist.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testDeleteBusinessStakeholdersOkWhenNoneExist() throws Exception {
        BusinessStakeholderDeleteItem requestItem = buildDeleteItem("acc-3", "user-3");

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.emptyList());

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertEquals(asStringList(response).size(), 0);
        Mockito.verify(metadataDAO, Mockito.never())
            .deleteBatchBusinessStakeholderPermissions(Mockito.any(Connection.class), Mockito.anyList());
        }

        /**
         * Verifies delete only removes existing subset in partial-existing scenario.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testDeleteBusinessStakeholdersOkWhenPartialExisting() throws Exception {
        BusinessStakeholderDeleteItem requestItem = buildDeleteItem("acc-3", "user-3", "user-4");
        BusinessStakeholderPermissionItem existing = buildItem("acc-3", "user-3", "VIEW");

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(requestItem));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<String> body = asStringList(response);
        Assert.assertEquals(body.size(), 1);
        Assert.assertEquals(body.get(0), "acc-3");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessStakeholderPermissionItem>> deleteCaptor =
            (ArgumentCaptor<List<BusinessStakeholderPermissionItem>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        Mockito.verify(metadataDAO).deleteBatchBusinessStakeholderPermissions(Mockito.eq(connection),
            deleteCaptor.capture());
        Assert.assertEquals(deleteCaptor.getValue().size(), 1);
        Assert.assertEquals(deleteCaptor.getValue().get(0).getUserId(), "user-3");
        }

        /**
         * Verifies internal server error when delete retrieval fails.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testDeleteBusinessStakeholdersServiceErrorOnGetBatch() throws Exception {
        BusinessStakeholderDeleteItem item = buildDeleteItem("acc-1", "user-1");

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenThrow(new AccountMetadataException("fail"));

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to delete business stakeholder records:"));
        }

        /**
         * Verifies internal server error when delete persistence fails.
         *
         * @throws Exception if setup or invocation fails
         */
        @Test
        public void testDeleteBusinessStakeholdersServiceErrorOnDeleteBatch() throws Exception {
        BusinessStakeholderDeleteItem item = buildDeleteItem("acc-1", "user-1");
        BusinessStakeholderPermissionItem existing = buildItem("acc-1", "user-1", "VIEW");

        Mockito.when(metadataDAO.getBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList()))
            .thenReturn(Collections.singletonList(existing));
        Mockito.doThrow(new AccountMetadataException("fail"))
            .when(metadataDAO)
            .deleteBatchBusinessStakeholderPermissions(Mockito.eq(connection), Mockito.anyList());

        Response response = BusinessStakeholdersManagementApiImpl.deleteBusinessStakeholders(
            Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertTrue(body.getErrorDescription().startsWith("Failed to delete business stakeholder records:"));
        }

    /**
     * Builds a business stakeholder permission item.
     */
    private BusinessStakeholderPermissionItem buildItem(String accountId, String userId, String permission) {
        BusinessStakeholderPermissionItem item = new BusinessStakeholderPermissionItem();
        item.setAccountId(accountId);
        item.setUserId(userId);
        item.setPermission(permission);
        return item;
    }

    /**
     * Builds a business stakeholder representative model.
     */
    private BusinessStakeholderRepresentative buildRepresentative(String name, String permission) {
        BusinessStakeholderRepresentative representative = new BusinessStakeholderRepresentative();
        representative.setName(name);
        representative.setPermission(permission);
        return representative;
    }

    /**
     * Builds an upsert request item.
     */
    private BusinessStakeholderUpsertItem buildUpsertItem(String accountId,
            BusinessStakeholderRepresentative... representatives) {

        return buildUpsertItem(accountId, new ArrayList<>(), representatives);
    }

    /**
     * Builds an upsert request item with account owners.
     */
    private BusinessStakeholderUpsertItem buildUpsertItem(String accountId, List<String> accountOwners,
            BusinessStakeholderRepresentative... representatives) {

        BusinessStakeholderUpsertItem item = new BusinessStakeholderUpsertItem();
        item.setAccountID(accountId);
        item.setAccountOwners(new ArrayList<>(accountOwners));
        item.setNominatedRepresentatives(new ArrayList<>(Arrays.asList(representatives)));
        return item;
    }

    /**
     * Builds a delete request item.
     */
    private BusinessStakeholderDeleteItem buildDeleteItem(String accountId, String... representativeUserIds) {
        BusinessStakeholderDeleteItem item = new BusinessStakeholderDeleteItem();
        item.setAccountID(accountId);
        item.setAccountOwners(new ArrayList<>());
        item.setNominatedRepresentatives(new ArrayList<>(Arrays.asList(representativeUserIds)));
        return item;
    }

    /**
     * Asserts a bad request response with expected error description.
     */
    private void assertBadRequest(Response response, String expectedDescription) {
        Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertEquals(body.getErrorDescription(), expectedDescription);
    }

    /**
     * Casts response entity to list of account ids.
     */
    @SuppressWarnings("unchecked")
    private List<String> asStringList(Response response) {
        return (List<String>) response.getEntity();
    }

    /**
     * Resets singleton state to isolate test execution.
     *
     * @throws Exception if reflection access fails
     */
    private void resetSingleton() throws Exception {
        Field instanceField = AccountMetadataServiceImpl.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
