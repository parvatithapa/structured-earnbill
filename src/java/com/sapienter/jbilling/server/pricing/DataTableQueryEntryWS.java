package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.util.db.StringList;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Value Object for DataTableQueryEntryDTO used by the API for external communication.
 *
 * @author Gerhard
 * @since 31/01/14
 * @see com.sapienter.jbilling.server.pricing.db.DataTableQueryEntryDTO
 */
public class DataTableQueryEntryWS implements Serializable {
    private int id;
    @NotNull(message="validation.error.notnull")
    private Integer routeId;
    @NotNull(message="validation.error.notnull")
    private StringList columns;
    @Valid
    private DataTableQueryEntryWS nextQuery;
    private Integer versionNum;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public StringList getColumns() {
        return columns;
    }

    public void setColumns(StringList columns) {
        this.columns = columns;
    }

    public DataTableQueryEntryWS getNextQuery() {
        return nextQuery;
    }

    public void setNextQuery(DataTableQueryEntryWS nextQuery) {
        this.nextQuery = nextQuery;
    }

    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        return "DataTableQueryEntryWS{" +
                "id=" + id +
                ", routeId=" + routeId +
                ", columns=" + columns +
                ", nextQuery=" + nextQuery +
                ", versionNum=" + versionNum +
                '}';
    }
}
