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

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Panche Isajeski
 * @since 12/18/12
 */
public class JMRItemResolutionStep extends AbstractItemResolutionStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(JMRItemResolutionStep.class));

    private static final Integer LD_CALL_ITEM_ID = 2800;
    private JdbcTemplate jdbcTemplate = null;

    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {

        boolean passed = true;

        // validate call duration
        PricingField duration = PricingField.find(fields, "duration");
        if (duration == null || duration.getIntValue().intValue() < 0) {
            result.addError("ERR-DURATION");
            LOG.debug("Incorrect call duration for record " + result.getCdrRecordKey());
            passed &= false;
        }

        // discard unanswered calls
        PricingField disposition = PricingField.find(fields, "disposition");
        if (disposition == null || !disposition.getStrValue().equals("ANSWERED")) {

            LOG.debug("Not a billable record " + result.getCdrRecordKey());
            passed &= false;
        }

        PricingField destination = PricingField.find(fields, "dst");

        PricingField itemIdPF = PricingField.find(fields, "itemId");
        Integer itemId= LD_CALL_ITEM_ID;
        Map<String, Object> itemMap = null;

        if (null != itemIdPF && !StringUtils.isEmpty(itemIdPF.getStrValue())) {
      		itemMap = resolveItemByInternalNumber(result.getUserId(), itemIdPF.getStrValue());
        } else {
            Integer itemIdResolved = resolveItemById(result.getUserId(), itemId);
            if (itemIdResolved != null) {
                itemMap = new HashMap<>();
                itemMap.put(MediationStepResult.ITEM_ID, itemIdResolved);
            }
        }

        if (!itemMap.isEmpty()) {
            result.setItemId((Integer) itemMap.get(MediationStepResult.ITEM_ID));
            result.setQuantity(null != duration ? duration.getDecimalValue() : null);

            result.setDescription("Phone call to " + (null != destination ? destination.getStrValue() : "null"));
            LOG.debug("Number called = " + (null != destination ? destination.getStrValue() : "null") + ", "
                    + (null != duration ? duration.getStrValue() : "null") + " minutes");
            passed &= true;
        } else {
            passed &= false;
        }

        return passed;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context);
    }

    @Override
    protected Map<String, Object> resolveItemByInternalNumber(Integer userId, String internalNumber) {
        Map<String, Object> itemMap = new HashMap<String, Object>();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String parent = "select entity_id from base_user where id = ?";
        Integer userEntityId = null;
        
        SqlRowSet rs = jdbcTemplate.queryForRowSet(parent, userId);
        if(rs.next()) {
        	userEntityId= Integer.valueOf(rs.getInt("entity_id"));
        }
        if(userEntityId!=null){
            Integer itemId = getItemWithJdbcTemplateInternalNumberAndUserEntityId(internalNumber, userEntityId);
            if(itemId!=null) itemMap.put(MediationStepResult.ITEM_ID, itemId);
        }
        return itemMap;
    }

    public Integer getItemWithJdbcTemplateInternalNumberAndUserEntityId(String internalNumber, Integer entityId) {
        String sql = "SELECT id FROM item i left join item_entity_map iem\n" +
                " on iem.item_id = i.id " +
                " and i.deleted = 0 " +
                " i.internal_number = ? " +
                " and " +
                " (i.global = true or" +
                " iem.entity_id = ?)";
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, internalNumber, entityId, entityId);
        if (rs.next()) {
            return rs.getInt("currency_id");
        }
        return null;
    }

    public JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
