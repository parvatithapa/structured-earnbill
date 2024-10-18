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

package com.sapienter.jbilling.server.mediation.converter.common.steps;


import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;

import java.util.List;

/**
 * Single unit of work in the process of resolving the CDRs into JMR.
 * <p/>
 * This step will be executed from the mediation resolver
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public interface IMediationStep<T> {

    @Deprecated
    boolean executeStep(Integer entityId, T result, List<PricingField> fields);

    /**
     *
     * @param context
     * @return
     */
    boolean executeStep(MediationStepContext context);
}
