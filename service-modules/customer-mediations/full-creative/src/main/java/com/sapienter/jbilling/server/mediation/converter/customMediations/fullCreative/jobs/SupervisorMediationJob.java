package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.jobs;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.DefaultMediationJob;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;

/**
 * 
 * @author Harshad Pathan
 *
 */
public class SupervisorMediationJob extends DefaultMediationJob {
	
    protected SupervisorMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
    	super(job, lineConverter, resolver,writer, recycleJob);
    }
	
    public static MediationJob getMediationJobInstance() {
        return new SupervisorMediationJob(FullCreativeConstants.SUPERVISOR_MEDIATION_CONFIGURATION, 
        								  FullCreativeConstants.INBOUND_CALL_MEDIATION_CONVERTER_BEAN,
        								  FullCreativeConstants.INBOUND_CALL_MEDIATION_CDR_RESOLVER_BEAN, 
        								  FullCreativeConstants.JMR_DEFAULT_WRITER_BEAN, 
        								  FullCreativeConstants.SUPERVISOR_RECYCLE_CONFIGURATION);
    } 
    
    public static MediationJob getRecycleJobInstance() {
        return new SupervisorMediationJob(FullCreativeConstants.SUPERVISOR_RECYCLE_CONFIGURATION, 
        								  FullCreativeConstants.INBOUND_CALL_MEDIATION_CONVERTER_BEAN,
        								  FullCreativeConstants.INBOUND_CALL_MEDIATION_CDR_RESOLVER_BEAN, 
        								  FullCreativeConstants.JMR_DEFAULT_WRITER_BEAN, 
        								  null);
    }
}