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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.openbanking.consumerdatastandards.account.metadata.exceptions.AccountMetadataException;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.ErrorResponse;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.LegalEntitySharingItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataService;
import org.wso2.openbanking.consumerdatastandards.account.metadata.service.core.AccountMetadataServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

/**
 * Implementation for legal entity sharing API operations.
 */
public class CeasingSecondaryUserSharingApiImpl {

    private static final Log log = LogFactory.getLog(CeasingSecondaryUserSharingApiImpl.class);

    private static final AccountMetadataService accountMetadataService = AccountMetadataServiceImpl.getInstance();

    private CeasingSecondaryUserSharingApiImpl() {
        // Prevent instantiation
    }

    /**
     * Updates legal entity sharing statuses for one or more account-user records.
     *
     * @param request list of legal entity sharing records to update
     * @return response with list of processed records
     */
    public static Response updateLegalEntitySharingStatus(List<LegalEntitySharingItem> request) {

        List<LegalEntitySharingItem> validItems;
        try {
            validItems = validateRequest(request);
        } catch (AccountMetadataException e) {
            return sendBadRequest(e.getMessage());
        }

        try {
            if (validItems.isEmpty()) {
                return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
            }

            // Extract unique (accountId, userId) pairs 
            List<Pair<String, String>> accountUserPairs = buildAccountUserPairs(validItems);
            Map<Pair<String, String>, String> existingBlockedEntities =
                    accountMetadataService.getBatchSecondaryUserBlockedEntities(accountUserPairs);


            Map<Pair<String, String>, String> finalBlockedEntitiesByAccountUser = new LinkedHashMap<>();
            List<LegalEntitySharingItem> processedItems = new ArrayList<>();

            // Accumulate the final blocked entity CSV per account-user pair across all items in the request.
            // Using getOrDefault ensures that multiple items targeting the same pair are applied incrementally
            // on top of each other
            for (LegalEntitySharingItem item : validItems) {
                Pair<String, String> accountUserPair = Pair.of(item.getAccountID(), item.getSecondaryUserID());
                String originalCsv = existingBlockedEntities.get(accountUserPair);
                // Use the in-progress accumulated value 
                String baseCsv = finalBlockedEntitiesByAccountUser.getOrDefault(accountUserPair, originalCsv);
                String updatedCsv = getUpdatedBlockedEntities(baseCsv, item);

                // Update the accumulated value for this account-user pair
                finalBlockedEntitiesByAccountUser.put(accountUserPair, updatedCsv);

                processedItems.add(item);
            }

            Map<Pair<String, String>, String> updates = new LinkedHashMap<>();
            Map<Pair<String, String>, String> inserts = new LinkedHashMap<>();

            // Separate the final state into inserts (new records) and updates (existing records that changed)
            for (Map.Entry<Pair<String, String>, String> entry : finalBlockedEntitiesByAccountUser.entrySet()) {
                Pair<String, String> accountUserPair = entry.getKey();
                String finalCsv = entry.getValue();

                if (existingBlockedEntities.containsKey(accountUserPair)) {
                    // Only update if the blocked entity list actually changed
                    String originalCsv = existingBlockedEntities.get(accountUserPair);
                    if (!StringUtils.equals(finalCsv, originalCsv)) {
                        updates.put(accountUserPair, finalCsv);
                    }
                } else {
                    inserts.put(accountUserPair, finalCsv);
                }
            }

            if (!inserts.isEmpty()) {
                accountMetadataService.addBatchSecondaryUserBlockedEntities(inserts);
            }

            if (!updates.isEmpty()) {
                accountMetadataService.updateBatchSecondaryUserBlockedEntities(updates);
            }

            // Combine results and return 201 Created if any new items added, 200 OK otherwise
            Response.ResponseBuilder responseBuilder = inserts.isEmpty() ?
                    Response.status(Response.Status.OK) : Response.status(Response.Status.CREATED);

            return responseBuilder.entity(processedItems).build();

        } catch (AccountMetadataException e) {
            log.error("[Legal Entity Sharing] Failed to update legal entity sharing statuses", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to update legal entity sharing statuses: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Retrieves legal entity sharing statuses for one user and multiple accounts.
     *
     * @param accountIds comma-separated account IDs
     * @param userId user ID
     * @return response with legal entity sharing status records
     */
    public static Response getLegalEntitySharingStatus(String accountIds, String userId) {

        if (StringUtils.isBlank(accountIds) || StringUtils.isBlank(userId)) {
            return sendBadRequest("At least one accountId and userId are required");
        }

        // Split the comma-separated accountIds and strip whitespace from each token
        List<String> accountIdList = Arrays.stream(accountIds.split(",")).map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank).collect(Collectors.toList());

        try {
            List<Pair<String, String>> accountUserPairs = new ArrayList<>();
            String normalizedUserId = StringUtils.trimToEmpty(userId);

            // Build (accountId, userId) pairs for each requested account
            for (String accountId : accountIdList) {
                accountUserPairs.add(Pair.of(accountId, normalizedUserId));
            }

            Map<Pair<String, String>, String> blockedEntitiesByAccountUser =
                    accountMetadataService.getBatchSecondaryUserBlockedEntities(accountUserPairs);

            List<LegalEntitySharingItem> responseItems = new ArrayList<>();
            for (Pair<String, String> accountUserPair : accountUserPairs) {
                // Skip accounts that have no stored record
                if (!blockedEntitiesByAccountUser.containsKey(accountUserPair)) {
                    continue;
                }

                Set<String> blockedEntityIds = parseBlockedEntities(blockedEntitiesByAccountUser.get(accountUserPair));

                if (blockedEntityIds.isEmpty()) {
                    // A record exists but the blocked list is empty, meaning all entities are currently active
                    LegalEntitySharingItem activeItem = new LegalEntitySharingItem();
                    activeItem.setSecondaryUserID(accountUserPair.getRight());
                    activeItem.setAccountID(accountUserPair.getLeft());
                    activeItem.setLegalEntityID("");
                    activeItem.setLegalEntitySharingStatus(LegalEntitySharingItem.LegalEntitySharingStatusEnum.active);
                    responseItems.add(activeItem);
                } else {
                    // Return one response item per blocked legal entity
                    for (String blockedEntityId : blockedEntityIds) {
                        LegalEntitySharingItem blockedItem = new LegalEntitySharingItem();
                        blockedItem.setSecondaryUserID(accountUserPair.getRight());
                        blockedItem.setAccountID(accountUserPair.getLeft());
                        blockedItem.setLegalEntityID(blockedEntityId);
                        blockedItem.setLegalEntitySharingStatus(
                                LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked);
                        responseItems.add(blockedItem);
                    }
                }
            }

            return Response.status(Response.Status.OK).entity(responseItems).build();

        } catch (AccountMetadataException e) {
            log.error("[Legal Entity Sharing] Failed to retrieve legal entity sharing statuses", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse().errorDescription(
                            "Failed to retrieve legal entity sharing statuses: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Applies a single legal entity sharing item to the current blocked entities CSV, adding or removing
     * the legal entity ID depending on the requested sharing status.
     *
     * @param blockedEntitiesCsv current comma-separated list of blocked legal entity IDs (may be null or empty)
     * @param item               the sharing item describing the desired status change
     * @return updated comma-separated list of blocked legal entity IDs
     */
    private static String getUpdatedBlockedEntities(String blockedEntitiesCsv, LegalEntitySharingItem item) {
        Set<String> blockedEntities = parseBlockedEntities(blockedEntitiesCsv);
        String legalEntityId = item.getLegalEntityID();
        LegalEntitySharingItem.LegalEntitySharingStatusEnum sharingStatus = item.getLegalEntitySharingStatus();

        if (LegalEntitySharingItem.LegalEntitySharingStatusEnum.blocked.equals(sharingStatus)) {
            blockedEntities.add(legalEntityId);
        } else {
            blockedEntities.remove(legalEntityId);
        }

        return String.join(",", blockedEntities);
    }

    /**
     * Validates the incoming request items, ensuring that there are no duplicate
     * (accountID, secondaryUserID, legalEntityID) combinations.
     *
     * @param request raw list of legal entity sharing items from the caller
     * @return list of validated and trimmed items ready for processing
     * @throws AccountMetadataException if any item is missing required fields or a duplicate entry is detected
     */
    private static List<LegalEntitySharingItem> validateRequest(List<LegalEntitySharingItem> request)
            throws AccountMetadataException {
        Set<String> seenKeys = new LinkedHashSet<>();
        List<LegalEntitySharingItem> validatedItems = new ArrayList<>();

        for (LegalEntitySharingItem item : request) {

            String accountId = StringUtils.trimToEmpty(item.getAccountID());
            String secondaryUserId = StringUtils.trimToEmpty(item.getSecondaryUserID());
            String legalEntityId = StringUtils.trimToEmpty(item.getLegalEntityID());

            item.setAccountID(accountId);
            item.setSecondaryUserID(secondaryUserId);
            item.setLegalEntityID(legalEntityId);

            String dedupeKey = accountId + "::" + secondaryUserId + "::" + legalEntityId;
            if (!seenKeys.add(dedupeKey)) {
                throw new AccountMetadataException("Duplicate entry for accountID=" + accountId
                        + ", secondaryUserID=" + secondaryUserId + ", legalEntityID=" + legalEntityId);
            }
            validatedItems.add(item);
        }

        return validatedItems;
    }

    /**
     * Extracts unique (accountID, secondaryUserID) pairs from the given items, preserving insertion order.
     * Deduplication ensures each pair appears only once in the resulting batch service call.
     *
     * @param items list of legal entity sharing items
     * @return deduplicated list of account-user pairs
     */
    private static List<Pair<String, String>> buildAccountUserPairs(List<LegalEntitySharingItem> items) {
        Map<String, Pair<String, String>> uniquePairs = new LinkedHashMap<>();
        for (LegalEntitySharingItem item : items) {
            String key = item.getAccountID() + "::" + item.getSecondaryUserID();
            uniquePairs.put(key, Pair.of(item.getAccountID(), item.getSecondaryUserID()));
        }
        return new ArrayList<>(uniquePairs.values());
    }

    /**
     * Parses a comma-separated list of blocked legal entity IDs into an ordered set.
     * Blank tokens are ignored and each remaining value is trimmed of whitespace.
     *
     * @param blockedEntitiesCsv comma-separated string of blocked entity IDs (may be null or empty)
     * @return ordered set of non-blank entity IDs; empty set if input is blank
     */
    private static Set<String> parseBlockedEntities(String blockedEntitiesCsv) {
        if (StringUtils.isBlank(blockedEntitiesCsv)) {
            return new LinkedHashSet<>();
        }

        return Arrays.stream(blockedEntitiesCsv.split(",")).map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Logs the given message at error level and returns a 400 Bad Request response.
     *
     * @param message human-readable description of the validation error
     * @return 400 Bad Request response containing the error description
     */
    private static Response sendBadRequest(String message) {
        log.error("[Legal Entity Sharing] " + message);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse().errorDescription(message))
                .build();
    }
}
