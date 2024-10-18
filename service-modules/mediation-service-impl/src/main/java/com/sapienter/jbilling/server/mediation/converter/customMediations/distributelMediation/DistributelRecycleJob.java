/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;

/**
 * Created by pablo_galera on 31/01/17.
 */
public class DistributelRecycleJob extends DefaultMediationJob {

    protected DistributelRecycleJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        super(job, lineConverter, resolver, writer, recycleJob);
    }

    public static MediationJob getJobInstance() {
        return new DistributelRecycleJob("distributelRecycleJob", "distributelMediationConverter",
                "distributelRecordMediationCdrResolver", "jmrDefaultWriter", null);
    }
}