package com.sapienter.jbilling.saml.integration.remote.vo.saml;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class CertificateWS implements Serializable {
    private static final long serialVersionUID = 1232782022243262919L;

    private String uuid;
    private String publicCertificate;
    private String privateKey;
    private String publicKey;
    private String fingerprint;
    private Date expirationDate;
}
