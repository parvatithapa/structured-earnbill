package com.sapienter.jbilling.server.customerEnrollment.csv;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentAgentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.*;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CustomerEnrollmentEntryParser implements CSVEntryParser<CustomerEnrollmentWS> {

    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private CompanyDAS companyDAS = new CompanyDAS();
    private AccountTypeDAS accountTypeDAS = new AccountTypeDAS();
    private AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
    private ItemDAS itemDAS = new ItemDAS();
    private PlanDAS planDAS = new PlanDAS();

    public CustomerEnrollmentWS parseEntry(String... data) {
        CustomerEnrollmentWS customerEnrollment = new CustomerEnrollmentWS();

        customerEnrollment.setId(0);
        customerEnrollment.setStatus(CustomerEnrollmentStatus.PENDING);
        customerEnrollment.setBulkEnrollment(true);
        customerEnrollment.setCompanyName(data[0]);
        customerEnrollment.setAccountNumber(data[19]);

        boolean isResidentialCustomer = data[1].equals("R");
        String accountTypeName = isResidentialCustomer ? FileConstants.RESIDENTIAL_ACCOUNT_TYPE : (data[1].equals("C") || data[1].equals("I") ? FileConstants.COMMERCIAL_ACCOUNT_TYPE : null);

        try {
            CompanyDTO company = companyDAS.findEntityByName(customerEnrollment.getCompanyName());
            Integer companyId = company.getId();
            AccountTypeDTO accountTypeDTO = accountTypeDAS.findAccountTypeByName(companyId, accountTypeName);
            Integer accountTypeId = accountTypeDTO != null ? accountTypeDTO.getId() : null;

            customerEnrollment.setAccountTypeName(accountTypeName);
            customerEnrollment.setEntityId(companyId);
            customerEnrollment.setCreateDatetime(TimezoneHelper.companyCurrentDate(companyId));
            customerEnrollment.setAccountTypeId(accountTypeId);
            customerEnrollment.setBrokerCatalogVersion(data[3].substring(0, 6));

            // MetaFields mapping
            String planInternalNumber = data[3].substring(6);
            PlanDTO plan = planDAS.findPlanByItemId(itemDAS.findItemByInternalNumber(planInternalNumber, companyId).getId());

            List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();

            // Service Information
            Integer serviceInformationMetaFieldGroupId = accountInformationTypeDAS.getAccountInformationTypeByName(companyId, accountTypeId, FileConstants.SERVICE_INFORMATION_AIT).getId();
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.DIVISION, serviceInformationMetaFieldGroupId, DataType.ENUMERATION, true, plan.getMetaField(FileConstants.DIVISION).getValue()));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.NAME, serviceInformationMetaFieldGroupId, DataType.STRING, true, data[4]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ADDRESS1, serviceInformationMetaFieldGroupId, DataType.STRING, true, data[5]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ADDRESS2, serviceInformationMetaFieldGroupId, DataType.STRING, false, data[6]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CITY, serviceInformationMetaFieldGroupId, DataType.STRING, true, data[7]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.STATE, serviceInformationMetaFieldGroupId, DataType.ENUMERATION, true, data[8]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ZIP_CODE, serviceInformationMetaFieldGroupId, DataType.STRING, true, data[9]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.TELEPHONE, serviceInformationMetaFieldGroupId, DataType.STRING, false, data[10]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.EMAIL, serviceInformationMetaFieldGroupId, DataType.STRING, true, data[12]));

            // Billing Information
            Integer billingInformationMetaFieldGroupId = accountInformationTypeDAS.getAccountInformationTypeByName(companyId, accountTypeId, FileConstants.BILLING_INFORMATION_AIT).getId();
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.NAME, billingInformationMetaFieldGroupId, DataType.STRING, true, data[13]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ADDRESS1, billingInformationMetaFieldGroupId, DataType.STRING, true, data[14]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ADDRESS2, billingInformationMetaFieldGroupId, DataType.STRING, false, data[15]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CITY, billingInformationMetaFieldGroupId, DataType.STRING, true, data[16]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.STATE, billingInformationMetaFieldGroupId, DataType.ENUMERATION, true, data[17]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ZIP_CODE, billingInformationMetaFieldGroupId, DataType.STRING, true, data[18]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.TELEPHONE, billingInformationMetaFieldGroupId, DataType.STRING, false, data[19]));

            // Account Information
            Integer accountInformationMetaFieldGroupId = accountInformationTypeDAS.getAccountInformationTypeByName(companyId, accountTypeId, FileConstants.ACCOUNT_INFORMATION_AIT).getId();
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.COMMODITY, accountInformationMetaFieldGroupId, DataType.ENUMERATION, true, data[2].equals("E") ? "Electricity" : (data[2].equals("G") ? "Gas" : null)));
            MetaFieldValue durationMetaFieldValue = plan.getMetaField(FileConstants.DURATION);
            if (durationMetaFieldValue != null) {
                metaFieldValues.add(new MetaFieldValueWS(FileConstants.DURATION, accountInformationMetaFieldGroupId, DataType.STRING, true, durationMetaFieldValue.getValue()));
            }
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.PLAN, accountInformationMetaFieldGroupId, DataType.ENUMERATION, true, planInternalNumber));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CUSTOMER_ACCOUNT_KEY, accountInformationMetaFieldGroupId, DataType.STRING, true, data[20]));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.METER_TYPE, accountInformationMetaFieldGroupId, DataType.STRING, true, data[21].equals("N") ? "Non Interval" : (data[21].equals("I") ? "Interval" : (data[21].equals("U") ? "Unknown" : null))));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ACTUAL_START_DATE, accountInformationMetaFieldGroupId, DataType.DATE, false, StringUtils.isNotEmpty(data[22]) ? dateFormat.parse(data[22]) : null));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CUST_LIFE_SUPPORT, accountInformationMetaFieldGroupId, DataType.BOOLEAN, true, data[23].equals("Y")));

            // Non AIT meta fields
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.NOTIFICATION_METHOD, null, DataType.ENUMERATION, true, data[11].equals("E") ? "Email" : ((data[11].equals("R") || data[11].equals("P")) ? "Paper" : (data[11].equals("B") ? "Both" : "Paper"))));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CUSTOMER_SPECIFIC_RATE, null, DataType.DECIMAL, false, !data[24].isEmpty() ? new BigDecimal(data[24]) : null));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ADDER_FEE_METAFIELD_NAME, null, DataType.DECIMAL, false, !data[25].isEmpty() ? new BigDecimal(data[25]) : null));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CUSTOMER_ACCOUNT_KEY, accountInformationMetaFieldGroupId, DataType.STRING, true, data[20]));
            metaFieldValues.add(new MetaFieldValueWS("METER_TYPE", accountInformationMetaFieldGroupId, DataType.STRING, true, data[21].equals("N") ? "Non Interval" : (data[21].equals("I") ? "Interval" : (data[21].equals("U") ? "Unknown" : null))));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.ACTUAL_START_DATE, accountInformationMetaFieldGroupId, DataType.DATE, false, StringUtils.isNotEmpty(data[22]) ? dateFormat.parse(data[22]) : null));
            metaFieldValues.add(new MetaFieldValueWS(FileConstants.CUST_LIFE_SUPPORT, accountInformationMetaFieldGroupId, DataType.BOOLEAN, true, data[23].equals("Y")));

            List<CustomerEnrollmentAgentWS> agentList = new ArrayList<>();
            for( int i=26; i<data.length; i += 2) {
                CustomerEnrollmentAgentWS agent = new CustomerEnrollmentAgentWS();
                agent.setBrokerId(data[i]);
                if(data.length > i+1 ) {
                    if(data[i+1].trim().length() > 0) {
                        try {
                            new BigDecimal(data[i+1]);
                        } catch (Exception e) {
                            throw new CustomerEnrollmentEntryParserException(customerEnrollment, "Broker ID "+data[i]+" has invalid rate "+data[i+1]);
                        }
                        agent.setRate(data[i+1]);
                    }
                }
                if (validateAgent(agent)) agentList.add(agent);
            }
            customerEnrollment.setCustomerEnrollmentAgents(agentList.toArray(new CustomerEnrollmentAgentWS[agentList.size()]));

            customerEnrollment.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[metaFieldValues.size()]));
        }
        catch (CustomerEnrollmentEntryParserException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CustomerEnrollmentEntryParserException(customerEnrollment, "Incomplete or invalid data", e);
        }

        return customerEnrollment;
    }

    private boolean validateAgent(CustomerEnrollmentAgentWS agent) {
        return agent != null && StringUtils.isNotEmpty(agent.getBrokerId());
    }
}