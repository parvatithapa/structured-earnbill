package com.sapienter.jbilling.server.user;


import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

public class RouteWS implements WSSecured, Serializable {

    private Integer id;
    @NotNull(message = "validation.error.notnull")
    @Size(min=1,max=50, message="validation.error.size,1,50")
    private String name;
    private Integer entityId;
    private String tableName;
    private Boolean rootTable;
    private Boolean routeTable = true;
    @Size(min=0,max=150, message="validation.error.size,0,150")
    private String outputFieldName;
    @Size(min=0,max=255, message="validation.error.size,0,255")
    private String defaultRoute;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    @Size(min = 1, message = "validation.error.notnull")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultRoute() {
        return defaultRoute;
    }

    public void setDefaultRoute(String defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Boolean getRootTable() {
        return rootTable;
    }

    public void setRootTable(Boolean rootTable) {
        this.rootTable = rootTable;
    }

    public Boolean getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(Boolean routeTable) {
        this.routeTable = routeTable;
    }

    public String getOutputFieldName() {
        return outputFieldName;
    }

    public void setOutputFieldName(String outputFieldName) {
        this.outputFieldName = outputFieldName;
    }

    @Override
    public String toString() {
        return "RouteWS{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", entityId=" + entityId +
                ", tableName='" + tableName + '\'' +
                ", rootTable='" + rootTable + '\'' +
                ", routeTable='" + routeTable + '\'' +
                ", outputFieldName='" + outputFieldName + '\'' +
                ", defaultRoute='" + defaultRoute + '\'' +
                '}';
    }

    /**
     * Returns the entity ID of the company owning the secure object, or null
     * if the entity ID is not available.
     *
     * @return owning entity ID
     */
    @Override
    public Integer getOwningEntityId() {
        return entityId;
    }

    /**
     * Returns the user ID of the user owning the secure object, or null if the
     * user ID is not available.
     *
     * @return owning user ID
     */
    @Override
    public Integer getOwningUserId() {
        return null;
    }
}
