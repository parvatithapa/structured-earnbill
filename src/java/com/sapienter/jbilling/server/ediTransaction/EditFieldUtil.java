package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customerEnrollment.helper.CustomerEnrollmentFileGenerationHelper;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class can be used to extract fields commonly used in EDI records from the customer/company/contact details.
 *
 * @author Gerhard Maree
 * @since 05-11-2015
 */
public class EditFieldUtil {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EditFieldUtil.class));

    private CompanyDTO company;
    private CustomerDTO customer;
    private CompanyWS companyWS;
    private UserWS userWS;
    private ContactDTOEx contact;
    private IEDITransactionBean ediTransactionBean;
    private int entityId;

    private Map<String, Object> companyMetaFieldValueMap;
    private Map<String, Object> customerMetaFieldValueMap;

    public EditFieldUtil(CompanyDTO company, CustomerDTO customer, Date activateDate, IEDITransactionBean ediTransactionBean) {
        this.company = company;
        this.customer = customer;
        this.ediTransactionBean = ediTransactionBean;
        if(activateDate == null) {
            activateDate = TimezoneHelper.companyCurrentDate(company.getId());
        }

        if(customer != null) {
            contact = ContactBL.buildFromMetaField(customer.getBaseUser().getUserId(), activateDate);
        }
        entityId = company.getId();
        buildMetaFieldMaps();
    }

    public EditFieldUtil(CompanyWS company, UserWS customer, Date activateDate) {
        this.companyWS = company;
        this.userWS = customer;
        if(activateDate == null) {
            activateDate = TimezoneHelper.companyCurrentDate(company.getId());
        }

        if(customer != null) {
            contact = ContactBL.buildFromMetaField(userWS.getUserId(), activateDate);
        }
        entityId = companyWS.getId();
        buildMetaFieldMaps();
    }

    public String supplierDuns() {
        return (String) companyMetaFieldValueMap.get(FileConstants.SUPPLIER_DUNS_META_FIELD_NAME);
    }

    public String utilityName() {
        return (String) companyMetaFieldValueMap.get(FileConstants.UTILITY_NAME_META_FIELD_NAME);
    }

    public String supplierName() {
        return (String) companyMetaFieldValueMap.get(FileConstants.SUPPLIER_NAME_META_FIELD_NAME);
    }

    public String utilityDuns() {
        return (String) companyMetaFieldValueMap.get(FileConstants.UTILITY_DUNS_META_FIELD_NAME);
    }

    public String customerAccountNr() {
        return (String) customerMetaFieldValueMap.get(FileConstants.CUSTOMER_ACCOUNT_KEY);
    }

    public String meterCycle() {
        return String.valueOf(customerMetaFieldValueMap.get(FileConstants.CUSTOMER_METER_CYCLE_METAFIELD_NAME));
    }

    public String firstName() {
        return contact.getFirstName();
    }

    public String lastName() {
        return contact.getLastName();
    }

    public String address1() {
        return contact.getAddress1();
    }

    public String address2() {
        return contact.getAddress2();
    }

    public String city() {
        return contact.getCity();
    }

    public String stateProvince() {
        String state = contact.getStateProvince();
        if(state != null && state.length() > 2) {
            state = CustomerEnrollmentFileGenerationHelper.USState.getAbbreviationForState(state.trim());
        }
        return state;
    }

    public String postalCode() {
        return contact.getPostalCode();
    }

    public String phoneNr() {
        return contact.getPhoneNumber();
    }

    public String commodity() {
        String commodity = (String) customerMetaFieldValueMap.get(FileConstants.COMMODITY);
        if(commodity != null && commodity.length() > 0) {
            commodity = ediTransactionBean.getCommodityCode(commodity, entityId);
        }
        return commodity;
    }

    private void buildMetaFieldMaps() {
        companyMetaFieldValueMap = new HashMap<>();
        if(company != null) {
            List<MetaFieldValue> metaFieldValues = company.getMetaFields();
            for(MetaFieldValue metaFieldValue : metaFieldValues) {
                companyMetaFieldValueMap.put(metaFieldValue.getField().getName(), metaFieldValue.getValue());
            }
        } else if (companyWS != null) {
            MetaFieldValueWS[] metaFieldValues = companyWS.getMetaFields();
            for(MetaFieldValueWS metaFieldValue : metaFieldValues) {
                companyMetaFieldValueMap.put(metaFieldValue.getFieldName(), metaFieldValue.getValue());
            }
        }

        customerMetaFieldValueMap = new HashMap<>();
        if(customer != null) {
            List<MetaFieldValue> metaFieldValues = customer.getMetaFields();
            for(MetaFieldValue metaFieldValue : metaFieldValues) {
                customerMetaFieldValueMap.put(metaFieldValue.getField().getName(), metaFieldValue.getValue());
            }

            for(CustomerAccountInfoTypeMetaField mf : customer.getCustomerAccountInfoTypeMetaFields()) {
                customerMetaFieldValueMap.put(mf.getMetaFieldValue().getField().getName(), mf.getMetaFieldValue().getValue());
            }
        } else if(userWS != null) {
            MetaFieldValueWS[] metaFieldValues = userWS.getMetaFields();
            for(MetaFieldValueWS metaFieldValue : metaFieldValues) {
                customerMetaFieldValueMap.put(metaFieldValue.getFieldName(), metaFieldValue.getValue());
            }
        }
    }
}
