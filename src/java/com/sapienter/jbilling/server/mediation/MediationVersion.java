package com.sapienter.jbilling.server.mediation;

/**
 * MediationVersion
 *
 * @author Panche Isajeski
 * @since 01/30/2012
 */
public enum MediationVersion {

    MEDIATION_VERSION_2_0 ("2.0"),
    MEDIATION_VERSION_3_0 ("3.0"),
    MEDIATION_VERSION_4_0 ("4.0");

    private String version;

    private MediationVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public boolean isEqualTo(String version) {
        return this.equals(MediationVersion.getMediationVersion(version));
    }

    public static MediationVersion getMediationVersion(String version) {

        for (MediationVersion mediationVersion : MediationVersion.values()) {
            if (mediationVersion.getVersion().equals(version)) {
                return mediationVersion;
            }
        }

        return null;
    }
}
