package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.util.stream.Stream;

/**
 * @author Neelabh
 * @since Jan 29, 2019
 */
public enum MediationServiceType {

    //Service name (abbreviated word) and file name prefix both should be unique
    OPTUS_FIXED_LINE("ofl", "tap_stn", "optusFixedLineCDRResolver"),
    OPTUS_MOBILE("om", "RESELL_", "optusMobileCDRResolver"),
    ENGIN("eng", "ENGINCF", "enginSConnectCDRResolver"),
    SCONNECT("scon", "SConnect_Voice", "enginSConnectCDRResolver"),
    TELSTRA_MOBILE_4G("telstraMobile4G", "Reseller", "telstraMobile4GCDRResolver"),
    AAPT_INTERNET_USAGE("aaptiu", "_Daily_", "aaptInternetUsageCDRResolver"),
    AAPT_VOIP_CTOP("aapvpct","CTOP","aaptVoipCtopCDRResolver"),
    TELSTRA_FIXED_LINE("telstra", "EBILL", "telstraFixedLineCDRResolver"),
    SERVICE_ELEMENTS_VOICE("sevoice", "BBS", "serviceElementsVoiceCDRResolver"),
    SERVICE_ELEMENTS_DATA("sedata", "BBS", "serviceElementsDataCDRResolver"),
    SCONNECT_DATA("sConnect_data","SConnect_Data","vocusInternetUsageCDRResolver");

    private final String serviceName;
    private final String fileNamePrefix;
    private final String cdrResolverBeanName;

    MediationServiceType(String serviceName, String fileNamePrefix, String cdrResolverBeanName) {
        this.serviceName = serviceName;
        this.fileNamePrefix = fileNamePrefix;
        this.cdrResolverBeanName = cdrResolverBeanName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public String getCdrResolverBeanName() {
        return cdrResolverBeanName;
    }

    public static MediationServiceType fromServiceName(String name) {
        for (MediationServiceType type : MediationServiceType.values()) {
            if (type.getServiceName().equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException(name);
    }

    public static String[] getCdrTypes() {
        return Stream.of(MediationServiceType.values()).map(MediationServiceType::getServiceName).toArray(String[]::new);
    }
}
