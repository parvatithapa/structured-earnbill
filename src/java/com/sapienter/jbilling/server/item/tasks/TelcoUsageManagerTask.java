/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.item.tasks;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.util.Constants;

public class TelcoUsageManagerTask extends BasicItemManager {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription DNIS_FIELD_NAME =
            new ParameterDescription("DNIS_Field_Name", true, ParameterDescription.Type.STR);

    public TelcoUsageManagerTask() {
        descriptions.add(DNIS_FIELD_NAME);
    }


    @Override
    protected void validateContext(ItemPurchaseManagerContext context) throws TaskException {
        super.validateContext(context);
        String dnisFieldName = parameters.get(DNIS_FIELD_NAME.getName());
        if (dnisFieldName == null) {
            throw new TaskException("Cannot find configured DNIS Field Name: " + dnisFieldName);
        }

        List<CallDataRecord> records = context.getRecords();
        if(CollectionUtils.isNotEmpty(records)) {
            //Getting the Values for setting the MetaField
            String dnis = null;
            boolean isFound = false;
            for (PricingField pricingField : records.get(0).getFields()) {
                if (pricingField.getName().equals(dnisFieldName)) {
                    isFound = true;
                    dnis = pricingField.getStrValue();
                    break;
                }
            }
            if (!isFound) {
                throw new TaskException("DNIS field name of file does not match with configured field name parameter: "+ dnisFieldName);
            }
            if (dnis == null) {
                throw new SessionInternalError("DNIS value should not be null: "+ dnis);
            }
        }
    }

    @Override
    protected String getCallIdentifierFromPricingFields(List<PricingField> pricingFields) {
        if (CollectionUtils.isNotEmpty(pricingFields)) {
            String dnisFieldName = parameters.get(DNIS_FIELD_NAME.getName());
            String callIdentifier = PricingField.find(pricingFields, dnisFieldName).getStrValue();
            logger.debug("call identifer {} found from pricing fields {}", callIdentifier, pricingFields);
            return callIdentifier;
        }
        return StringUtils.EMPTY;
    }

    @Override
    protected void setMetaFieldsOnLine(OrderLineDTO line, ItemPurchaseManagerContext context) {
        OrderDTO order = context.getOrder();
        if (null != order.getPlanItemId()) {
            // Setting the MetaFields in the OrderLine
            MetaField metaField = MetaFieldBL.getFieldByName(order.getUser().getCompany().getId(),
                    new EntityType[] { EntityType.ORDER_LINE }, Constants.PLAN_ITEM_ID);
            line.setMetaField(metaField, order.getPlanItemId());
        }
    }

}
