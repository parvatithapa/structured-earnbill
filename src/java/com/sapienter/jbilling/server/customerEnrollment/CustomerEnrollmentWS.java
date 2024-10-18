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

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.customerEnrollment;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/** @author Emil */
public class CustomerEnrollmentWS implements WSSecured, Serializable {

    private Integer id;
    private Integer accountTypeId;
    private Integer entityId;
    private String companyName;
    private Integer customerId; //User Id of customer created by customer enrollment
    private String accountNumber;

    private Integer parentUserId;
    private Integer parentEnrollmentId;
    @ConvertToTimezone
    private Date createDatetime;
    private CustomerEnrollmentStatus status;
    private Integer deleted=0;

    private String accountTypeName;
    @Size(min = 0, max = 1000, message = "validation.error.size.exceed,1000")
    private String comment;

    @Valid
    private MetaFieldValueWS[] metaFields;
    private CustomerEnrollmentCommentWS[] customerEnrollmentComments;
    private CustomerEnrollmentAgentWS[] customerEnrollmentAgents;
    private Boolean bulkEnrollment=false;
    private String brokerCatalogVersion;

    private String message;
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    public Object getMetaFieldValue(String metaFieldName) {
        Object value = null;
        for(MetaFieldValueWS metaFieldValueWS : metaFields) {
            if(metaFieldValueWS.getFieldName().equals(metaFieldName)) {
                value = metaFieldValueWS.getValue();
            }
        }
        return value;
    }

    public Object getMetaFieldValueByGroupId(String metaFieldName, int groupId) {
        Object value = null;
        for(MetaFieldValueWS metaFieldValueWS : metaFields) {
            if(metaFieldValueWS.getGroupId()!=null && metaFieldValueWS.getGroupId().equals(groupId) && metaFieldValueWS.getFieldName().equals(metaFieldName)) {
                value = metaFieldValueWS.getValue();
                break;
            }
        }
        return value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountTypeId = accountTypeId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public CustomerEnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerEnrollmentStatus status) {
        this.status = status;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public CustomerEnrollmentCommentWS[] getCustomerEnrollmentComments() {
        return customerEnrollmentComments;
    }

    public void setCustomerEnrollmentComments(CustomerEnrollmentCommentWS[] customerEnrollmentComments) {
        this.customerEnrollmentComments = customerEnrollmentComments;
    }

    public CustomerEnrollmentAgentWS[] getCustomerEnrollmentAgents() {
        return customerEnrollmentAgents;
    }

    public void setCustomerEnrollmentAgents(CustomerEnrollmentAgentWS[] customerEnrollmentAgents) {
        this.customerEnrollmentAgents = customerEnrollmentAgents;
    }

    public Integer getParentEnrollmentId() {
        return parentEnrollmentId;
    }

    public void setParentEnrollmentId(Integer parentEnrollmentId) {
        this.parentEnrollmentId = parentEnrollmentId;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public String getAccountTypeName() {
        return accountTypeName;
    }

    public void setAccountTypeName(String accountTypeName) {
        this.accountTypeName = accountTypeName;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(Integer parentUserId) {
        this.parentUserId = parentUserId;
    }

    @Override
    public Integer getOwningEntityId() {
        return getEntityId();
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean isBulkEnrollment() {
        return bulkEnrollment;
    }

    public void setBulkEnrollment(Boolean bulkEnrollment) {
        this.bulkEnrollment = bulkEnrollment;
    }

    public String getBrokerCatalogVersion() {
        return brokerCatalogVersion;
    }

    public void setBrokerCatalogVersion(String brokerCatalogVersion) {
        this.brokerCatalogVersion = brokerCatalogVersion;
    }
}
