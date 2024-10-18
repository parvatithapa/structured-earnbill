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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentAgentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.nges.export.row.ExportEnrollmentRow;
import com.sapienter.jbilling.server.nges.export.row.ExportRow;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;

/**
 * Created by hitesh on 30/9/16.
 */
public class NGESExportEnrollmentProcessor extends NGESExportCustomerProcessor {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportEnrollmentProcessor.class));

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("beforeStep");
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    }

    @Override
    public ExportRow process(Integer enrollmentId) throws Exception {
        LOG.debug("process execute for enrollmentId:" + enrollmentId);
        CustomerEnrollmentWS enrollmentWS = webServicesSessionSpringBean.getCustomerEnrollment(enrollmentId);
        ExportRow row = prepare(enrollmentWS);
        return row;
    }

    public ExportRow prepare(final CustomerEnrollmentWS enrollmentWS) {
        LOG.debug("prepare row for enrollment");

        ExportEnrollmentRow enrollmentRow = new ExportEnrollmentRow();

        enrollmentRow.setCompanyName(validate(FieldName.COMPANY_NAME, enrollmentWS.getCompanyName(), true));
        enrollmentRow.setStatus(validate(FieldName.ENROLLMENT_STATUS, enrollmentWS.getStatus().toString(), true));

        LOG.debug("Account Information(AI) Initialize.");
        accountTypeWS = webServicesSessionSpringBean.getAccountType(enrollmentWS.getAccountTypeId());
        Integer aiMetaFieldGroupId = this.getAccountInformationTypeByName(accountTypeWS.getId(), FileConstants.ACCOUNT_INFORMATION_AIT).getId();
        if (aiMetaFieldGroupId == null) {
            LOG.debug(FileConstants.ACCOUNT_INFORMATION_AIT + " not found.");
            throw new SessionInternalError(FileConstants.ACCOUNT_INFORMATION_AIT + " not found.");
        }
        enrollmentRow.setCustomerType(validate(FieldName.ACCOUNT_NAME, getAccountName(), true));
        enrollmentRow.setCommodity(validate(FileConstants.COMMODITY, getMetaFieldValue(FileConstants.COMMODITY, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), true).equals("Electricity") ? "E" : "G");
        enrollmentRow.setProductId(validate(FileConstants.PLAN, getMetaFieldValue(FileConstants.PLAN, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setContractLength(validate(FileConstants.DURATION, getMetaFieldValue(FileConstants.DURATION, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setUtilityAccountNumber(validate(FileConstants.UTILITY_CUST_ACCT_NR, getMetaFieldValue(FileConstants.UTILITY_CUST_ACCT_NR, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setCustomerStartDate(getFormattedDate(validate(FileConstants.ACTUAL_START_DATE, getMetaFieldValue(FileConstants.ACTUAL_START_DATE, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), true)));
        enrollmentRow.setLifeSupport(getLifeSupport(validate(FileConstants.CUST_LIFE_SUPPORT, getMetaFieldValue(FileConstants.CUST_LIFE_SUPPORT, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), false)));
        enrollmentRow.setMeterType(validate(FileConstants.METER_TYPE, getMeterType((String) getMetaFieldValue(FileConstants.METER_TYPE, aiMetaFieldGroupId, enrollmentWS.getMetaFields())), true));
        enrollmentRow.setCharge1(validate(FileConstants.PASS_THROUGH_CHARGES_META_FIELD, getMetaFieldValue(FileConstants.PASS_THROUGH_CHARGES_META_FIELD, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setCharge2("");
        enrollmentRow.setTaxExemptionCode(validate(FieldName.TAX_EXEMPTION_CODE, getMetaFieldValue(FieldName.TAX_EXEMPTION_CODE, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setTaxDiscountPercentage(validate(FieldName.TAX_DISCOUNT_PERCENTAGE, getMetaFieldValue(FieldName.TAX_DISCOUNT_PERCENTAGE, aiMetaFieldGroupId, enrollmentWS.getMetaFields()), false));

        LOG.debug("Service Information(SI) Initialize.");
        Integer siMetaFieldGroupId = this.getAccountInformationTypeByName(accountTypeWS.getId(), FileConstants.SERVICE_INFORMATION_AIT).getId();
        if (siMetaFieldGroupId == null) {
            LOG.debug(FileConstants.SERVICE_INFORMATION_AIT + " not found.");
            throw new SessionInternalError(FileConstants.SERVICE_INFORMATION_AIT + " not found.");
        }
        enrollmentRow.setCustomerName(validate(FileConstants.NAME, getMetaFieldValue(FileConstants.NAME, siMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setServiceAddressLine1(validate(FileConstants.ADDRESS1, getMetaFieldValue(FileConstants.ADDRESS1, siMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setServiceAddressLine2(validate(FileConstants.ADDRESS2, getMetaFieldValue(FileConstants.ADDRESS2, siMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setServiceCity(validate(FileConstants.CITY, getMetaFieldValue(FileConstants.CITY, siMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setServiceState(validate(FileConstants.STATE, getMetaFieldValue(FileConstants.STATE, siMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setServiceZip(validate(FileConstants.ZIP_CODE, getMetaFieldValue(FileConstants.ZIP_CODE, siMetaFieldGroupId, enrollmentWS.getMetaFields()), true));
        enrollmentRow.setServicePhone(validate(FileConstants.TELEPHONE, getMetaFieldValue(FileConstants.TELEPHONE, siMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setEmail(validate(FileConstants.EMAIL, getMetaFieldValue(FileConstants.EMAIL, siMetaFieldGroupId, enrollmentWS.getMetaFields()), false));

        LOG.debug("Billing Information(BI) Initialize.");
        Integer biMetaFieldGroupId = this.getAccountInformationTypeByName(accountTypeWS.getId(), FileConstants.BILLING_INFORMATION_AIT).getId();
        if (biMetaFieldGroupId == null) {
            LOG.debug(FileConstants.BILLING_INFORMATION_AIT + " not found.");
            throw new SessionInternalError(FileConstants.BILLING_INFORMATION_AIT + " not found.");
        }
        enrollmentRow.setContactName(validate(FileConstants.NAME, getMetaFieldValue(FileConstants.NAME, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setContactAddressLine1(validate(FileConstants.ADDRESS1, getMetaFieldValue(FileConstants.ADDRESS1, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setContactAddressLine2(validate(FileConstants.ADDRESS2, getMetaFieldValue(FileConstants.ADDRESS2, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setContactAddressCity(validate(FileConstants.CITY, getMetaFieldValue(FileConstants.CITY, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setContactAddressState(validate(FileConstants.STATE, getMetaFieldValue(FileConstants.STATE, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setContactAddressZip(validate(FileConstants.ZIP_CODE, getMetaFieldValue(FileConstants.ZIP_CODE, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setContactPhone(validate(FileConstants.TELEPHONE, getMetaFieldValue(FileConstants.TELEPHONE, biMetaFieldGroupId, enrollmentWS.getMetaFields()), false));

        LOG.debug("Enrollment Information(EI) Initialize.");
        enrollmentRow.setCommunicationMode(validate(FieldName.COMMUNICATION_MODE, getMetaFieldValue(FileConstants.NOTIFICATION_METHOD, null, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setAdder(validate(FieldName.ADDER_FEE, getMetaFieldValue(FileConstants.ADDER_FEE_METAFIELD_NAME, null, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setCustomerSpecificRate(validate(FileConstants.CUSTOMER_SPECIFIC_RATE, getMetaFieldValue(FileConstants.CUSTOMER_SPECIFIC_RATE, null, enrollmentWS.getMetaFields()), false));
        enrollmentRow.setCalculateTaxManually(getCalculateTaxManually(validate(FileConstants.CUSTOMER_CALCULATE_TAX_MANUALLY, getMetaFieldValue(FileConstants.CUSTOMER_CALCULATE_TAX_MANUALLY, null, enrollmentWS.getMetaFields()), false)));

        LOG.debug("Tax Information(TI) Initialize.");
        taxesInitialize(findTaxMetaFIeld(enrollmentWS));
        enrollmentRow.setCityTax("");
        enrollmentRow.setCountyTax(getTax("COUNTY SALES TAX"));
        enrollmentRow.setStateTax(getTax("STATE SALES TAX"));
        enrollmentRow.setFederalTax("");
        enrollmentRow.setgRTTabable(getTax("LOCAL GROSS RECEIPTS TAX"));
        enrollmentRow.setgRTNonTaxable("");

        LOG.debug("Sales Information(SI) Initialize.");
        enrollmentRow.setSalesAgent1(getSalesAgent(enrollmentWS.getCustomerEnrollmentAgents(), 0));
        enrollmentRow.setSalesRate1(getSalesRate(enrollmentWS.getCustomerEnrollmentAgents(), 0));
        enrollmentRow.setSalesAgent2(getSalesAgent(enrollmentWS.getCustomerEnrollmentAgents(), 1));
        enrollmentRow.setSalesRate2(getSalesRate(enrollmentWS.getCustomerEnrollmentAgents(), 1));
        enrollmentRow.setSalesAgent3(getSalesAgent(enrollmentWS.getCustomerEnrollmentAgents(), 2));
        enrollmentRow.setSalesRate3(getSalesRate(enrollmentWS.getCustomerEnrollmentAgents(), 2));

        enrollmentRow.getRow();
        return enrollmentRow;
    }

    protected String getSalesAgent(CustomerEnrollmentAgentWS[] arr, int location) {
        if (arr == null || location >= arr.length) return "";
        return arr[location] != null ? getSalesAgentName(arr[location].getPartnerId()) : "";
    }

    protected String getSalesRate(CustomerEnrollmentAgentWS[] arr, int location) {
        if (arr == null || location >= arr.length) return "";
        return arr[location] != null ? arr[location].getRate() : "";
    }

    private String getCalculateTaxManually(String status) {
        return Boolean.parseBoolean(status) ? "Y" : "N";
    }

    private MetaFieldValueWS findTaxMetaFIeld(final CustomerEnrollmentWS enrollmentWS) {
        for (MetaFieldValueWS metaFieldValueWS : enrollmentWS.getMetaFields()) {
            if (metaFieldValueWS.getFieldName().equals(FileConstants.CUSTOMER_TAX_METAFIELD)) {
                return metaFieldValueWS;
            }
        }
        return null;
    }
}
