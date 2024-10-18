package com.sapienter.jbilling.server.mediation.converter.common;

/**
 * Created by marcolin on 08/10/15.
 */
public interface MediationJobInformations {
    public String mediationJobReader();
    public Object mediationCdrResolver();
    public String mediationJobProcessor();
    public String mediationJobWriter();
}
