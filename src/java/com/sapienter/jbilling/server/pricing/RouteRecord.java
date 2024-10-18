/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2013] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.pricing.db.RouteDTO;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *  Route Record
 *  <p>
 *  Represents a record from the provided route table
 *
 *  @author Panche Isajeski
 *  @since 21-Aug-2013
 */
public class RouteRecord implements MatchingRecord {

    private int id;
    private String name;
    private String routeId;
    // route record attributes
    private Map<String, String> attributes = new HashMap<String, String>();

    // route table Id (RouteDTO) that this route record belongs to
    private RouteDTO routeTable;

    private String nextRoute;
    private String product;

    public RouteRecord() {
    }

    public RouteRecord(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public RouteDTO getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(RouteDTO routeTable) {
        this.routeTable = routeTable;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String attributeName, String attributeValue) {
        getAttributes().put(attributeName, attributeValue);
    }

    public String getNextRoute() {
        return nextRoute;
    }

    public void setNextRoute(String nextRoute) {
        this.nextRoute = nextRoute;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }
}
