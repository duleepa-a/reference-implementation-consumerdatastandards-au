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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.openbanking.consumerdatastandards.account.metadata.exceptions.AccountMetadataException;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderDeleteItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderPermissionItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderRepresentative;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderUpsertItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.ErrorResponse;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataService;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

/**
 * Implementation for business stakeholder management API operations.
 */
public class BusinessStakeholdersManagementApiImpl {

    private static final Log log = LogFactory.getLog(BusinessStakeholdersManagementApiImpl.class);

    private static final AccountMetadataService accountMetadataService = AccountMetadataServiceImpl.getInstance();

    private BusinessStakeholdersManagementApiImpl() {
        // Prevent instantiation
    }

    /**
     * Adds business stakeholder permission records for account-user combinations.
     *
     * @param request list of business stakeholder upsert records
     * @return response with list of account IDs where records were added
     */
    public static Response addBusinessStakeholders(List<BusinessStakeholderUpsertItem> request) {

        if (request == null) {
            log.error("[Business Stakeholders] No business stakeholder items provided");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse().errorDescription("No business stakeholder items provided"))
                    .build();
        }

        try {
            List<BusinessStakeholderPermissionItem> validItems = validateAndFlattenUpsertRequest(request);
            if (validItems.isEmpty()) {
                return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
            }

            List<BusinessStakeholderPermissionItem> existingItems =
                    accountMetadataService.getBatchBusinessStakeholderPermissions(validItems);
            Set<String> existingKeys = existingItems.stream().map(BusinessStakeholdersManagementApiImpl::buildKey)
                    .collect(Collectors.toSet());

            List<BusinessStakeholderPermissionItem> itemsToAdd = validItems.stream()
                    .filter(item -> !existingKeys.contains(buildKey(item)))
                    .collect(Collectors.toList());

            if (!itemsToAdd.isEmpty()) {
                accountMetadataService.addBatchBusinessStakeholderPermissions(itemsToAdd);
            }

            Response.ResponseBuilder responseBuilder = itemsToAdd.isEmpty() ?
                    Response.status(Response.Status.OK) : Response.status(Response.Status.CREATED);
            return responseBuilder.entity(getDistinctAccountIds(itemsToAdd)).build();

            } catch (IllegalArgumentException e) {
                return badRequest(e.getMessage());
        } catch (AccountMetadataException e) {
            log.error("[Business Stakeholders] Failed to add business stakeholder records", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to add business stakeholder records: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieves business stakeholder permissions for a single user and multiple accounts.
     *
     * @param accountIds comma-separated list of account IDs
     * @param userId user ID to retrieve permissions for
     * @return response with business stakeholder permission records
     */
    public static Response getBusinessStakeholders(String accountIds, String userId) {

        if (StringUtils.isBlank(accountIds) || StringUtils.isBlank(userId)) {
            return badRequest("At least one accountId and userId are required");
        }

        List<String> accountIdList = Arrays.stream(accountIds.split(","))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        if (accountIdList.isEmpty()) {
            return badRequest("At least one accountId and userId are required");
        }

        try {
            List<BusinessStakeholderPermissionItem> queryItems =
                    getBusinessStakeholderPermissionItems(userId, accountIdList);

            List<BusinessStakeholderPermissionItem> result =
                    accountMetadataService.getBatchBusinessStakeholderPermissions(queryItems);

            return Response.status(Response.Status.OK).entity(result).build();

        } catch (AccountMetadataException e) {
            log.error("[Business Stakeholders] Failed to retrieve business stakeholder permissions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to retrieve business stakeholder permissions: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Updates business stakeholder permission records for existing account-user combinations.
     *
     * @param request list of business stakeholder upsert records
     * @return response with list of account IDs where records were updated
     */
    public static Response updateBusinessStakeholders(List<BusinessStakeholderUpsertItem> request) {

        if (request == null) {
            return badRequest("No business stakeholder items provided");
        }

        try {
            List<BusinessStakeholderPermissionItem> validItems = validateAndFlattenUpsertRequest(request);
            if (validItems.isEmpty()) {
                return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
            }

            List<BusinessStakeholderPermissionItem> existingItems =
                    accountMetadataService.getBatchBusinessStakeholderPermissions(validItems);
            Set<String> existingKeys = existingItems.stream().map(BusinessStakeholdersManagementApiImpl::buildKey)
                    .collect(Collectors.toSet());

            List<BusinessStakeholderPermissionItem> itemsToUpdate = validItems.stream()
                    .filter(item -> existingKeys.contains(buildKey(item)))
                    .collect(Collectors.toList());

            if (!itemsToUpdate.isEmpty()) {
                accountMetadataService.updateBatchBusinessStakeholderPermissions(itemsToUpdate);
            }

            return Response.status(Response.Status.OK).entity(getDistinctAccountIds(itemsToUpdate)).build();

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccountMetadataException e) {
            log.error("[Business Stakeholders] Failed to update business stakeholder records", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to update business stakeholder records: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Deletes business stakeholder permission records for existing account-user combinations.
     *
     * @param request list of business stakeholder delete records
     * @return response with list of account IDs where records were deleted
     */
    public static Response deleteBusinessStakeholders(List<BusinessStakeholderDeleteItem> request) {

        if (request == null) {
            return badRequest("No business stakeholder items provided");
        }

        try {
            List<BusinessStakeholderPermissionItem> validItems = validateAndFlattenDeleteRequest(request);
            if (validItems.isEmpty()) {
                return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
            }

            List<BusinessStakeholderPermissionItem> existingItems =
                    accountMetadataService.getBatchBusinessStakeholderPermissions(validItems);
            Set<String> existingKeys = existingItems.stream().map(BusinessStakeholdersManagementApiImpl::buildKey)
                    .collect(Collectors.toSet());

            List<BusinessStakeholderPermissionItem> itemsToDelete = validItems.stream()
                    .filter(item -> existingKeys.contains(buildKey(item)))
                    .collect(Collectors.toList());

            if (!itemsToDelete.isEmpty()) {
                accountMetadataService.deleteBatchBusinessStakeholderPermissions(itemsToDelete);
            }

            return Response.status(Response.Status.OK).entity(getDistinctAccountIds(itemsToDelete)).build();

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AccountMetadataException e) {
            log.error("[Business Stakeholders] Failed to delete business stakeholder records", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to delete business stakeholder records: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Validates and flattens upsert payload into account-user-permission records.
     */
    private static List<BusinessStakeholderPermissionItem> validateAndFlattenUpsertRequest(
            List<BusinessStakeholderUpsertItem> request) {

        Map<String, BusinessStakeholderPermissionItem> deduplicatedItems = new LinkedHashMap<>();

        for (BusinessStakeholderUpsertItem requestItem : request) {
            if (requestItem == null) {
                throw new IllegalArgumentException("Request contains null business stakeholder item");
            }

            String accountId = StringUtils.trimToEmpty(requestItem.getAccountID());
            if (StringUtils.isBlank(accountId)) {
                throw new IllegalArgumentException("accountID is required");
            }

            List<String> accountOwners = requestItem.getAccountOwners();
            if (accountOwners != null) {
                // Add account owners with VIEW permission first so representative permissions can override them.
                for (String owner : accountOwners) {
                    String ownerId = StringUtils.trimToEmpty(owner);
                    if (StringUtils.isNotBlank(ownerId)) {
                        BusinessStakeholderPermissionItem permissionItem =
                                new BusinessStakeholderPermissionItem(accountId, ownerId, "VIEW");
                        deduplicatedItems.put(buildKey(permissionItem), permissionItem);
                    }
                }
            }

            // Add nominated representatives (may override owner VIEW with AUTHORIZE if same user)
            List<BusinessStakeholderRepresentative> nominatedRepresentatives =
                    requestItem.getNominatedRepresentatives();
            if (nominatedRepresentatives == null) {
                throw new IllegalArgumentException("nominatedRepresentatives is required for accountID " + accountId);
            }

            for (BusinessStakeholderRepresentative representative : nominatedRepresentatives) {
                if (representative == null) {
                    throw new IllegalArgumentException(
                            "nominatedRepresentatives contains null item for accountID " + accountId);
                }

                String userId = StringUtils.trimToEmpty(representative.getName());
                String permission = StringUtils.trimToEmpty(representative.getPermission());

                if (StringUtils.isBlank(userId)) {
                    throw new IllegalArgumentException("Representative name is required for accountID " + accountId);
                }
                if (StringUtils.isBlank(permission)) {
                    throw new IllegalArgumentException(
                            "Representative permission is required for accountID " + accountId +
                                    " and user " + userId);
                }

                BusinessStakeholderPermissionItem permissionItem =
                    new BusinessStakeholderPermissionItem(accountId, userId, permission);
                deduplicatedItems.put(buildKey(permissionItem), permissionItem);
            }
        }

        return new ArrayList<>(deduplicatedItems.values());
    }

    /**
     * Validates and flattens delete payload into account-user records.
     */
    private static List<BusinessStakeholderPermissionItem> validateAndFlattenDeleteRequest(
            List<BusinessStakeholderDeleteItem> request) {

        Map<String, BusinessStakeholderPermissionItem> deduplicatedItems = new LinkedHashMap<>();

        for (BusinessStakeholderDeleteItem requestItem : request) {
            if (requestItem == null) {
                throw new IllegalArgumentException("Request contains null business stakeholder item");
            }

            String accountId = StringUtils.trimToEmpty(requestItem.getAccountID());
            if (StringUtils.isBlank(accountId)) {
                throw new IllegalArgumentException("accountID is required");
            }

            List<String> nominatedRepresentatives = requestItem.getNominatedRepresentatives();
            if (nominatedRepresentatives == null) {
                throw new IllegalArgumentException("nominatedRepresentatives is required for accountID " + accountId);
            }

            for (String representative : nominatedRepresentatives) {
                String userId = StringUtils.trimToEmpty(representative);
                if (StringUtils.isBlank(userId)) {
                    throw new IllegalArgumentException(
                            "Representative name is required for accountID " + accountId);
                }

                BusinessStakeholderPermissionItem permissionItem =
                    new BusinessStakeholderPermissionItem(accountId, userId, null);
                deduplicatedItems.put(buildKey(permissionItem), permissionItem);
            }
        }

        return new ArrayList<>(deduplicatedItems.values());
    }

    /**
     * Builds query items for batch retrieval using a single user ID and a list of account IDs.
     */
    private static List<BusinessStakeholderPermissionItem> getBusinessStakeholderPermissionItems(
            String userId, List<String> accountIdList) {

        String normalizedUserId = StringUtils.trimToEmpty(userId);
        List<BusinessStakeholderPermissionItem> queryItems = new ArrayList<>();
        for (String accountId : accountIdList) {
            BusinessStakeholderPermissionItem queryItem = new BusinessStakeholderPermissionItem();
            queryItem.setAccountId(accountId);
            queryItem.setUserId(normalizedUserId);
            queryItems.add(queryItem);
        }
        return queryItems;
    }

    private static String buildKey(BusinessStakeholderPermissionItem item) {
        return item.getAccountId() + "::" + item.getUserId();
    }

    private static List<String> getDistinctAccountIds(List<BusinessStakeholderPermissionItem> items) {
        return items.stream().map(BusinessStakeholderPermissionItem::getAccountId).distinct()
                .collect(Collectors.toList());
    }

    private static Response badRequest(String message) {
        log.error("[Business Stakeholders] " + message);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse().errorDescription(message))
                .build();
    }
}
