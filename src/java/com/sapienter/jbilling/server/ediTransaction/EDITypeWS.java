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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** @author Emil */
public class EDITypeWS implements WSSecured, Serializable {

    private Integer id;

    @NotEmpty(message="validation.error.notnull")
    @Size(min=1,max=100, message="validation.error.size,1,100")
    private String name;

    private String path;

    private Integer global=0;

    @NotNull(message="validation.error.notnull")
    private Integer entityId;

    private List<Integer> entities = new ArrayList<Integer>(0);
    @Valid
    private List<EDIFileStatusWS> ediStatuses = new ArrayList<EDIFileStatusWS>(0);

    @ConvertToTimezone
    private Date createDatetime;

    @NotEmpty(message="validation.error.notnull")
    private String ediSuffix;

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getGlobal() {
        return global;
    }

    public void setGlobal(Integer global) {
        this.global = global;
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

    public List<Integer> getEntities() {
        return entities;
    }

    public void setEntities(List<Integer> entities) {
        this.entities = entities;
    }

    public List<EDIFileStatusWS> getEdiStatuses() {
        return ediStatuses;
    }

    public void setEdiStatuses(List<EDIFileStatusWS> ediStatuses) {
        this.ediStatuses = ediStatuses;
    }

    @Override
    public Integer getOwningEntityId() {
        return getEntityId();
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }

    public String getEdiSuffix() {
        return ediSuffix;
    }

    public void setEdiSuffix(String ediSuffix) {
        this.ediSuffix = ediSuffix;
    }

    @Override
    public String toString() {
        return "EDITypeWS{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", global=" + global +
                ", entityId=" + entityId +
                ", createDatetime=" + createDatetime +
                '}';
    }
}
