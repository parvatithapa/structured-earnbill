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

import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.Date;

/** @author Emil */
public class EDIFileWS implements WSSecured, Serializable {

    private Integer id;
    @NotEmpty(message="validation.error.notnull")
    private String name;
    private Integer entityId;
    private EDITypeWS ediTypeWS;
    private TransactionType type;
    @ConvertToTimezone
    private Date createDatetime;
    private EDIFileStatusWS ediFileStatusWS;
    private String exceptionCode;
    private int versionNum = 0;
    private String comment;
    private Integer userId;
    private Date startDate;
    private Date endDate;
    private String utilityAccountNumber;

    private EDIFileRecordWS[] EDIFileRecordWSes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public EDITypeWS getEdiTypeWS() {
        return ediTypeWS;
    }

    public void setEdiTypeWS(EDITypeWS ediTypeWS) {
        this.ediTypeWS = ediTypeWS;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Override
    public Integer getOwningEntityId() {
        return getEntityId();
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }

    public EDIFileRecordWS[] getEDIFileRecordWSes() {
        return EDIFileRecordWSes;
    }

    public void setEDIFileRecordWSes(EDIFileRecordWS[] EDIFileRecordWSes) {
        this.EDIFileRecordWSes = EDIFileRecordWSes;
    }

    public EDIFileStatusWS getEdiFileStatusWS() {
        return ediFileStatusWS;
    }

    public void setEdiFileStatusWS(EDIFileStatusWS ediFileStatusWS) {
        this.ediFileStatusWS = ediFileStatusWS;
    }

    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getUtilityAccountNumber() {
        return utilityAccountNumber;
    }

    public void setUtilityAccountNumber(String utilityAccountNumber) {
        this.utilityAccountNumber = utilityAccountNumber;
    }

    public String findField(String recordName, String fieldName) {
       for(EDIFileRecordWS record : getEDIFileRecordWSes()) {
           if(record.getHeader().equals(recordName)) {
               for(EDIFileFieldWS field : record.getEdiFileFieldWSes()) {
                   if(field.getKey().equals(fieldName)) {
                       return field.getValue();
                   }
               }
               return null;
           }
       }
       return null;
    }

    @Override
    public String toString() {
        return "EDIFileWS{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", entityId=" + entityId +
                ", ediTypeWS=" + ediTypeWS +
                ", type=" + type +
                ", createDatetime=" + createDatetime +
                '}';
    }
}
