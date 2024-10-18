package com.sapienter.jbilling.server.boa.batch;

import org.springframework.batch.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FieldSetFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rivero
 * @since 05/01/16.
 */
public class TransactionRecordTokenizer extends DelimitedLineTokenizer {
    private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();
    private static final String FUND_TYPE_AVAILABILITY_UNKNOWN = "Z";
    private static final String FUND_TYPE_AVAILABILITY_DISTRIBUTED = "S";
    private static final int RECORD_TYPE_16_FUND_TYPE_INDEX = 3;
    private static final int RECORD_TYPE_16_FUND_TYPE_Z_BANK_REF_INDEX = 4;
    private static final int RECORD_TYPE_16_FUND_TYPE_S_BANK_REF_INDEX = 7;
    private static final int RECORD_TYPE_16_FUND_TYPE_Z_CUST_REF_INDEX = 5;
    private static final int RECORD_TYPE_16_FUND_TYPE_S_CUST_REF_INDEX = 8;

    /**
     * parses record that begins with '16' (Transaction Detail record)
     */
    @Override
    public FieldSet tokenize(String line) {
        if (line == null) {
            line = "";
        }

        //Create a FieldSet of only fields that are used in the payment processing by the application. Discard rest of the fields
        List<String> tempTokens = new ArrayList<String>(doTokenize(line));
        //List<String> tokens = tempTokens.subList(0, 3);
        List<String> tokens = new ArrayList<String>();
        tokens.add(tempTokens.get(0));
        tokens.add(tempTokens.get(1));
        tokens.add(tempTokens.get(2));

        if (tempTokens.get(RECORD_TYPE_16_FUND_TYPE_INDEX).equals(FUND_TYPE_AVAILABILITY_UNKNOWN)) {
            //If fund type is Z, bank reference number will be at position 4
            tokens.add(tempTokens.get(RECORD_TYPE_16_FUND_TYPE_Z_BANK_REF_INDEX));
            //If fund type is Z, customer reference number will be at position 5
            tokens.add(tempTokens.get(RECORD_TYPE_16_FUND_TYPE_Z_CUST_REF_INDEX));
        } else if (tempTokens.get(RECORD_TYPE_16_FUND_TYPE_INDEX).equals(FUND_TYPE_AVAILABILITY_DISTRIBUTED)) {
            //If fund type is S, bank reference number will be at position 7
            tokens.add(tempTokens.get(RECORD_TYPE_16_FUND_TYPE_S_BANK_REF_INDEX));
            //If fund type is S, customer reference number will be at position 8
            tokens.add(tempTokens.get(RECORD_TYPE_16_FUND_TYPE_S_CUST_REF_INDEX));
        }

//        The entire line is added here as token because we need raw data in paymentNote field.
        tokens.add(line);
        String[] values = (String[]) tokens.toArray(new String[tokens.size()]);
        return fieldSetFactory.create(values, names);
    }
}
