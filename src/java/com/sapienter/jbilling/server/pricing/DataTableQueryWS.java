package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.pricing.DataTableQueryEntryWS;
import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Value Object for DataTableQueryDTO used by the API for external communication.
 *
 * @author Gerhard Maree
 * @since 31/01/2014
 * @see com.sapienter.jbilling.server.pricing.db.DataTableQueryDTO
 */
public class DataTableQueryWS implements WSSecured, Serializable {
    private int id;
    @NotNull(message="validation.error.notnull")
    @Size(min=1,max=50, message="validation.error.size,1,50")
    private String name;
    private int routeId;
    private int global = 0;
    private int userId;
    private Integer versionNum;
    @NotNull(message="validation.error.notnull")
    @Valid
    private DataTableQueryEntryWS rootEntry;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRouteId() {
        return routeId;
    }

    public void setRouteId(int routeId) {
        this.routeId = routeId;
    }

    public int getGlobal() {
        return global;
    }

    public void setGlobal(int global) {
        this.global = global;
    }

    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    public DataTableQueryEntryWS getRootEntry() {
        return rootEntry;
    }

    public void setRootEntry(DataTableQueryEntryWS rootEntry) {
        this.rootEntry = rootEntry;
    }

    @Override
    public String toString() {
        return "DataTableQueryWS{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", routeId=" + routeId +
                ", global=" + global +
                ", versionNum=" + versionNum +
                ", rootEntry=" + rootEntry +
                '}';
    }

    @Override
    public Integer getOwningEntityId() {
        return null;
    }

    @Override
    public Integer getOwningUserId() {
        return userId;
    }
}
