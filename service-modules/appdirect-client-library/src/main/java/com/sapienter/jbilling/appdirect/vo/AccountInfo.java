package com.sapienter.jbilling.appdirect.vo;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Data;

@XmlRootElement(name = "account")
@Data @Builder
public class AccountInfo implements Serializable {
    private static final long serialVersionUID = -400499571158068365L;

    /**
     * Account identifier
     */
    private String accountIdentifier;

    /**
     * Company entitlement status. May be INITIALIZED, FAILED, FREE_TRIAL, FREE_TRIAL_EXPIRED,
     * ACTIVE, SUSPENDED, or CANCELLED.
     */
    private String status;

    /**
     * Parent account identifier. Applies to add-on products.
     */
    private String parentAccountIdentifier;

}
