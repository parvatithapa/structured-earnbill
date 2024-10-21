package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.OptusMobileRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;

/**
 * @author Harshad
 * @since Dec 19, 2018
 */
public class ItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private SPCMediationHelperService service;

    public ItemResolutionStep(SPCMediationHelperService service) {
        this.service= service;
    }

    public ItemResolutionStep() {
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            logger.debug("ItemResolutionStep.executeStep() : result {} : ", result);
            String productCode = null;
            String tariffCode = null;
            PricingField assetIdentifier = null;
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_FIXED_LINE)){
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.TELSTRA_ASSET_NUMBER);
                context.getRecord().addField(new PricingField(SPCConstants.FROM_NUMBER, assetIdentifier.getStrValue()), false);
            } else if (MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_FIXED_LINE_MONTHLY)) {
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER);
                context.getRecord().addField(new PricingField(SPCConstants.FROM_NUMBER, assetIdentifier.getStrValue()), false);
            } else if (MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_MOBILE_4G)) {
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.P1_S_P_NUMBER_EC_ADDRESS);
                context.getRecord().addField(new PricingField(SPCConstants.FROM_NUMBER, assetIdentifier.getStrValue()), false);
            } else {
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.FROM_NUMBER);
            }
            //JBSPC-613
            if(null == assetIdentifier ||
                    StringUtils.isEmpty(assetIdentifier.getStrValue())) {
                result.addError("ITEM-NOT-FOUND");
                logger.debug("Asset Number not found from pricing fields ");
                return false;
            }
            // Adding asset number for resolving quantity
            context.getRecord().addField(new PricingField(SPCConstants.ASSET_NUMBER, assetIdentifier.getStrValue()), false);
            PricingField codeString = PricingField.find(context.getPricingFields(), SPCConstants.CODE_STRING);
            if (codeString != null && StringUtils.isNotEmpty(codeString.getStrValue())) {
                Map<String, String> tariffInfo =
                        service.getProductCodeFromRouteRateCard(result.getUserId(),
                                assetIdentifier.getStrValue(), codeString.getStrValue(), result.getEventDate());

                productCode = tariffInfo.get(SPCConstants.PRODUCT_CODE);
                tariffCode = tariffInfo.get(SPCConstants.TARIFF_CODE);
                if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_FIXED_LINE)
                        && productCode.equalsIgnoreCase("tf_service_&_equipment")){
                    logger.debug("Telstra fixed line contain service_&_equipment product ", result.getUserId(), codeString);
                    result.addError("SERVICE-AND-EQUIPMENT-PRODUCT-FOUND");
                    return false;
                }
                //Adding TARIFF_CODE to pricing fields
                context.getRecord().addField(new PricingField(SPCConstants.TARIFF_CODE, tariffCode), false);
                logger.debug("Resolved PRODUCT_CODE {} and TARIFF_CODE {} for CODE_STRING {}", productCode, tariffCode, codeString);

                // Validation of Price from the Route Rate Card
                if (tariffInfo.isEmpty()) {
                    logger.debug("Price not found for user {} for CODE_STRING {}", result.getUserId(), codeString);
                    result.addError("PRICE-NOT-FOUND");
                    return false;
                }
                if (service.getRecordCountFromRouteRateCard(result.getUserId(),
                        assetIdentifier.getStrValue(), codeString.getStrValue(), result.getEventDate()) > 1) {
                    logger.debug("Mulitple Prices found for user {} for CODE_STRING {}", result.getUserId(), codeString);
                    result.addError("MULTIPLE-PRICES-FOUND");
                    return false;
                }
            }
            PricingField toNumber = null;
            if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_MOBILE_4G)){
                toNumber = PricingField.find(context.getPricingFields(), SPCConstants.P1_O_P_NUMBER_EC_ADDRESS);
            }
            else if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_FIXED_LINE)){
                toNumber = PricingField.find(context.getPricingFields(), SPCConstants.TELSTRA_DESTINATION_NUMBER);
            }
            else{
                toNumber = PricingField.find(context.getPricingFields(), SPCConstants.TO_NUMBER);
            }
            //fetching free SPC customercareNumbers from data table
            StringBuilder customerCareTable = new StringBuilder("route_").append(context.getEntityId()).append("_calltozero");
            List<String> customerCareNumbers = service.getCustomerCareContactNumbers(customerCareTable.toString());
            if(null != toNumber && customerCareNumbers.contains(toNumber.getStrValue())){
                String customerCareProductid = new MetaFieldDAS().getComapanyLevelMetaFieldValue(SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_CUSTOMER_CARE_NUMBER_ITEM_ID, context.getEntityId());
                result.setItemId(Integer.valueOf(customerCareProductid));
                return true;
            }

            Optional<Integer> itemId = service.getItemIdByProductCode(productCode);
            if(!itemId.isPresent()) {
                logger.debug("Item not found for user {}", result.getUserId());
                result.addError("ITEM-NOT-FOUND");
                return false;
            }
            result.setItemId(itemId.get());
            BigDecimal itemPrice = BigDecimal.ZERO;
            if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_MOBILE_4G)){
                List<PricingField> fields =  context.getPricingFields();
                itemPrice = new BigDecimal(PricingField.find(fields, SPCConstants.AMOUNT_TELSTRA_4G_MOBILE).getStrValue());
                if (Integer.valueOf(1).equals(Integer.parseInt(PricingField.find(fields, SPCConstants.DISABLE_TAX).getStrValue()))){
                    itemPrice = service.getAmountWithoutGST(result.getUserId(), itemId.get(),itemPrice);
                }
                context.getRecord().addField(new PricingField(SPCConstants.CALL_CHARGE, itemPrice.toString()), false);
            }else if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_FIXED_LINE_MONTHLY)){
                String amount = PricingField.find(context.getPricingFields(), SPCConstants.AMOUNT_TELSTRA_FIXED_LINE_MONTHLY).getStrValue();
                if(!StringUtils.isBlank(amount)){
                    itemPrice = (new BigDecimal(amount.trim()).divide(new BigDecimal(10000)));
                }
                itemPrice = service.getAmountWithoutGST(result.getUserId(), itemId.get(),itemPrice);
                context.getRecord().addField(new PricingField(SPCConstants.CALL_CHARGE, itemPrice.toString()), false);
            }else if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.OPTUS_MOBILE)){
                String callCharge = PricingField.find(context.getPricingFields(), SPCConstants.TOTAL_CHARGES).getStrValue();
                PricingField pfTax = PricingField.find(context.getPricingFields(), SPCConstants.TOTAL_TAX);
                String tax = null != pfTax ? pfTax.getStrValue() : null ;
                if(NumberUtils.isCreatable(StringUtils.stripStart(callCharge, "0"))){
                    itemPrice = new BigDecimal(callCharge.trim()).divide(new BigDecimal(1000));
                }
                if (StringUtils.isNotBlank(tax) && NumberUtils.isCreatable(StringUtils.stripStart(tax, "0"))){
                    itemPrice = itemPrice.add((new BigDecimal(tax.trim()).divide(new BigDecimal(1000))));
                }

                /**
                 * JBSPC-712: For Optus Mobile roaming type 20 CDR, there should be no GST included
                 * means the CDR call charges of record type 20 should be assumed as exclusive of GST.
                 */
                PricingField recordType = PricingField.find(context.getPricingFields(), SPCConstants.CDR_IDENTIFIER);
                if (OptusMobileRecord.ROAM != OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    itemPrice = service.getAmountWithoutGST(result.getUserId(), itemId.get(),itemPrice);
                }
                context.getRecord().addField(new PricingField(SPCConstants.CALL_CHARGE, itemPrice.toString()), false);
            }
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
