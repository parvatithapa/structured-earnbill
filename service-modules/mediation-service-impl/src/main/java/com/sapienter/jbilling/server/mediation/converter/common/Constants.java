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


package com.sapienter.jbilling.server.mediation.converter.common;



/**
 * Constants for the client
 *
 */

public final class Constants {
    // mediation record status
    public final static Integer MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE = new Integer(1);
    public final static Integer MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE = new Integer(2);
    public final static Integer MEDIATION_RECORD_STATUS_ERROR_DETECTED = new Integer(3);
    public final static Integer MEDIATION_RECORD_STATUS_ERROR_DECLARED = new Integer(4);
    
}
