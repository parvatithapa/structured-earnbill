package com.sapienter.jbilling.batch;

import org.springframework.batch.core.ExitStatus;

public class BatchConstants {

    private BatchConstants() {
    }

    public static final ExitStatus COMPLETED_REVIEW = new ExitStatus("COMPLETED_REVIEW");
    public static final ExitStatus FAILED_REVIEW    = new ExitStatus("FAILED_REVIEW");

    public static final String PARAM_ENTITY_ID = "entityId";
    public static final String PARAM_BILLING_DATE = "billingDate";
    public static final String PARAM_AGEING_DATE = "ageingDate";
    public static final String PARAM_PERIOD_VALUE = "periodValue";
    public static final String PARAM_PERIOD_TYPE = "periodType";
    public static final String PARAM_REVIEW = "review";
    public static final String PARAM_UNIQUE = "unique";
    public static final String GENERATE_INVOICE_STEP_NAME  = "generateInvoices";
    public static final String EMAIL_AND_PAYMENT_STEP_NAME = "emailAndPayment";
}
