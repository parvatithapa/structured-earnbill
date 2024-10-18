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

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;


/**
 * @author Emil
 */
public class AgeingDTOEx extends AgeingEntityStepDTO {
    private Integer statusId = null;
    private String statusStr = null;
    private Boolean inUse = null;
    private Integer canLogin = null;
    
    /**
     * @return
     */
    public Integer getStatusId() {
        return statusId;
    }

    /**
     * @param statusId
     */
    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    /**
     * @return
     */
    public Boolean getInUse() {
        return inUse;
    }

    /**
     * @param inUse
     */
    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }

    /**
     * @return
     */
    public String getStatusStr() {
        return statusStr;
    }

    /**
     * @param statusStr
     */
    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
    }

    /**
     * @return
     */
    public Integer getCanLogin() {
        return canLogin;
    }

    /**
     * @param canLogin
     */
    public void setCanLogin(Integer canLogin) {
        this.canLogin = canLogin;
    }

	public String toString() {
		return "AgeingDTOEx [statusId=" + statusId + ", statusStr=" + statusStr
				+ ", notification=" + getSendNotification()
				+ ", retry=" + getRetryPayment() + ", collectionType=" 
				+ getCollectionType() +", suspend="
				+ getSuspend() + "]";
	}

}
