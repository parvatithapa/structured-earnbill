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

import com.sapienter.jbilling.server.system.event.Event;

public class UpdateDistributelCustomersInvoiceTemplateEvent implements Event {

    private final String templateName;
    private final Integer entityId;
    private final Integer templateId;
    private final static String eventName = "Update Customer Invoice Template Event";

    public UpdateDistributelCustomersInvoiceTemplateEvent(String templateName, Integer entityId, Integer templateId) {
        this.templateName = templateName;
        this.entityId = entityId;
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Integer getTemplateId() {
        return templateId;
    }


    @Override
    public String getName() {
        return eventName;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }
}
