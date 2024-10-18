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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/** @author Emil */
public class EDIFileRecordWS implements WSSecured, Serializable {

    private Integer id;
    private Integer ediFileId;

    private String header;

    private Integer recordOrder;

    private EDIFileFieldWS[] ediFileFieldWSes;

    private Integer entityId;

    @ConvertToTimezone
    private Date createDatetime;

    private Integer totalFileField;

    private String comment;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getHeader() {
        return header;
    }

    public Integer getEdiFileId() {
        return ediFileId;
    }

    public void setEdiFileId(Integer ediFileId) {
        this.ediFileId = ediFileId;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Integer getRecordOrder() {
        return recordOrder;
    }

    public void setRecordOrder(Integer recordOrder) {
        this.recordOrder = recordOrder;
    }

    public EDIFileFieldWS[] getEdiFileFieldWSes() {
        return ediFileFieldWSes;
    }

    public void setEdiFileFieldWSes(EDIFileFieldWS[] ediFileFieldWSes) {
        this.ediFileFieldWSes = ediFileFieldWSes;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public Integer getTotalFileField() {
        return totalFileField;
    }

    public void setTotalFileField(Integer totalFileField) {
        this.totalFileField = totalFileField;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public Integer getOwningEntityId() {
        return getEntityId();
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }


    @Override
    public String toString() {
        return "EdiFileRecordWS{" +
                "id=" + id +
                ", ediFile=" + ediFileId +
                ", header='" + header + '\'' +
                ", recordOrder=" + recordOrder +
                ", ediFileRecordDataWSes=" + Arrays.toString(ediFileFieldWSes) +
                ", entityId=" + entityId +
                ", createDatetime=" + createDatetime +
                '}';
    }
}
