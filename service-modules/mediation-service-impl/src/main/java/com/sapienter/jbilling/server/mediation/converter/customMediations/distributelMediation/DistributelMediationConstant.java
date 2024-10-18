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

package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

/**
 * Created by igutierrez on 25/01/17.
 */
public class DistributelMediationConstant {
    public static String CUSTOMER_METAFIELD_NAME = "Customer Name";
    public static String LEGANCY_ACCOUNT_NUMBER_METAFIELD_NAME = "Legacy Account Number";

    //CSV field name
    public static String CUSTOMER_NUMBER = "CustomerID";
    public static String INVOICE_DATE = "BillingDate";
    public static String DESCRIPTIVE_TEXT = "InvoiceDescriptiveText";
    public static String AMOUNT = "DollarAmount";
    public static String DETAIL_FILES_NAME = "DetailBaseFilename";
    public static String DETAIL_TYPE = "DetailType";
    public static String BILLING_IDENTIFIER = "BillingIdentifier";
    public static String SUBSCRIPTION_ORDER_ID = "SubscriptionOrderID";

    //Order information
    public static Integer QUANTITY_ITEM = 1;

    public static String MCF_RATED_ITEM_TYPE = "MCF Rated";

    public static String DATE_FORMAT = "yyyyMMdd";
}