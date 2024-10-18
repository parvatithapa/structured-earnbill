package com.sapienter.jbilling.saml.integration.remote.vo;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Data
@XmlRootElement(name = "account")
public class AccountInfo implements Serializable {
    private static final long serialVersionUID = -400499571158068365L;

    private String accountIdentifier;
    private String status;
}
