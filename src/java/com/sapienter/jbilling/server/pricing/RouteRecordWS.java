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

import com.sapienter.jbilling.server.util.NameValueString;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Arrays;

/**
 *  Route Record WS
 *
 *  Represents a record from the provided route table
 *
 *  @author Gerhard Maree
 *  @since 12-Dec-2013
 */
public class RouteRecordWS implements Serializable {

    private Integer id;

    @Size(min=1,max=255, message="validation.error.size,1,255")
    private String name;
    private String routeId;
    // route record attributes
    private NameValueString[] attributes = new NameValueString[0];

    public RouteRecordWS() {
    }

    public RouteRecordWS(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

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

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public NameValueString[] getAttributes() {
        return attributes;
    }

    public void setAttributes(NameValueString[] attributes) {
        this.attributes = attributes;
    }

    /**
     * Create empty attributes.
     *
     * @param count  - nr of attributes to create
     */
    public void createAttributes(Integer count) {
        attributes = new NameValueString[count];
        for(int i=0; i<count; i++) {
            attributes[i] = new NameValueString();
        }
    }

    @Override
    public String toString() {
        return "RouteRecordWS{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", routeId='" + routeId + '\'' +
                ", attributes=" + Arrays.toString(attributes) +
                '}';
    }
}
