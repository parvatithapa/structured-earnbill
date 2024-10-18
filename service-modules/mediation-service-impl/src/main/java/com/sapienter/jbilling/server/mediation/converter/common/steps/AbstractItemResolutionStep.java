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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract implementation(mediation step) of the process of resolving the item,
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public abstract class AbstractItemResolutionStep<T> extends AbstractMediationStep<T> {

    protected Map<String, Object> resolveItemByInternalNumber(Integer entityId, String internalNumber) {
        JdbcTemplate jdbcTemplate = Context.getBean("jBillingJdbcTemplate");
        String sql = "SELECT i.id FROM item i " +
                "left join item_entity_map iem on iem.item_id = i.id WHERE " +
                "i.internal_number = ? AND i.deleted = 0 AND " +
                "((i.entity_id = ? AND i.global = true) OR iem.entity_id = ?)";
        Integer id = jdbcTemplate.queryForObject(sql, new Object[]{internalNumber, entityId, entityId}, Integer.class);
        Map<String, Object> itemMap = new HashMap<>();
        if (id != null) itemMap.put(MediationStepResult.ITEM_ID, id);
        return itemMap;
    }

    protected Integer resolveItemById(Integer entityId, Integer itemId) {
        JdbcTemplate jdbcTemplate = Context.getBean("jBillingJdbcTemplate");
        String sql = "SELECT i.id FROM item i " +
                "left join item_entity_map iem on iem.item_id = i.id WHERE " +
                "i.id = ? AND i.deleted = 0 AND " +
                "((i.entity_id = ? AND i.global = true) OR iem.entity_id = ?)";
        Integer id = jdbcTemplate.queryForObject(sql, new Object[]{itemId, entityId, entityId}, Integer.class);
        return id;
    }

}
