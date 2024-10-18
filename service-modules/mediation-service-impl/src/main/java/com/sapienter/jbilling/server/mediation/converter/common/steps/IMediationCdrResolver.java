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


import com.sapienter.jbilling.server.mediation.ICallDataRecord;

/**
 * Mediation CDR resolver is responsible for resolving the CDR-s i.e. executing
 * predefined set of step to resolve certain information about the CDR (customer
 * id, item id, event date ...)
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public interface IMediationCdrResolver {

    /**
     * Resolves the CDR record (fields) into mediation result
     *
     * @param result step result object. Keep the information of the step processing
     * @param record the recort that is being processed
     *
     * @return mediation step resolution status object
     * */
    MediationResolverStatus resolveCdr(MediationStepResult result, ICallDataRecord record);
}
