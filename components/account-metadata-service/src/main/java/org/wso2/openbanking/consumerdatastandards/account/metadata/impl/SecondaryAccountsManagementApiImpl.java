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
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.ModelApiResponse;
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
     * @return response with update status
     */
    public static Response updateSecondaryAccountInstructions(List<SecondaryAccountInstructionItem> request) {

        if (request == null || request.isEmpty()) {
            log.error("[Secondary Accounts] No secondary account instruction items provided to update");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ModelApiResponse().message("No secondary account instruction items provided"))
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

            List<String> nonExistingKeys = validItems.stream()
                    .filter(item -> !existingKeys.contains(buildCompositeKey(item)))
                    .map(SecondaryAccountsManagementApiImpl::buildCompositeKey)
                    .collect(Collectors.toList());

            if (!itemsToUpdate.isEmpty()) {
                accountMetadataService.updateBatchSecondaryAccountInstructions(itemsToUpdate);
            }

            if (nonExistingKeys.isEmpty()) {
                return Response.ok()
                        .entity(new ModelApiResponse().message("Secondary account instructions updated successfully"))
                        .build();
            }

            if (itemsToUpdate.isEmpty()) {
                return Response.ok()
                        .entity(new ModelApiResponse().message("No secondary account instructions were updated. " +
                                "AccountId-UserId record(s) do not exist: " +
                                String.join(", ", nonExistingKeys)))
                        .build();
            }

            return Response.ok()
                    .entity(new ModelApiResponse().message("Secondary account instructions updated successfully for " +
                            "existing records. AccountId-UserId record(s) do not exist: " +
                            String.join(", ", nonExistingKeys)))
                    .build();

        } catch (IllegalArgumentException e) {
            log.error("[Secondary Accounts] Invalid request received", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ModelApiResponse().message(e.getMessage()))
                    .build();
        } catch (AccountMetadataException e) {
            log.error("[Secondary Accounts] Failed to update secondary account instructions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ModelApiResponse().message(
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
                    .entity(new ModelApiResponse().message(
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
                    .entity(new ModelApiResponse().message(
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
                        .entity(new ModelApiResponse().message(
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
     * @return response with creation status
     */
    public static Response addSecondaryAccountInstructions(List<SecondaryAccountInstructionItem> request) {

        if (request == null || request.isEmpty()) {
            log.error("[Secondary Accounts] No secondary account instruction items provided to add");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ModelApiResponse().message("No secondary account instruction items provided"))
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

            List<String> existingItemKeys = validItems.stream()
                    .filter(item -> existingKeys.contains(buildCompositeKey(item)))
                    .map(SecondaryAccountsManagementApiImpl::buildCompositeKey)
                    .collect(Collectors.toList());

            if (!itemsToAdd.isEmpty()) {
                accountMetadataService.addBatchSecondaryAccountInstructions(itemsToAdd);
            }

            if (!existingItemKeys.isEmpty()) {
                return Response.ok()
                        .entity(new ModelApiResponse().message(
                                "Secondary account instructions added for new records. " +
                                        "Already exists for AccountId-UserId record(s): " +
                                        String.join(", ", existingItemKeys)))
                        .build();
            }

            return Response.status(Response.Status.CREATED)
                    .entity(new ModelApiResponse().message("Secondary account instructions added successfully"))
                    .build();

        } catch (AccountMetadataException e) {
            log.error("[Secondary Accounts] Failed to add secondary account instructions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ModelApiResponse().message(
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
