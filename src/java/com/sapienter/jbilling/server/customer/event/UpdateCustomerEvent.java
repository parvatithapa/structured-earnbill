/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.customer.event;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.google.common.collect.MapDifference.ValueDifference;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.balance.CustomerProperties;

/**
 * Created by igutierrez on 3/28/17.
 */
public class UpdateCustomerEvent implements Event {

    private final Integer customerId;
    private final CustomerProperties oldCustomer;
    private final Map<Integer, Map<String, ValueDifference<Object>>> oldNewAITValueMapByName;
    private final Integer entityId;

    public UpdateCustomerEvent(Integer customerId, CustomerProperties oldCustomer,
            Map<Integer, Map<String, ValueDifference<Object>>> oldNewAITValueMapByName, Integer entityId) {
        this.customerId = customerId;
        this.oldCustomer = oldCustomer;
        this.oldNewAITValueMapByName =  oldNewAITValueMapByName;
        this.entityId = entityId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public CustomerProperties getOldCustomer() {
        return oldCustomer;
    }

    public Map<Integer, Map<String, ValueDifference<Object>>> getOldNewAITValueMapByName() {
        return MapUtils.isEmpty(oldNewAITValueMapByName) ? Collections.emptyMap() : oldNewAITValueMapByName;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "UpdateCustomerEvent";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UpdateCustomerEvent [customerId=");
        builder.append(customerId);
        builder.append(", oldCustomer=");
        builder.append(oldCustomer);
        builder.append(", oldNewAITValueMapByName=");
        builder.append(oldNewAITValueMapByName);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append("]");
        return builder.toString();
    }

}
