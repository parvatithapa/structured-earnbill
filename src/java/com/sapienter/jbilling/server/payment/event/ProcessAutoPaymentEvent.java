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
package com.sapienter.jbilling.server.payment.event;

import com.sapienter.jbilling.server.system.event.Event;

public class ProcessAutoPaymentEvent implements Event {
    private final Integer entityId;
    private final Integer userId;
    
    public ProcessAutoPaymentEvent(Integer userId, Integer entityId) {
        this.userId = userId;
        this.entityId= entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Process Auto Payment";
    }

    public final Integer getUserId() {
        return userId;
    }

}
