/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.payment;

import java.io.Serializable;

import com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;


/**
 * @author Emil
 */
@ApiModel(value = "PaymentAuthorization Data",
        description = "PaymentAuthorizationDTOEx Model",
        parent = PaymentAuthorizationDTO.class)
public class PaymentAuthorizationDTOEx extends PaymentAuthorizationDTO implements Serializable {
    private Boolean result;
    
    // BillingHub - Stripe payment gateway integration 
    // Strong customer Authentication (SCA) - authenticating with 3D Secure    
    SecurePaymentWS securePaymentWS;
    
    public PaymentAuthorizationDTOEx() {
        super();
    }
    
    public PaymentAuthorizationDTOEx(PaymentAuthorizationDTO dto) {
        super(dto);
    }

    @ApiModelProperty(value = "true if the the authorization succeeded, false otherwise")
    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }
    
    public SecurePaymentWS getSecurePaymentWS() {
		return securePaymentWS;
	}

	public void setSecurePaymentWS(SecurePaymentWS securePaymentWS) {
		this.securePaymentWS = securePaymentWS;
	}
	
    public String toString() {
        return super.toString() + " result=" + result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentAuthorizationDTOEx)) return false;
        if (!super.equals(o)) return false;

        PaymentAuthorizationDTOEx that = (PaymentAuthorizationDTOEx) o;
        return nullSafeEquals(result, that.result);
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + nullSafeHashCode(result);
        return result1;
    }
}
