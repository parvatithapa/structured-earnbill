package com.sapienter.jbilling.appdirect.vo;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlRootElement(name = "result")
@Getter
@Setter
@NoArgsConstructor
public class BillingAPIResult implements Serializable {
    private static final long serialVersionUID = -7027507409588330850L;

    private boolean success;
    private String message;

    public BillingAPIResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
