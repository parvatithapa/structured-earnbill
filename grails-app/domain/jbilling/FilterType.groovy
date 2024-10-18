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

package jbilling

/**
 * FilterType
 
 * @author Brian Cowdery
 * @since  30-11-2010
 */
enum FilterType {
    ALL, INVOICE, ORDER, PRODUCT, CUSTOMER, PARTNER, PAYMENT, BILLINGPROCESS, MEDIATIONPROCESS, LOG, PROVISIONING_CMD, PROVISIONING_REQ, PLAN, CUSTOMER_ENROLLMENT, EDI_TYPE, EDI_FILE, CREDITNOTE, OUTBOUNDINTERCHANGE
}