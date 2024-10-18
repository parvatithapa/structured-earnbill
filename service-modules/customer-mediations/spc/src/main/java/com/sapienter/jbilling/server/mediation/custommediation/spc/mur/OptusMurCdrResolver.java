package com.sapienter.jbilling.server.mediation.custommediation.spc.mur;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public enum OptusMurCdrResolver {

    DATA("G", "Data Record", "dataCdrResolver");


    private final String switchType;
    private final String cdrType;
    private final String resolverBeanName;

    OptusMurCdrResolver(String switchType, String cdrType, String resolverBeanName) {
        this.switchType = switchType;
        this.cdrType = cdrType;
        this.resolverBeanName = resolverBeanName;
    }

    public String getSwitchType() {
        return switchType;
    }

    public String getCdrType() {
        return cdrType;
    }

    public String getResolverBeanName() {
        return resolverBeanName;
    }

    /**
     * finds cdrResolver for given switch key.
     * @param switchkey
     * @return
     */
    public static OptusMurCdrResolver getResolverBySwitchType(String switchkey) {
        if(StringUtils.isEmpty(switchkey)) {
            throw new IllegalArgumentException("Specify Switch type for cdr resolver");
        }
        for(OptusMurCdrResolver resolver : values()) {
            if(resolver.getSwitchType().equals(switchkey)) {
                return resolver;
            }
        }
        throw new IllegalArgumentException("Cdr resolver not found for switch type "+ switchkey);
    }
}
