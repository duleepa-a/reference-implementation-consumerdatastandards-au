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
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.ErrorResponse;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.SecondaryAccountInstructionItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataService;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

/**
 * Implementation for secondary account instruction management API operations.
 */
public class SecondaryAccountsManagementApiImpl {

    private static final Log log = LogFactory.getLog(SecondaryAccountsManagementApiImpl.class);

    private static final AccountMetadataService accountMetadataService = AccountMetadataServiceImpl.getInstance();

    private SecondaryAccountsManagementApiImpl() {
        // Prevent instantiation
    }

    /**
     * Updates secondary account instructions for multiple account-user combinations.
     *
     * @param request list of secondary account instruction items to update
     * @return response with list of updated items
     */
    public static Response updateSecondaryAccountInstructions(List<SecondaryAccountInstructionItem> request) {

        if (request == null) {
            log.error("[Secondary Accounts] No secondary account instruction items provided to update");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse().errorDescription("No secondary account instruction items provided"))
                    .build();
        }

        try {
            List<SecondaryAccountInstructionItem> validItems = validateAndNormalizeRequest(request);

            List<SecondaryAccountInstructionItem> existingItems =
                    accountMetadataService.getBatchSecondaryAccountInstructions(validItems);
            Set<String> existingKeys = existingItems.stream()
                    .map(SecondaryAccountsManagementApiImpl::buildCompositeKey)
                    .collect(Collectors.toSet());

            List<SecondaryAccountInstructionItem> itemsToUpdate = validItems.stream()
                    .filter(item -> existingKeys.contains(buildCompositeKey(item)))
                    .collect(Collectors.toList());

            if (!itemsToUpdate.isEmpty()) {
                accountMetadataService.updateBatchSecondaryAccountInstructions(itemsToUpdate);
            }

            if (log.isDebugEnabled()) {
                log.debug("[Secondary Accounts] Updated secondary account instructions for " +
                        itemsToUpdate.size() + " item(s)");
            }

            return Response.status(Response.Status.OK).entity(itemsToUpdate).build();

        } catch (AccountMetadataException e) {
            log.error("[Secondary Accounts] Failed to update secondary account instructions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to update secondary account instructions: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieves secondary account instructions for a single user and multiple accounts.
     *
     * @param accountIds comma-separated list of account IDs
     * @param userId user ID to retrieve instruction statuses for
     * @return response with secondary account instruction records
     */
    public static Response getSecondaryAccountInstructions(String accountIds, String userId) {

        if (StringUtils.isBlank(accountIds) || StringUtils.isBlank(userId)) {
            log.error("[Secondary Accounts] accountIds or userId are missing in get request");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse().errorDescription(
                            "At least one accountId and userId are required"))
                    .build();
        }

        List<String> accountIdList = Arrays.stream(accountIds.split(","))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        if (accountIdList.isEmpty()) {
            log.error("[Secondary Accounts] No valid accountIds found after parsing");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse().errorDescription(
                            "At least one accountId and userId are required"))
                    .build();
        }

        try {
            List<SecondaryAccountInstructionItem> queryItems =
                    getSecondaryAccountInstructionItems(userId, accountIdList);

            List<SecondaryAccountInstructionItem> result =
                                accountMetadataService.getBatchSecondaryAccountInstructions(queryItems);

            return Response.status(Response.Status.OK).entity(result).build();

        } catch (AccountMetadataException e) {
                log.error("[Secondary Accounts] Error retrieving secondary account instructions", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse().errorDescription(
                                "Failed to retrieve secondary account instructions: " + e.getMessage()))
                        .build();
        }
    }

    private static List<SecondaryAccountInstructionItem> getSecondaryAccountInstructionItems(
            String userId, List<String> accountIdList) {

        String normalizedUserId = StringUtils.trimToEmpty(userId);
        List<SecondaryAccountInstructionItem> queryItems = new ArrayList<>();
        for (String accountId : accountIdList) {
                SecondaryAccountInstructionItem queryItem = new SecondaryAccountInstructionItem();
                queryItem.setAccountId(accountId);
                queryItem.setSecondaryUserId(normalizedUserId);
                queryItems.add(queryItem);
        }
        return queryItems;
    }

    /**
     * Adds secondary account instructions for multiple account-user combinations.
     *
     * @param request list of secondary account instruction items to add
     * @return response with list of added items
     */
    public static Response addSecondaryAccountInstructions(List<SecondaryAccountInstructionItem> request) {

        if (request == null) {
            log.error("[Secondary Accounts] No secondary account instruction items provided to add");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse().errorDescription("No secondary account instruction items provided"))
                    .build();
        }

        try {
            List<SecondaryAccountInstructionItem> validItems = validateAndNormalizeRequest(request);

            List<SecondaryAccountInstructionItem> existingItems =
                    accountMetadataService.getBatchSecondaryAccountInstructions(validItems);
            Set<String> existingKeys = existingItems.stream()
                    .map(SecondaryAccountsManagementApiImpl::buildCompositeKey)
                    .collect(Collectors.toSet());

            List<SecondaryAccountInstructionItem> itemsToAdd = validItems.stream()
                    .filter(item -> !existingKeys.contains(buildCompositeKey(item)))
                    .collect(Collectors.toList());

            if (!itemsToAdd.isEmpty()) {
                accountMetadataService.addBatchSecondaryAccountInstructions(itemsToAdd);
            }

            if (log.isDebugEnabled()) {
                log.debug("[Secondary Accounts] Added secondary account instructions for " +
                        itemsToAdd.size() + " item(s)");
            }

            // Return 201 Created if all were new, 200 OK if some already existed
            Response.ResponseBuilder responseBuilder = itemsToAdd.isEmpty() ? 
                    Response.status(Response.Status.OK) : Response.status(Response.Status.CREATED);
            
            return responseBuilder.entity(itemsToAdd).build();

        } catch (AccountMetadataException e) {
            log.error("[Secondary Accounts] Failed to add secondary account instructions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to add secondary account instructions: " + e.getMessage()))
                    .build();
        }
    }

    private static List<SecondaryAccountInstructionItem> validateAndNormalizeRequest(
            List<SecondaryAccountInstructionItem> request) throws AccountMetadataException {

        Map<String, SecondaryAccountInstructionItem> deduplicatedItems = new HashMap<>();

        for (SecondaryAccountInstructionItem item : request) {
            if (item == null) {
                throw new AccountMetadataException("Request contains null secondary account instruction item");
            }
            String accountId = StringUtils.trimToEmpty(item.getAccountId());
            String secondaryUserId = StringUtils.trimToEmpty(item.getSecondaryUserId());
            Boolean otherAccountsAvailability = item.getOtherAccountsAvailablitiy();
            String instructionStatus = item.getSecondaryAccountInstructionStatus();

            if (StringUtils.isBlank(accountId) || StringUtils.isBlank(secondaryUserId)) {
                throw new AccountMetadataException("accountId and secondaryUserId are required");
            }
            if (otherAccountsAvailability == null) {
                throw new AccountMetadataException(
                        "otherAccountsAvailablitiy is required for accountId " + accountId +
                                " and secondaryUserId " + secondaryUserId);
            }
            if (instructionStatus == null) {
                throw new AccountMetadataException(
                        "secondaryAccountInstructionStatus is required for accountId " + accountId +
                                " and secondaryUserId " + secondaryUserId);
            }

            item.setAccountId(accountId);
            item.setSecondaryUserId(secondaryUserId);

            deduplicatedItems.put(buildCompositeKey(item), item);
        }

        return new ArrayList<>(deduplicatedItems.values());
    }

    private static String buildCompositeKey(SecondaryAccountInstructionItem item) {
        return item.getAccountId() + "::" + item.getSecondaryUserId();
    }
}
