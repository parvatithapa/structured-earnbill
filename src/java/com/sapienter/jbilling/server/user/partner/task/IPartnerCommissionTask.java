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
package com.sapienter.jbilling.server.user.partner.task;

/**
 * Plug-in category that holds the logic that calculates the partners commissions.
 */
public interface IPartnerCommissionTask {
    /**
     * Calculates all the agents' commissions for the given entity
     *
     * @param entityId company id
     */
    public void calculateCommissions(Integer entityId);
}
