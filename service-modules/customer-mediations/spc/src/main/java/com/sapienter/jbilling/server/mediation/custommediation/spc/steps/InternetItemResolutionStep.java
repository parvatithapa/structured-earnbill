package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.util.Constants;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Neelabh
 * @since Mar 14, 2019
 */
public class InternetItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private SPCMediationHelperService service;

    public InternetItemResolutionStep(SPCMediationHelperService service) {
        this.service= service;
    }

    public InternetItemResolutionStep() {
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            Map<String, String> internetItemMap = null;
            Integer userId = context.getResult().getUserId();
            StringBuilder tariffCode = new StringBuilder();

            /**
             * Identify mediation file type in order to use appropriate company level MF.
             */
            String companyLevelMFName = null;
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            MediationServiceType mediationType = MediationServiceType.fromServiceName(serviceType.getStrValue());
            if (MediationServiceType.AAPT_INTERNET_USAGE.equals(mediationType)) {
                companyLevelMFName = SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID;
                tariffCode.append("AA");
            } else if (MediationServiceType.SCONNECT_DATA.equals(mediationType)) {
                companyLevelMFName = SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID;
                tariffCode.append("SC");
            } else if (MediationServiceType.SERVICE_ELEMENTS_DATA.equals(mediationType)) {
                companyLevelMFName = SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID;
                tariffCode.append("SE");
            }

            String planIdStrValue = PricingField.find(context.getPricingFields(), Constants.PLAN_ID).getStrValue();
            if(!StringUtils.isNumeric(planIdStrValue)){
                result.addError("ERR-PLAN-NOT-RESOLVED");
                return false;
            }
            /**
             * The data usage (download, upload or total) will be charged
             * based on identified plan of user as per item id of usage products
             * configured at company level MF. In addition to that, Plan should have
             * chargeable unit and type of Internet technology defined in MF.
             */
            internetItemMap = service.getInternetItemIdWithQuantityUnit(userId, context.getEntityId(), companyLevelMFName, Integer.valueOf(planIdStrValue));

            /**
             * Constructing tariff code and storing in pricing field. Below is the rule.
             * Example: AAA:DWL
             * 1) First 2 alphabet (AA) indicate identified mediation file (Carrier) type.
             * 2) Third alphabet (A) indicates initial of Internet technology type (A or N).
             * 3) After colon sign, 3 alphabet code indicate unit of chargeable usage.
             */
            String technologyType = internetItemMap.get(SPCConstants.INTERNET_TECHNOLOGY_TYPE);
            String usageChargeableUnit = internetItemMap.get(SPCConstants.INTERNET_USAGE_CHARGEABLE_UNIT);
            String usageChargeableUnitCode = CdrRecordType.InternetDataUsage.fromUsageName(usageChargeableUnit).getUsageTypeCode();
            tariffCode.append(technologyType.substring(0, 1));
            tariffCode.append(":");
            tariffCode.append(usageChargeableUnitCode);
            context.getRecord().addField(new PricingField(SPCConstants.TARIFF_CODE, tariffCode.toString()), false);

            /**
             * Storing usage chargeable type/unit in pricing field. This is
             * required in item quantity resolution step to charge usage quantity
             * based on the defined unit (like Download, Upload or Total).
             */
            context.getRecord().addField(new PricingField(SPCConstants.INTERNET_USAGE_CHARGEABLE_UNIT, usageChargeableUnitCode), false);
            result.setItemId(Integer.valueOf(internetItemMap.get(SPCConstants.INTERNET_USAGE_ITEM_ID)));
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
