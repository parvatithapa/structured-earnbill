/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.steps;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import org.apache.log4j.Logger;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Abstract implementation(mediation step) of the process of resolving the user,
 * currency and date from the CDR
 *
 * @author: Panche Isajeski
 * @since: 12/16/12
 */
public abstract class AbstractUserResolutionStep<T> extends AbstractMediationStep<T> {

    protected static final String USER_EXPIRED = "deleted";
    protected final FormatLogger LOG = new FormatLogger(Logger.getLogger(this.getClass()));

    private static final String RESOLVE_ITEM_BY_INTERNAL_NUMBER_QUERY = "SELECT i.id FROM item i " +
                                                                     "LEFT JOIN item_entity_map iem ON iem.item_id = i.id " +
                                                                         "WHERE i.internal_number = ? " +
                                                                           "AND i.deleted = 0 " +
                                                                           "AND ((i.entity_id = ? AND i.global = TRUE) OR iem.entity_id = ?)";
    
    protected Map<String, Object> resolveUserByUsername(Integer entityId, String username) {
        return Collections.emptyMap();
    }

    protected Map<String, Object> resolveUserById(Integer entityId, Integer userId) {
        return Collections.emptyMap();
    }

    protected boolean setUserOnResult(MediationStepResult result, Map<String, Object> userDTOMap) {
        if (!userDTOMap.isEmpty() && isValid(userDTOMap, result)) {
            result.setUserId((Integer) userDTOMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId((Integer) userDTOMap.get(MediationStepResult.CURRENCY_ID));

            LOG.debug("Set result user id " + result.getUserId() + ", currency id " + result.getCurrencyId());
            return true;
        }
        return false;
    }

    protected boolean isValid(Map<String, Object> userDTOmap, MediationStepResult result) {
        if (userDTOmap == null || userDTOmap.containsKey(USER_EXPIRED))  {
            result.addError("JB_RESOLVED_USER_IS_EXPIRED");
            return false;
        }
        return true;
    }

    protected Map<String, Object> resolveItemByInternalNumber(Integer entityId, String internalNumber) {
        JdbcTemplate jdbcTemplate = Context.getBean("jBillingJdbcTemplate");
        Integer id = jdbcTemplate.queryForObject(RESOLVE_ITEM_BY_INTERNAL_NUMBER_QUERY, new Object[]{internalNumber, entityId, entityId}, Integer.class);
        Map<String, Object> itemMap = new HashMap<>();
        if (id != null) itemMap.put(MediationStepResult.ITEM_ID, id);
        return itemMap;
    }

}
