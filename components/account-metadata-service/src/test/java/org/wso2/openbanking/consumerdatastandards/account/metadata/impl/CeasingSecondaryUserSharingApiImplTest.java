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

import org.apache.commons.lang3.tuple.Pair;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.ErrorResponse;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.LegalEntitySharingItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataServiceImpl;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.dao.AccountMetadataDAO;
import org.wso2.openbanking.consumerdatastandards.account.metadata.utils.connection.provider.ConnectionProvider;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

/**
 * Unit tests for {@link CeasingSecondaryUserSharingApiImpl}.
 */
public class CeasingSecondaryUserSharingApiImplTest {

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
        Class.forName(CeasingSecondaryUserSharingApiImpl.class.getName(), true,
                CeasingSecondaryUserSharingApiImpl.class.getClassLoader());
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
     * Verifies blocked entity add/remove logic for existing records.
     *
     * @throws Exception if setup or invocation fails
     */
    @Test
    public void testUpdateLegalEntitySharingStatusUpdatesBlockedEntities() throws Exception {
        LegalEntitySharingItem blockRequest = buildItem("user-1", "acc-1", "le-003",
                LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked);
        LegalEntitySharingItem activeRequest = buildItem("user-1", "acc-2", "le-001",
                LegalEntitySharingItem.LegalEntitySharingStatusEnum.active);

        Map<Pair<String, String>, String> existing = new HashMap<>();
        existing.put(Pair.of("acc-1", "user-1"), "le-001,le-002");
        existing.put(Pair.of("acc-2", "user-1"), "le-001,le-004");

        Mockito.when(metadataDAO.getBatchSecondaryUserBlockedEntities(Mockito.eq(connection), Mockito.anyList()))
                .thenReturn(existing);

        Response response = CeasingSecondaryUserSharingApiImpl.updateLegalEntitySharingStatus(
                Arrays.asList(blockRequest, activeRequest));

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        List<LegalEntitySharingItem> body = (List<LegalEntitySharingItem>) response.getEntity();
        Assert.assertEquals(body.size(), 2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Pair<String, String>, String>> updatesCaptor =
                (ArgumentCaptor<Map<Pair<String, String>, String>>) (ArgumentCaptor<?>)
                        ArgumentCaptor.forClass(Map.class);
        Mockito.verify(metadataDAO).updateBatchSecondaryUserBlockedEntities(Mockito.eq(connection),
                updatesCaptor.capture());

        Map<Pair<String, String>, String> updates = updatesCaptor.getValue();
        Assert.assertEquals(updates.get(Pair.of("acc-1", "user-1")), "le-001,le-002,le-003");
        Assert.assertEquals(updates.get(Pair.of("acc-2", "user-1")), "le-004");
    }

    /**
     * Verifies missing account-user records are inserted during legal entity status update.
     *
     * @throws Exception if setup or invocation fails
     */
    @Test
    public void testUpdateLegalEntitySharingStatusAddsMissingRecord() throws Exception {
        LegalEntitySharingItem blockRequest = buildItem("user-10", "acc-10", "le-010",
                LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked);

        Mockito.when(metadataDAO.getBatchSecondaryUserBlockedEntities(Mockito.eq(connection), Mockito.anyList()))
                .thenReturn(Collections.emptyMap());

        Response response = CeasingSecondaryUserSharingApiImpl.updateLegalEntitySharingStatus(
                Collections.singletonList(blockRequest));

        Assert.assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Pair<String, String>, String>> insertsCaptor =
                (ArgumentCaptor<Map<Pair<String, String>, String>>) (ArgumentCaptor<?>)
                        ArgumentCaptor.forClass(Map.class);
        Mockito.verify(metadataDAO).addBatchSecondaryUserBlockedEntities(
                Mockito.eq(connection), insertsCaptor.capture());

        Map<Pair<String, String>, String> inserts = insertsCaptor.getValue();
        Assert.assertEquals(inserts.size(), 1);
        Assert.assertEquals(inserts.get(Pair.of("acc-10", "user-10")), "le-010");
        Mockito.verify(metadataDAO, Mockito.never())
                .updateBatchSecondaryUserBlockedEntities(Mockito.any(Connection.class), Mockito.anyMap());
    }

    /**
     * Verifies retrieval response mapping from BLOCKED_ENTITIES to API items.
     *
     * @throws Exception if setup or invocation fails
     */
    @Test
    public void testGetLegalEntitySharingStatusSuccess() throws Exception {
        Map<Pair<String, String>, String> existing = new HashMap<>();
        existing.put(Pair.of("acc-1", "user-1"), "le-001,le-002");
        existing.put(Pair.of("acc-2", "user-1"), "");

        Mockito.when(metadataDAO.getBatchSecondaryUserBlockedEntities(Mockito.eq(connection), Mockito.anyList()))
                .thenReturn(existing);

        Response response = CeasingSecondaryUserSharingApiImpl.getLegalEntitySharingStatus("acc-1,acc-2", "user-1");

        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        List<LegalEntitySharingItem> body = (List<LegalEntitySharingItem>) response.getEntity();
        Assert.assertEquals(body.size(), 3);

        Assert.assertTrue(body.stream().anyMatch(item ->
                "acc-1".equals(item.getAccountID())
                        && "le-001".equals(item.getLegalEntityID())
                        && LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked
                                .equals(item.getLegalEntitySharingStatus())));
        Assert.assertTrue(body.stream().anyMatch(item ->
                "acc-1".equals(item.getAccountID())
                        && "le-002".equals(item.getLegalEntityID())
                        && LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked
                                .equals(item.getLegalEntitySharingStatus())));
        Assert.assertTrue(body.stream().anyMatch(item ->
                "acc-2".equals(item.getAccountID())
                        && "".equals(item.getLegalEntityID())
                        && LegalEntitySharingItem.LegalEntitySharingStatusEnum.active
                                .equals(item.getLegalEntitySharingStatus())));
    }

    /**
     * Verifies bad request for missing query parameters.
     */
    @Test
    public void testGetLegalEntitySharingStatusBadRequest() {
        Response response = CeasingSecondaryUserSharingApiImpl.getLegalEntitySharingStatus("  ", " ");

        Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertEquals(body.getErrorDescription(), "At least one accountId and userId are required");
    }

    /**
     * Verifies bad request when required fields (secondaryUserID, accountID, legalEntityID) are blank.
     */
    @Test
    public void testUpdateLegalEntitySharingStatusBadRequestOnBlankRequiredFields() {
        LegalEntitySharingItem item = new LegalEntitySharingItem();
        item.setSecondaryUserID("   ");
        item.setAccountID("   ");
        item.setLegalEntityID("   ");
        item.setLegalEntitySharingStatus(LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked);

        Response response = CeasingSecondaryUserSharingApiImpl.updateLegalEntitySharingStatus(
                Collections.singletonList(item));

        Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        Assert.assertNotNull(body);
        Assert.assertEquals(body.getErrorDescription(),
                "secondaryUserID, accountID and legalEntityID are required");
    }

    private LegalEntitySharingItem buildItem(String secondaryUserId,
                                             String accountId,
                                             String legalEntityId,
                                             LegalEntitySharingItem.LegalEntitySharingStatusEnum status) {
        LegalEntitySharingItem item = new LegalEntitySharingItem();
        item.setSecondaryUserID(secondaryUserId);
        item.setAccountID(accountId);
        item.setLegalEntityID(legalEntityId);
        item.setLegalEntitySharingStatus(status);
        return item;
    }

    private void resetSingleton() throws Exception {
        Field instanceField = AccountMetadataServiceImpl.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
