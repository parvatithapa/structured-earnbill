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
package com.sapienter.jbilling.server.process.db;

import java.util.List;

import org.hibernate.Query;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class BillingProcessInfoDAS extends AbstractDAS<BillingProcessInfoDTO> {

    public BillingProcessInfoDTO create (BillingProcessDTO billingProcessDTO, Integer jobExecutionId,
            Integer totalFailedUsers, Integer totalSuccessfulUsers) {
        BillingProcessInfoDTO dto = new BillingProcessInfoDTO(billingProcessDTO, jobExecutionId, totalFailedUsers,
                totalSuccessfulUsers);

        dto = save(dto);
        return dto;
    }

    //@formatter:off
    private static final String HQL = String.join(System.getProperty("line.separator"),
            "select a",
            "  from BillingProcessInfoDTO a",
            " where a.billingProcess.id = :billingProcessId",
            " order by a.id desc");
    //@formatter:on

    public List<BillingProcessInfoDTO> findExecutionsInfoByBillingProcessId (Integer billingProcessId) {

        Query query = getSession().createQuery(HQL);
        query.setParameter("billingProcessId", billingProcessId);
        return (List<BillingProcessInfoDTO>) query.list();
    }
}
