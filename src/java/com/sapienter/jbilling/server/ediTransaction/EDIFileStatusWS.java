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
package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** @author Emil */
public class EDIFileStatusWS implements Serializable {

    private Integer id;
    @NotEmpty(message="validation.error.notnull")
    @Size(min=1,max=100, message="validation.error.size,1,100")
    private String name;
    @ConvertToTimezone
    private Date createDatetime;
    private List<Integer> childStatuesIds=new ArrayList<Integer>();
    private List<EDIFileExceptionCodeWS> exceptionCodes=new ArrayList<EDIFileExceptionCodeWS>();
    private List<EDIFileStatusWS> associatedEDIStatuses = new ArrayList<EDIFileStatusWS>();
    private boolean isError = false;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EDIFileExceptionCodeWS> getExceptionCodes() {
        return exceptionCodes;
    }

    public void setExceptionCodes(List<EDIFileExceptionCodeWS> exceptionCodes) {
        this.exceptionCodes = exceptionCodes;
    }

    public List<Integer> getChildStatuesIds() {
        return childStatuesIds;
    }

    public void setChildStatuesIds(List<Integer> childStatuesIds) {
        this.childStatuesIds = childStatuesIds;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    public void setAssociatedEDIStatuses(List<EDIFileStatusWS> associatedEDIStatuses) {
        this.associatedEDIStatuses = associatedEDIStatuses;
    }

    public List<EDIFileStatusWS> getAssociatedEDIStatuses() {
        return associatedEDIStatuses;
    }

    @Override
    public String toString() {
        return "EDIFileStatusWS{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createDatetime=" + createDatetime +
                '}';
    }
}
