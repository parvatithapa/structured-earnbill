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

import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/** @author Emil */
public class EDIFileFieldWS implements  Serializable {

    private Integer id;
    private String key;
    private String value;
    private String comment;
    private Integer order;

    public EDIFileFieldWS() {
    }

    public EDIFileFieldWS(String key, String value, String comment, int order) {
        this.key = key;
        this.value = value;
        this.comment = comment;
        this.order = order;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "EDIFileRecordDataWS{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", comment='" + comment + '\'' +
                ", order='" + order + '\'' +
                '}';
    }
}
