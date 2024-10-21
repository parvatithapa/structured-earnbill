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
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

/**
 * @author Mahesh Shivarkar
 * @since Mar 27, 2019
 */

public class SPCUsageManagerTask extends BasicItemManager implements IItemPurchaseManager {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription VOIP_USAGE_FIELD_NAME =
            new ParameterDescription("VOIP_Usage_Field_Name", true, ParameterDescription.Type.STR);
    private static final ParameterDescription INTERNET_USAGE_FIELD_NAME =
            new ParameterDescription("Internate_Usage_Field_Name", true, ParameterDescription.Type.STR);

    public SPCUsageManagerTask() {
        descriptions.add(VOIP_USAGE_FIELD_NAME);
        descriptions.add(INTERNET_USAGE_FIELD_NAME);
    }

    @Override
    protected void validateContext(ItemPurchaseManagerContext context) throws TaskException {
        super.validateContext(context);
        String voipUsageFieldName = parameters.get(VOIP_USAGE_FIELD_NAME.getName());
        if (voipUsageFieldName == null) {
            logger.error("Cannot find configured VOIP Usage Field Name for entity {}", getEntityId());
            throw new TaskException("Cannot find configured VOIP Usage Field Name: " + voipUsageFieldName);
        }

        String internateUsageFieldName = parameters.get(INTERNET_USAGE_FIELD_NAME.getName());
        if (internateUsageFieldName == null) {
            logger.error("Cannot find configured Internet Usage Field Name for entity {}", getEntityId());
            throw new TaskException("Cannot find configured Internet Usage Field Name: " + internateUsageFieldName);
        }

        //Getting the Values for setting the MetaField
        String usageValue = null;
        List<CallDataRecord> records = context.getRecords();
        if(CollectionUtils.isNotEmpty(records)) {
            boolean isFound = false;
            for (PricingField pricingField : records.get(0).getFields()) {
                String pricingFieldName = pricingField.getName();
                if(pricingFieldName.equals(voipUsageFieldName) || pricingFieldName.equals(internateUsageFieldName)) {
                    isFound = true;
                    usageValue = pricingField.getStrValue();
                    break;
                }
            }
            if (!isFound) {
                throw new TaskException("Usage field name of file does not match with configured field name parameter");
            }
            if (StringUtils.isEmpty(usageValue)) {
                throw new SessionInternalError("VOIP Usage or Internate Usage both should not be null");
            }
        }
    }

    @Override
    protected String getCallIdentifierFromPricingFields(List<PricingField> pricingFields) {
        if (CollectionUtils.isNotEmpty(pricingFields)) {
            PricingField callIdentifer;
            callIdentifer = PricingField.find(pricingFields, parameters.get(VOIP_USAGE_FIELD_NAME.getName()));
            if(null == callIdentifer) {
                callIdentifer = PricingField.find(pricingFields, parameters.get(INTERNET_USAGE_FIELD_NAME.getName()));
            }
            return null!= callIdentifer ? callIdentifer.getStrValue() : StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

}
