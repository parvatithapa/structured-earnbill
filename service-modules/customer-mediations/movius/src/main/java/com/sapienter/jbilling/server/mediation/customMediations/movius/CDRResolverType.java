package com.sapienter.jbilling.server.mediation.customMediations.movius;

public enum CDRResolverType {

    OUT_GOING_CALL_TYPE("outgoing-call", "outGoingCallsCDRResolver"), 
    OUT_GOING_SMS_DETAILS_TYPE("outgoing-sms-details", "outGoingSmsDetailsCDRResolver"),
    OUT_GOING_SMS_TYPE("outgoing-sms", "outGoingSmsCDRResolver"),
    IN_COMING_CALL_TYPE("incoming-call", "incomingCallsCDRResolver"),
    IN_COMING_SMS_DETAILS_TYPE("incoming-sms-details", "inComingSmsDetailsCDRResolver");
    
    private final String cdrType;
    private final String cdrResolverBeanName;
    
    CDRResolverType(String cdrType, String cdrResolverBeanName) {
        this.cdrType = cdrType;
        this.cdrResolverBeanName = cdrResolverBeanName;
    }

    public String getCdrType() {
        return cdrType;
    }

    public String getCdrResolverBeanName() {
        return cdrResolverBeanName;
    }
}
