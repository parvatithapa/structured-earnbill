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
package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.lang.Integer;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompanyWS implements java.io.Serializable, WSSecured {

    private static final long serialVersionUID = 20140605L;

    private int id;
    private Integer currencyId;
    private Integer languageId;
    @Size(min = 5, max = 100, message = "validation.error.size,5,100")
    private String description;
    @Valid
    private ContactWS contact;
    private Integer owningEntityId;
    private String timezone;

    private String customerInformationDesign;
    private Integer uiColor;
    @Valid
    private MetaFieldValueWS[] metaFields;
    private Map<Integer, ArrayList<MetaFieldValueWS>> companyInfoTypeFieldsMap = new HashMap<Integer, ArrayList<MetaFieldValueWS>>();
    private Integer[] companyInformationTypes = null;
    @Pattern(regexp = "^([a-zA-Z0-9#\\!$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]|\\\\ ))+(\\.([a-zA-Z0-9!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]))+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.([A-Za-z]{2,})$", message = "validation.error.email")
    @Size(min = 6, max = 200, message = "validation.error.size,6,200")
    private String failedEmailNotification;

    public CompanyWS() {
    }

    public CompanyWS(int i) {
        id = i;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContactWS getContact() {
        return contact;
    }

    public void setContact(ContactWS contact) {
        this.contact = contact;
    }

    public String getCustomerInformationDesign() {
        return customerInformationDesign;
    }

    public void setCustomerInformationDesign(String customerInformationDesign) {
        this.customerInformationDesign = customerInformationDesign;
    }

    public Integer getUiColor() {
        return uiColor;
    }

    public void setUiColor(Integer uiColor) {
        this.uiColor = uiColor;
    }

    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    public Integer[] getCompanyInformationTypes() {
        return companyInformationTypes;
    }

    public void setCompanyInformationTypes(Integer[] companyInformationTypes) {
        this.companyInformationTypes = companyInformationTypes;
    }

    public Map<Integer, ArrayList<MetaFieldValueWS>> getCompanyInfoTypeFieldsMap() {
        return companyInfoTypeFieldsMap;
    }

    public void setCompanyInfoTypeFieldsMap(Map<Integer, ArrayList<MetaFieldValueWS>> companyInfoTypeFieldsMap) {
        this.companyInfoTypeFieldsMap = companyInfoTypeFieldsMap;
    }

    public String getFailedEmailNotification() {
        return this.failedEmailNotification;
    }

    public void setFailedEmailNotification(String email) {
        this.failedEmailNotification = email;
    }

    public String toString() {
        return "CompanyWS [id=" + id + ", currencyId=" + currencyId
                + ", languageId=" + languageId + ", description=" + description
                + ", contact=" + contact + "]";
    }

    @Override
    public Integer getOwningEntityId() {
        return this.owningEntityId;
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }

    public String getTimezone() { return timezone; }

    public void setTimezone(String timezone) { this.timezone = timezone; }

    public MetaFieldValueWS getMetaFieldByName(String name) {
        MetaFieldValueWS returnValue = null;
        if(metaFields != null && name != null && !name.trim().isEmpty()) {
            for (int i = 0; i < metaFields.length; i++) {
                if (name.equals(metaFields[i].getFieldName())) {
                    returnValue = metaFields[i];
                    break;
                }
            }
        }
        return returnValue;
    }
}
