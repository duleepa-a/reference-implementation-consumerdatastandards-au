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

package org.wso2.openbanking.consumerdatastandards.account.metadata.service.dao.queries;

import org.wso2.openbanking.consumerdatastandards.account.metadata.model.BusinessStakeholderPermissionItem;
import org.wso2.openbanking.consumerdatastandards.account.metadata.model.SecondaryAccountInstructionItem;

import java.util.List;

/**
 * Database queries for account metadata operations.
 */
public interface AccountMetadataDbQueries {

    // DB Queries related to DOMS Feature.
    /**
     * Get the SQL query for retrieving disclosure options for multiple accounts.
     * Uses IN clause for batch retrieval.
     *
     * @param accountCount the number of accounts to query
     * @return the SQL query with placeholders
     */
    String getBatchGetDisclosureOptionQuery(int accountCount);

    /**
     * Get the SQL query for batch adding disclosure options.
     * Uses batch insert for multiple accounts.
     *
     * @return the SQL query for batch insert
     */
    String getBatchAddDisclosureOptionQuery();

    /**
     * Get the SQL query for batch updating disclosure options using CASE statement.
     *
     * @return the SQL query for batch update
     */
    String getBatchUpdateDisclosureOptionQuery();

    /**
     * Get the SQL query for retrieving secondary account instruction records for multiple account-user pairs.
     *
     * @param items list of secondary account instruction items with accountId and userId
     * @return the SQL query with placeholders
     */
    String getBatchGetSecondaryAccountInstructionQuery(List<SecondaryAccountInstructionItem> items);

    /**
     * Get the SQL query for batch adding secondary account instruction records.
     *
     * @return the SQL query for batch insert
     */
    String getBatchAddSecondaryAccountInstructionQuery();

    /**
     * Get the SQL query for batch updating secondary account instruction records.
     *
     * @return the SQL query for batch update
     */
    String getBatchUpdateSecondaryAccountInstructionQuery();

    /**
     * Get the SQL query for retrieving business stakeholder permission records for
     * multiple account-user pairs.
     *
     * @param items list of account-user permission records with accountId and userId
     * @return the SQL query with placeholders
     */
    String getBatchGetBusinessStakeholderPermissionQuery(List<BusinessStakeholderPermissionItem> items);

    /**
     * Get the SQL query for retrieving business stakeholder permission records for
     * multiple account IDs.
     *
     * @param accountCount number of account IDs to query
     * @return the SQL query with placeholders
     */
    String getBatchGetBusinessStakeholderPermissionByAccountQuery(int accountCount);

    /**
     * Get the SQL query for batch adding business stakeholder permission records.
     *
     * @return the SQL query for batch insert
     */
    String getBatchAddBusinessStakeholderPermissionQuery();

    /**
     * Get the SQL query for batch updating business stakeholder permission records.
     *
     * @return the SQL query for batch update
     */
    String getBatchUpdateBusinessStakeholderPermissionQuery();

    /**
     * Get the SQL query for batch deleting business stakeholder permission records.
     *
     * @return the SQL query for batch delete
     */
    String getBatchDeleteBusinessStakeholderPermissionQuery();

}
