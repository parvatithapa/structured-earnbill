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

package com.sapienter.jbilling.server.process;

import java.util.List;

import javax.annotation.Resource;

import com.sapienter.jbilling.server.process.db.BillingProcessInfoDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessInfoDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;

public class BillingProcessInfoBL {

    @Resource
    private BillingProcessInfoDAS billingProcessInfoDas;
    @Resource
    private BillingProcessDAS billingProcessDas;

    public BillingProcessInfoDTO create (Integer billingProcessId, Integer jobExecutionId, Integer totalFailedUsers,
            Integer totalSuccessfulUsers) {
        return billingProcessInfoDas.create(billingProcessDas.find(billingProcessId), jobExecutionId, totalFailedUsers,
                totalSuccessfulUsers);
    }

    public boolean canRestart (Integer billingProcessId) {
        List<BillingProcessInfoDTO> list = billingProcessInfoDas.findExecutionsInfoByBillingProcessId(billingProcessId);
        if (list.isEmpty()) {
            return false;
        }
        return list.get(0).getTotalFailedUsers() > 0;
    }

    public List<BillingProcessInfoDTO> findExecutionsInfoByBillingProcessId (Integer billingProcessId) {
        return billingProcessInfoDas.findExecutionsInfoByBillingProcessId(billingProcessId);
    }
}
