/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.nges.export.batch.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.nges.export.row.ExportCustomerRow;
import com.sapienter.jbilling.server.nges.export.row.ExportRow;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.DayAheadPricingStrategy;
import com.sapienter.jbilling.server.pricing.strategy.LbmpPlusBlendedRatePricingStrategy;
import com.sapienter.jbilling.server.pricing.strategy.NYMEXPlusMonthlyPricingStrategy;
import com.sapienter.jbilling.server.pricing.strategy.UsageLimitPricingStrategy;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CustomerCommissionDefinitionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/**
 * Created by hitesh on 3/8/16.
 */
public class NGESExportCustomerProcessor extends AbstractNGESExportProcessor {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportCustomerProcessor.class));

    protected IWebServicesSessionBean webServicesSessionSpringBean;
    private Integer userId;
    private UserWS userWS;
    private OrderWS orderWS;
    private PlanWS planWS;
    private PriceModelWS priceModelWS;
    protected AccountTypeWS accountTypeWS;
    protected Map<String, Object> taxes;

    @Override
    public ExportRow process(Integer userId) throws Exception {
        LOG.debug("process execute for userId:" + userId);
        this.userId = userId;
        init();
	    return prepare();
    }

    private void init() {
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        this.userWS = webServicesSessionSpringBean.getUserWS(userId);
        this.accountTypeWS = webServicesSessionSpringBean.getAccountType(userWS.getAccountTypeId());
        OrderDTO orderDTO = findOneTimeOrder(userId, getCommodity(userWS));
        if (orderDTO == null) throw new SessionInternalError("order not found");
        OrderBL orderBL = new OrderBL(orderDTO);
        this.orderWS = orderBL.getWS(userWS.getLanguageId());
        this.planWS = PlanBL.getWS(new PlanDAS().findPlanByItemId(orderWS.getOrderLines()[0].getItemId()));
        if (planWS == null) throw new SessionInternalError("plan not found");
        SortedMap<Date, PriceModelWS> planPriceModels = planWS.getPlanItems().get(0).getModels();
        this.priceModelWS = planPriceModels.get(planPriceModels.firstKey());
        if (priceModelWS == null) throw new SessionInternalError("priceModelWS not found");
    }

    public ExportRow prepare() {
        LOG.debug("prepare row for customer");
        //TODO:make a separate method for build Account,Billing and Service Information
        ExportCustomerRow customerRow = new ExportCustomerRow();

        customerRow.setCompanyName(validate(FieldName.COMPANY_NAME, userWS.getCompanyName(), true));
        customerRow.setZone(validate(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME, getMetaFieldValue(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME, null, userWS.getMetaFields()), true));
        customerRow.setCustomerAcct("Extract SupplierId");
        customerRow.setActive(validate(FieldName.USER_STATUS, userWS.getStatus(), true).equals("Active") ? "Y" : "N");
        customerRow.setCustomerType(validate(FieldName.ACCOUNT_NAME, getAccountName(), true));
        customerRow.setCommodity(validate(FileConstants.COMMODITY, getMetaFieldValue(FileConstants.COMMODITY, null, userWS.getMetaFields()), true).equals("Electricity") ? "E" : "G");
        customerRow.setProductId(validate(FileConstants.PLAN, getMetaFieldValue(FileConstants.PLAN, null, userWS.getMetaFields()), true));
        customerRow.setCustumerUsername("Extract SupplierId");


        LOG.debug("Service Information(SI) Initialize.");
        Integer siMetaFieldGroupId = this.getAccountInformationTypeByName(accountTypeWS.getId(), FileConstants.SERVICE_INFORMATION_AIT).getId();
        if (siMetaFieldGroupId == null) {
            LOG.debug(FileConstants.SERVICE_INFORMATION_AIT + " not found.");
            throw new SessionInternalError(FileConstants.SERVICE_INFORMATION_AIT + " not found.");
        }
        customerRow.setCustomerName(validate(FileConstants.NAME, getMetaFieldValue(FileConstants.NAME, siMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setServiceAddressLine1(validate(FileConstants.ADDRESS1, getMetaFieldValue(FileConstants.ADDRESS1, siMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setServiceAddressLine2(validate(FileConstants.ADDRESS2, getMetaFieldValue(FileConstants.ADDRESS2, siMetaFieldGroupId, userWS.getMetaFields()), false));
        customerRow.setServiceCity(validate(FileConstants.CITY, getMetaFieldValue(FileConstants.CITY, siMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setServiceState(validate(FileConstants.STATE, getMetaFieldValue(FileConstants.STATE, siMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setServiceZip(validate(FileConstants.ZIP_CODE, getMetaFieldValue(FileConstants.ZIP_CODE, siMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setServicePhone(validate(FileConstants.TELEPHONE, getMetaFieldValue(FileConstants.TELEPHONE, siMetaFieldGroupId, userWS.getMetaFields()), false));
        customerRow.setEmail(validate(FileConstants.EMAIL, getMetaFieldValue(FileConstants.EMAIL, siMetaFieldGroupId, userWS.getMetaFields()), false));

        LOG.debug("Billing Information(BI) Initialize.");
        Integer biMetaFieldGroupId = this.getAccountInformationTypeByName(accountTypeWS.getId(), FileConstants.BILLING_INFORMATION_AIT).getId();
        if (biMetaFieldGroupId == null) {
            LOG.debug(FileConstants.BILLING_INFORMATION_AIT + " not found.");
            throw new SessionInternalError(FileConstants.BILLING_INFORMATION_AIT + " not found.");
        }
        customerRow.setContactName(validate(FileConstants.NAME, getMetaFieldValue(FileConstants.NAME, biMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setContactAddressLine1(validate(FileConstants.ADDRESS1, getMetaFieldValue(FileConstants.ADDRESS1, biMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setContactAddressLine2(validate(FileConstants.ADDRESS2, getMetaFieldValue(FileConstants.ADDRESS2, biMetaFieldGroupId, userWS.getMetaFields()), false));
        customerRow.setContactAddressCity(validate(FileConstants.CITY, getMetaFieldValue(FileConstants.CITY, biMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setContactAddressState(validate(FileConstants.STATE, getMetaFieldValue(FileConstants.STATE, biMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setContactAddressZip(validate(FileConstants.ZIP_CODE, getMetaFieldValue(FileConstants.ZIP_CODE, biMetaFieldGroupId, userWS.getMetaFields()), true));
        customerRow.setContactPhone(validate(FileConstants.TELEPHONE, getMetaFieldValue(FileConstants.TELEPHONE, biMetaFieldGroupId, userWS.getMetaFields()), false));

        customerRow.setCommunicationMode(validate(FieldName.COMMUNICATION_MODE, getCommunicationMode(userWS.getInvoiceDeliveryMethodId()), true));

        customerRow.setUtilityAccountNumber(validate(FileConstants.UTILITY_CUST_ACCT_NR, getMetaFieldValue(FileConstants.UTILITY_CUST_ACCT_NR, null, userWS.getMetaFields()), true));
        customerRow.setMeterType(validate(FileConstants.METER_TYPE, getMeterType((String) getMetaFieldValue(FileConstants.METER_TYPE, null, userWS.getMetaFields())), true));
        customerRow.setProductStartDate(getFormattedDate(validate(FieldName.ORDER_CREATION_DATE, orderWS.getCreateDate(), true)));
        customerRow.setLifeSupport(getLifeSupport(validate(FileConstants.CUST_LIFE_SUPPORT, getMetaFieldValue(FileConstants.CUST_LIFE_SUPPORT, null, userWS.getMetaFields()), false)));
        customerRow.setFixedPrice(validate(FieldName.FIXED_PRICE, getFixedPrice(), false));
        customerRow.setAdder(validate(FieldName.ADDER_FEE, getAdderFee(userWS), false));
        customerRow.setContractLength(validate(FileConstants.DURATION, getMetaFieldValue(FileConstants.DURATION, null, planWS.getMetaFields()), true));
        customerRow.setSwingUpperLimit(validate(UsageLimitPricingStrategy.PARAM_UPPER_LIMIT, priceModelWS.getAttributes().get(UsageLimitPricingStrategy.PARAM_UPPER_LIMIT), false));
        customerRow.setSwingLowerLimit(validate(UsageLimitPricingStrategy.PARAM_LOWER_LIMIT, priceModelWS.getAttributes().get(UsageLimitPricingStrategy.PARAM_LOWER_LIMIT), false));

        //TODO:Need to identify
        taxesInitialize(findTaxMetaField());
        customerRow.setTaxExemptPercentage("");
        customerRow.setCityTax("");
        customerRow.setCountyTax(getTax("COUNTY SALES TAX"));
        customerRow.setStateTax(getTax("STATE SALES TAX"));
        customerRow.setFederalTax("");
        customerRow.setgRTTabable(getTax("LOCAL GROSS RECEIPTS TAX"));
        customerRow.setgRTNonTaxable("");

        customerRow.setAnnualUsage(validate(FileConstants.ANNUAL_USAGE, getMetaFieldValue(FileConstants.ANNUAL_USAGE, null, userWS.getMetaFields()), false));
        customerRow.setUofM(validate(FileConstants.UOM, getMetaFieldValue(FileConstants.UOM, null, userWS.getMetaFields()), false));

        //TODO:These fields provided in migration data but we are need .some specification
        customerRow.setDiscountAmount("");
        customerRow.setDiscountMultiplier("");
        customerRow.setReceiveableType("");
        customerRow.setBillParty("");
        customerRow.setBillCalcParty("");
        customerRow.setMeterCalcParty("");
        //Todo: Need to discuss.
        customerRow.setLineLoss1(getLineLoss1());
        customerRow.setLineLoss2("");


        customerRow.setCustomerStartDate(getFormattedDate(validate(FieldName.USER_CREATION_DATE, userWS.getCreateDatetime(), true)));
        customerRow.setUsageFlowDate(getFormattedDate(validate(FileConstants.CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME, getMetaFieldValue(FileConstants.CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME, null, userWS.getMetaFields()), true)));

        //TODO:These fields provided in migration data but we are need .some specification
        customerRow.setCustomerIncrement("");
        customerRow.setContractID("");
        customerRow.setContractStatus("");
        customerRow.setSalesAgent1(getSalesAgent(userWS.getCommissionDefinitions(), 0));
        customerRow.setSalesRate1(getSalesRate(userWS.getCommissionDefinitions(), 0));
        customerRow.setSalesAgent2(getSalesAgent(userWS.getCommissionDefinitions(), 1));
        customerRow.setSalesRate2(getSalesRate(userWS.getCommissionDefinitions(), 1));
        customerRow.setSalesAgent3(getSalesAgent(userWS.getCommissionDefinitions(), 2));
        customerRow.setSalesRate3(getSalesRate(userWS.getCommissionDefinitions(), 2));
        customerRow.getRow();
        return customerRow;
    }

    protected String getSalesAgent(CustomerCommissionDefinitionWS[] arr, int location) {
        if (arr == null || location >= arr.length) return "";
        return arr[location] != null ? getSalesAgentName(arr[location].getPartnerId()) : "";
    }

    protected String getSalesRate(CustomerCommissionDefinitionWS[] arr, int location) {
        if (arr == null || location >= arr.length) return "";
        return arr[location] != null ? arr[location].getRate() : "";
    }

    protected String getSalesAgentName(Integer partnerId) {
        return validate(FieldName.AGENT_NAME, new PartnerDAS().findPartnerNameById(partnerId), false);
    }

    protected AccountInformationTypeWS getAccountInformationTypeByName(Integer accountTypeId, String name) {
        Optional<AccountInformationTypeWS> optional = Arrays.stream(webServicesSessionSpringBean.getInformationTypesForAccountType(accountTypeId)).filter(
                ait -> ait.getDescriptions().get(0).getContent().equals(name)).findFirst();
        return optional.orElse(null);
    }

    protected String getAccountName() {
        if (accountTypeWS != null) {
            String accountName = validate(FieldName.ACCOUNT_NAME, accountTypeWS.getDescriptions().get(0).getContent(), true);
            switch (accountName) {
                case FileConstants.RESIDENTIAL_ACCOUNT_TYPE:
                    return "R";
                case FileConstants.COMMERCIAL_ACCOUNT_TYPE:
                    return "C";
                default:
                    return null;
            }
        } else {
            throw new SessionInternalError("Account type not found for userID:" + userWS.getId());
        }
    }

    private String getCommunicationMode(Integer id) {
        switch (id) {
            case 1:
                return "E";
            case 2:
                return "R";
            //TODO:Need to discuss
            //case 2 : return "P";
            case 3:
                return "B";
            default:
                return "R";
        }
    }

    protected String getMeterType(String type) {
        switch (type) {
            case "Non Interval":
                return "N";
            case "Interval":
                return "I";
            case "Unknown":
                return "U";
            default:
                return "N";
        }
    }

    protected String getLifeSupport(String status) {
        return Boolean.parseBoolean(status) ? "Y" : "N";
    }

    private String getFixedPrice() {
        if (priceModelWS.getType().equals("FLAT")) {
            priceModelWS.getRate();
        }
        return null;
    }

    private String getAdderFee(UserWS userWs) {
        Object value = getMetaFieldValue(FileConstants.ADDER_FEE_META_FIELD, null, userWs.getMetaFields());
        if (value != null) return value.toString();

        switch (priceModelWS.getType()) {
            case "NYMEX_PLUS_MONTHLY":
                return priceModelWS.getAttributes().get(NYMEXPlusMonthlyPricingStrategy.ADDER_FEE);
            case "DAY_AHEAD":
                return priceModelWS.getAttributes().get(DayAheadPricingStrategy.ADDER_FEE);
            case "LBMP_PLUS_BLENDED_RATE":
                return priceModelWS.getAttributes().get(LbmpPlusBlendedRatePricingStrategy.ADDER_FEE);
        }
        return null;
    }

    public OrderDTO findOneTimeOrder(Integer userId, String commodity) {
        List<OrderDTO> orderDTOs = new OrderDAS().findByUserSubscriptions(userId);
        for (OrderDTO orderDTO : orderDTOs) {
            for (OrderLineDTO orderLineDTO : orderDTO.getLines()) {
                ItemDTO itemDTO = orderLineDTO.getItem();
                if (!itemDTO.getPlans().iterator().hasNext()) {
                    throw new SessionInternalError(getPlanNameByUser() + " plan not found.");
                }
                PlanDTO planDTO = itemDTO.getPlans().iterator().next();
                if (planDTO == null) return null;
                PlanItemDTO planItemDTO = null;
                if (!planDTO.getPlanItems().isEmpty()) planItemDTO = planDTO.getPlanItems().get(0);
                if (planItemDTO == null) return null;
                ItemDTO item = planItemDTO.getItem();
                MetaFieldValue metaFieldValue = item.getMetaField(FileConstants.COMMODITY);
                if (metaFieldValue != null && metaFieldValue.getValue() != null && (metaFieldValue.getValue().equals(commodity))) {
                    return orderDTO;
                }
            }
        }
        return null;
    }

    private String getLineLoss1() {
        String charges = validate(FileConstants.PASS_THROUGH_CHARGES_META_FIELD, getMetaFieldValue(FileConstants.PASS_THROUGH_CHARGES_META_FIELD, null, userWS.getMetaFields()), false);
        if (charges != null && !StringUtils.isEmpty(charges)) {
            return "Y";
        }
        return "N";
    }

    private String getPlanNameByUser() {
        return validate(FileConstants.PLAN, getMetaFieldValue(FileConstants.PLAN, null, userWS.getMetaFields()), true);
    }

    private String getSwingLimit(String attribute) {
        if (priceModelWS.getType().equals(PriceModelStrategy.USAGE_LIMIT.toString())) {
            return priceModelWS.getAttributes().get(attribute);
        }
        return "";
    }

    protected String getTax(String key) {
        if (taxes != null) {
            return validate(key, taxes.get(key), false);
        }
        return "";
    }


    private MetaFieldValueWS findTaxMetaField() {
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (metaFieldValueWS.getFieldName().equals(FileConstants.CUSTOMER_TAX_METAFIELD)) {
                return metaFieldValueWS;
            }
        }
        return null;
    }

    protected void taxesInitialize(MetaFieldValueWS taxesMFV) {

        if (taxesMFV != null && taxesMFV.getId() != null) {
            MetaFieldValue mf = new MetaFieldValueDAS().findNow(taxesMFV.getId());
            String taxFieldValue = (String) mf.getValue();
            ObjectMapper mapper = new ObjectMapper();
            try {
                taxes = mapper.readValue((taxFieldValue != null && !taxFieldValue.isEmpty()) ? taxFieldValue : "{}", new TypeReference<Map<String, Object>>() {
                });
            } catch (IOException ioe) {
                taxes = null;
                LOG.debug("Exception occurred while parsing taxes field value");
                LOG.error(ioe.getMessage());
            }
        }
    }
}
