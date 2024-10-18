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

package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPC specific JMR pricing resolution step with custom fields.
 *
 * @author Harshad
 * @since 04/01/19
 */
public class JMRPricingResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DEFAULT_CSV_FILE_SEPARATOR = ",";

    private static final String[] ENGIN_SCONNECT_FIELDS = new String[] {
        SPCConstants.CDR_ID,
        SPCConstants.FROM_NUMBER,
        SPCConstants.TO_NUMBER,
        SPCConstants.COUNTRY,
        SPCConstants.START_DATETIME,
        SPCConstants.END_DATETIME,
        SPCConstants.DURATION,
        SPCConstants.CALL_TYPE,
        SPCConstants.PLAN_NAME,
        SPCConstants.SERVICE_TYPE,
        SPCConstants.CALL_CHARGE,
        SPCConstants.CODE_STRING,
        SPCConstants.TARIFF_CODE,
        Constants.PLAN_ID,
        SPCConstants.PURCHASE_ORDER_ID
    };

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            List<PricingField> fields = context.getPricingFields();
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            switch (MediationServiceType.fromServiceName(serviceType.getStrValue())) {
                case ENGIN:
                case SCONNECT:
                    fields = fields.stream()
                                   .filter(pricingField -> ArrayUtils.contains(ENGIN_SCONNECT_FIELDS, pricingField.getName()))
                                   .collect(Collectors.toList());
                    break;
                default:
                    //Do nothing here
                    break;
            }
            String pricing = fields.stream()
                                   .map(PricingField::encode)
                                   .collect(Collectors.joining(DEFAULT_CSV_FILE_SEPARATOR));
            context.getResult().setPricingFields(pricing);
            logger.debug("Pricing Fields {}", pricing);
            return true;
        } catch (Exception e) {
            context.getResult().addError("ERR-PRICING-FIELDS-NOT-FOUND");
            logger.debug("Exception Occured in SPC JMRPricingResolutionStep", e);
            return false;
        }
    }

}
