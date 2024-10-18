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
package com.sapienter.jbilling.server.diameter;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAttribute;

public final class DiameterResultWS implements Serializable {

    private static final long serialVersionUID = -9146878106496518031L;

	// Valid diameter result.
    public static final int DIAMETER_SUCCESS = 2001;

    public static final int DIAMETER_REALM_NOT_SERVED = 3003;
    public static final int DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE = 4011;
    public static final int DIAMETER_END_USER_SERVICE_DENIED = 4010;
    public static final int DIAMETER_CREDIT_LIMIT_REACHED = 4012;

    public static final int DIAMETER_UNKNOWN_SESSION_ID = 5002;
    public static final int DIAMETER_UNABLE_TO_COMPLY = 5012;
    public static final int DIAMETER_USER_UNKNOWN = 5030;
    public static final int DIAMETER_RATING_FAILED = 5031;
    public static final int DIAMETER_INVALID_AVP_VALUE = 5004;

    @XmlAttribute
    private final int resultCode;
    @XmlAttribute
    private final BigDecimal grantedUnits;
    @XmlAttribute
    private final boolean terminateWhenConsumed;
    @XmlAttribute
    private final int quotaThreshold;

    public DiameterResultWS() {
    	this(DIAMETER_SUCCESS, BigDecimal.ZERO, false, 0);
    }
    
    public DiameterResultWS(int resultCode) {
        this(resultCode, BigDecimal.ZERO, false, 0);
    }

    public DiameterResultWS(int resultCode, BigDecimal grantedUnits,
                            boolean terminateWhenConsumed, int quotaThreshold) {

        this.resultCode = resultCode;
        this.grantedUnits = grantedUnits;
        this.terminateWhenConsumed = terminateWhenConsumed;
        this.quotaThreshold = quotaThreshold;
    }
    
    @Override
    public String toString() {
    	return String.format("DiameterResultWS { resultCode = [%d], " +
    			"grantedUnits=[%s], terminateWhenConsumed=[%s], " +
    			"quotaThreshold=[%d] }", resultCode, grantedUnits.toPlainString(), 
    			Boolean.toString(terminateWhenConsumed), quotaThreshold);
    }

    /**
     * The outcome of the operation.
     *
     * @return Diameter outcome code.
     */
    public int getResultCode() {
        return resultCode;
    }

    /**
     * Units granted. Equals the requested units except in case where insufficient
     * funds are available, in such case the system returns the nearest amount of
     * units for the available funds. In case of errors, this amount equals -1.
     *
     * @return a number indicating the amount of units reserved and allowed for the
     *         call.
     */
    public BigDecimal getGrantedUnits() {
        return grantedUnits;
    }

    /**
     * If true, this value signals that the session should be terminated when
     * grantedTime expires (this implies that this session will not support further
     * extendSession() calls). When false, subsequent extendSession() calls are allowed
     * for the session. This for example is true when grantedUnits &lt; requestedUnits
     * because available funds are not sufficient to further extend the session. In
     * case of errors, this equals false.
     *
     * @return true if this session can be extended further, false otherwise.
     */
    public boolean isTerminateWhenConsumed() {
        return terminateWhenConsumed;
    }

    /**
     * If terminateWhenConsumed=false, this value indicates the amount of units necessary
     * for extending the session. Calls for session extension should be done keeping this
     * amount of units as reserve. This value is a configuration parameter in jBilling and
     * does not change. It should roughly equate the amount of time needed by jBilling to
     * perform session extension. In case of errors, this amount equals 0.
     *
     * @return number of units to keep as reserve when extending sessions.
     */
    public int getQuotaThreshold() {
        return quotaThreshold;
    }
}
