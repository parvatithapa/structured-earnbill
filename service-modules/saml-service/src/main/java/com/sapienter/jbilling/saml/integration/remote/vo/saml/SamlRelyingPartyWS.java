package com.sapienter.jbilling.saml.integration.remote.vo.saml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SamlRelyingPartyWS implements Serializable {
    private static final long serialVersionUID = 1600586313870037663L;

    private String uuid;
    private String idpIdentifier;
    private String assertionConsumerServiceUrl;
    private String audienceUrl;
    private String relayState;
    private String nameIdType;
    private String authenticationContext;
    private Integer notBeforeMinutes;
    private Integer notAfterMinutes;
    private Map<String, SamlRelyingPartyAttributeWS> attributes = new HashMap<>();
    private CertificateWS certificate;
}
