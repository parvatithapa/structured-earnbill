package com.sapienter.jbilling.client.suretax;

import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.responsev1.SuretaxResponseV1;
import com.sapienter.jbilling.client.suretax.responsev2.SuretaxResponseV2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business logic for communicating with SureTax
 */
public class SureTaxBL {
    private String url;
    private SuretaxClient suretaxClient;

    private static String sequencePrefix = new SimpleDateFormat("yyyyMMddHHmmSS").format(new Date());
    private static AtomicLong sequence = new AtomicLong(1);

    public SureTaxBL(String url) {
        this.url = url;
        suretaxClient = new SuretaxClient();
    }

    public SuretaxResponseV1 calculateAggregateTax(SuretaxRequest suretaxRequest) {
        return suretaxClient.calculateAggregateTax(suretaxRequest, url);
    }

    public SuretaxResponseV2 calculateLineItemTax(SuretaxRequest suretaxRequest) {
        return suretaxClient.calculateLineItemTax(suretaxRequest, url);
    }

    public static String nextTransactionId() {
        return sequencePrefix + sequence.getAndIncrement();
    }
}
