package com.sapienter.jbilling.saml.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.saml.integration.remote.type.ErrorCode;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

@XmlRootElement(name = "result")
@Data
@NoArgsConstructor
@Getter
@Setter
public class APIResult implements Serializable {
    private static final long serialVersionUID = -7599199539526987847L;

    private boolean success;
    private boolean asynchronous = false;
    private ErrorCode errorCode;
    private String message;
    private String accountIdentifier;
    private String userIdentifier;
    private String id;

    public APIResult(boolean success, String message) {
        this(success, null, message);
    }

    public APIResult(boolean success, ErrorCode errorCode, String message) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
    }

    @XmlTransient
    @JsonIgnore
    public boolean isAsynchronous() {
        return asynchronous;
    }
}
