package com.sapienter.jbilling.saml.integration.remote.vo;

import java.io.Serializable;

public class MarketplaceInfo implements Serializable {
    private static final long serialVersionUID = 5629970479893675783L;

    private String partner;
    private String baseUrl;

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
