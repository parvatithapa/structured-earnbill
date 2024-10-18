/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.processor;

/**
 * Type of mediation step
 * <p/>
 * Represents pre-defined types of mediation steps to be mapped with a actual mediation step implementation
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public enum MediationStepType {

    USER_CURRENCY,

    EVENT_DATE,

    CURRENT_ORDER,

    ORDER_LINE_ITEM,

    PRICING,

    QUANTITY,

    RESOURCE,

    ITEM_MANAGEMENT,

    // optionally add steps
    RECALCULATE_TAX,
    POST_PROCESS,

    DIFF_MANAGEMENT ,

    // validation steps
    DUPLICATE_RECORD_VALIDATION,
    MEDIATION_RECORD_FORMAT_VALIDATION,
    MEDIATION_RESULT_VALIDATION,

    // AC specific
    USER_ID_AND_CURRENCY,

    ITEM_RESOLUTION,

    DESCRIPTION,

    CDR_TYPE_VALIDATION,

    //Movius Specific Step and validation
    PHONE_NUMBER_VALIDATION,
    JMR_BILLABLE

}
