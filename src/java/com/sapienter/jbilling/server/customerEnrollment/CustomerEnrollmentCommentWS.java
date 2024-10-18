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

import com.sapienter.jbilling.server.timezone.ConvertToTimezone;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/** @author Emil */
public class CustomerEnrollmentCommentWS implements  Serializable {

    private int id;

    @NotNull(message="validation.error.notnull")
    private String comment;

    @ConvertToTimezone
    @NotNull(message="validation.error.notnull")
    private Date dateCreated;

    private CustomerEnrollmentWS customerEnrollment;

    @NotNull(message="validation.error.notnull")
    private Integer userId;

    private String userName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public CustomerEnrollmentWS getCustomerEnrollmentWS() {
        return customerEnrollment;
    }

    public void setCustomerEnrollmentWS(CustomerEnrollmentWS customerEnrollment) {
        this.customerEnrollment = customerEnrollment;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }


}
