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
package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.jobs;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;

/**
 * @author Harshad Pathan
 * @since 18/02/2016
 */
public class LiveReceptionMediationJob extends DefaultMediationJob {
	 
	 protected LiveReceptionMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
    	super(job, lineConverter, resolver,writer, recycleJob);
    }

    public static MediationJob getMediationJobInstance() {
        return new LiveReceptionMediationJob(FullCreativeConstants.LIVE_RECEPTION_MEDIATION_CONFIGURATION, 
        									 FullCreativeConstants.INBOUND_CALL_MEDIATION_CONVERTER_BEAN,
        									 FullCreativeConstants.INBOUND_CALL_MEDIATION_CDR_RESOLVER_BEAN, 
        									 FullCreativeConstants.JMR_DEFAULT_WRITER_BEAN, 
        									 FullCreativeConstants.LIVE_RECEPTION_RECYCLE_CONFIGURATION);
    }
    
    public static MediationJob getRecycleJobInstance() {
        return new LiveReceptionMediationJob(FullCreativeConstants.LIVE_RECEPTION_RECYCLE_CONFIGURATION, 
        									 FullCreativeConstants.INBOUND_CALL_MEDIATION_CONVERTER_BEAN,
        									 FullCreativeConstants.INBOUND_CALL_MEDIATION_CDR_RESOLVER_BEAN, 
        									 FullCreativeConstants.JMR_DEFAULT_WRITER_BEAN,
        									 null);
    }
 }
 