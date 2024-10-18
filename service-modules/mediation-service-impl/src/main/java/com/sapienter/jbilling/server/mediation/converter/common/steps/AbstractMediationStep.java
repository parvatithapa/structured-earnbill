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

import java.util.List;

import org.apache.commons.lang.NotImplementedException;

/**
 * Abstract implementation of the mediation step
 * containing default implementation of most common methods
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public abstract class AbstractMediationStep<T> implements IMediationStep<T> {


    public boolean executeStep(Integer entityId, T result, List<PricingField> fields) {
        //TODO MODULARIZATION: THIS IS OLD, NORMALLY IT WILL THROW EXCEPTION
        throw new NotImplementedException();
    }
}
