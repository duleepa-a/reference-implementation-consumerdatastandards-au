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
import org.wso2.openbanking.consumerdatastandards.account.metadata.constants.CommonConstants;
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

        if (request == null) {
            return badRequest("No legal entity sharing items provided");
        }

        try {
            List<LegalEntitySharingItem> validItems = validateRequest(request);
            if (validItems.isEmpty()) {
                return Response.status(Response.Status.OK).entity(new ArrayList<>()).build();
            }

            List<Pair<String, String>> accountUserPairs = buildAccountUserPairs(validItems);
            Map<Pair<String, String>, String> existingBlockedEntities =
                    accountMetadataService.getBatchSecondaryUserBlockedEntities(accountUserPairs);

            Map<Pair<String, String>, String> finalBlockedEntitiesByAccountUser = new LinkedHashMap<>();
            List<LegalEntitySharingItem> processedItems = new ArrayList<>();

            for (LegalEntitySharingItem item : validItems) {
                Pair<String, String> accountUserPair = Pair.of(item.getAccountID(), item.getSecondaryUserID());
                String originalCsv = normalizeBlockedEntities(existingBlockedEntities.get(accountUserPair));
                String baseCsv = finalBlockedEntitiesByAccountUser.containsKey(accountUserPair)
                        ? finalBlockedEntitiesByAccountUser.get(accountUserPair) : originalCsv;
                String updatedCsv = getUpdatedBlockedEntities(baseCsv, item);
                finalBlockedEntitiesByAccountUser.put(accountUserPair, updatedCsv);

                processedItems.add(item);
            }

            Map<Pair<String, String>, String> updates = new LinkedHashMap<>();
            Map<Pair<String, String>, String> inserts = new LinkedHashMap<>();

            for (Map.Entry<Pair<String, String>, String> entry : finalBlockedEntitiesByAccountUser.entrySet()) {
                Pair<String, String> accountUserPair = entry.getKey();
                String finalCsv = entry.getValue();

                if (existingBlockedEntities.containsKey(accountUserPair)) {
                    String originalCsv = normalizeBlockedEntities(existingBlockedEntities.get(accountUserPair));
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

            return Response.status(Response.Status.OK).entity(processedItems).build();

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
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
            List<Pair<String, String>> accountUserPairs = new ArrayList<>();
            String normalizedUserId = StringUtils.trimToEmpty(userId);
            for (String accountId : accountIdList) {
                accountUserPairs.add(Pair.of(accountId, normalizedUserId));
            }

            Map<Pair<String, String>, String> blockedEntitiesByAccountUser =
                    accountMetadataService.getBatchSecondaryUserBlockedEntities(accountUserPairs);

            List<LegalEntitySharingItem> responseItems = new ArrayList<>();
            for (Pair<String, String> accountUserPair : accountUserPairs) {
                if (!blockedEntitiesByAccountUser.containsKey(accountUserPair)) {
                    continue;
                }

                Set<String> blockedEntityIds = parseBlockedEntities(
                        blockedEntitiesByAccountUser.get(accountUserPair));

                if (blockedEntityIds.isEmpty()) {
                    LegalEntitySharingItem activeItem = new LegalEntitySharingItem();
                    activeItem.setSecondaryUserID(accountUserPair.getRight());
                    activeItem.setAccountID(accountUserPair.getLeft());
                    activeItem.setLegalEntityID("");
                    activeItem.setLegalEntitySharingStatus(CommonConstants.LEGAL_ENTITY_SHARING_STATUS_ACTIVE);
                    responseItems.add(activeItem);
                } else {
                    for (String blockedEntityId : blockedEntityIds) {
                        LegalEntitySharingItem blockedItem = new LegalEntitySharingItem();
                        blockedItem.setSecondaryUserID(accountUserPair.getRight());
                        blockedItem.setAccountID(accountUserPair.getLeft());
                        blockedItem.setLegalEntityID(blockedEntityId);
                        blockedItem.setLegalEntitySharingStatus(CommonConstants.LEGAL_ENTITY_SHARING_STATUS_BLOCKED);
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

    private static String getUpdatedBlockedEntities(String blockedEntitiesCsv, LegalEntitySharingItem item) {
        Set<String> blockedEntities = parseBlockedEntities(blockedEntitiesCsv);
        String legalEntityId = item.getLegalEntityID();
        String sharingStatus = StringUtils.trimToEmpty(item.getLegalEntitySharingStatus());

        if (CommonConstants.LEGAL_ENTITY_SHARING_STATUS_BLOCKED.equalsIgnoreCase(sharingStatus)) {
            blockedEntities.add(legalEntityId);
        } else {
            blockedEntities.remove(legalEntityId);
        }

        return String.join(",", blockedEntities);
    }

    private static List<LegalEntitySharingItem> validateRequest(List<LegalEntitySharingItem> request) {
        Map<String, LegalEntitySharingItem> deduplicatedItems = new LinkedHashMap<>();

        for (LegalEntitySharingItem item : request) {
            if (item == null) {
                throw new IllegalArgumentException("Request contains null legal entity sharing item");
            }

            String accountId = StringUtils.trimToEmpty(item.getAccountID());
            String secondaryUserId = StringUtils.trimToEmpty(item.getSecondaryUserID());
            String legalEntityId = StringUtils.trimToEmpty(item.getLegalEntityID());
            String sharingStatus = StringUtils.trimToEmpty(item.getLegalEntitySharingStatus());

            if (StringUtils.isBlank(accountId) || StringUtils.isBlank(secondaryUserId)
                    || StringUtils.isBlank(legalEntityId)) {
                throw new IllegalArgumentException("secondaryUserID, accountID and legalEntityID are required");
            }

            if (StringUtils.isBlank(sharingStatus)) {
                throw new IllegalArgumentException("legalEntitySharingStatus is required");
            }

            if (!isValidSharingStatus(sharingStatus)) {
                throw new IllegalArgumentException(
                        "Invalid legalEntitySharingStatus for accountID " + accountId +
                                " and secondaryUserID " + secondaryUserId + ": " + sharingStatus);
            }

            item.setAccountID(accountId);
            item.setSecondaryUserID(secondaryUserId);
            item.setLegalEntityID(legalEntityId);
            if (CommonConstants.LEGAL_ENTITY_SHARING_STATUS_BLOCKED.equalsIgnoreCase(sharingStatus)) {
                item.setLegalEntitySharingStatus(CommonConstants.LEGAL_ENTITY_SHARING_STATUS_BLOCKED);
            } else {
                item.setLegalEntitySharingStatus(CommonConstants.LEGAL_ENTITY_SHARING_STATUS_ACTIVE);
            }

            String dedupeKey = accountId + "::" + secondaryUserId + "::" + legalEntityId;
            deduplicatedItems.put(dedupeKey, item);
        }

        return new ArrayList<>(deduplicatedItems.values());
    }

    private static boolean isValidSharingStatus(String sharingStatus) {
        return CommonConstants.LEGAL_ENTITY_SHARING_STATUS_BLOCKED.equalsIgnoreCase(sharingStatus)
                || CommonConstants.LEGAL_ENTITY_SHARING_STATUS_ACTIVE.equalsIgnoreCase(sharingStatus);
    }

    private static List<Pair<String, String>> buildAccountUserPairs(List<LegalEntitySharingItem> items) {
        Map<String, Pair<String, String>> uniquePairs = new LinkedHashMap<>();
        for (LegalEntitySharingItem item : items) {
            String key = item.getAccountID() + "::" + item.getSecondaryUserID();
            uniquePairs.put(key, Pair.of(item.getAccountID(), item.getSecondaryUserID()));
        }
        return new ArrayList<>(uniquePairs.values());
    }

    private static Set<String> parseBlockedEntities(String blockedEntitiesCsv) {
        if (StringUtils.isBlank(blockedEntitiesCsv)) {
            return new LinkedHashSet<>();
        }

        return Arrays.stream(blockedEntitiesCsv.split(","))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeBlockedEntities(String blockedEntitiesCsv) {
        return String.join(",", parseBlockedEntities(blockedEntitiesCsv));
    }

    private static Response badRequest(String message) {
        log.error("[Legal Entity Sharing] " + message);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse().errorDescription(message))
                .build();
    }
}
