package com.sapienter.jbilling.server.boa.batch;

import org.springframework.batch.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FieldSetFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rivero
 * @since  05/01/16.
 */
public class ContinuationRecordTokenizer extends DelimitedLineTokenizer {
    private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

    /**
     * parses record that begins with '88' (Continuation record)
     */
    @Override
    public FieldSet tokenize(String line) {
        if (line == null) {
            line = "";
        }

        //Create a FieldSet of only fields that are used in the payment processing by the application. Discard rest of the fields
        List<String> tempTokens = new ArrayList<String>(doTokenize(line));
        List<String> tokens = tempTokens.subList(0, 2);
//        The entire line is added here as token because we need raw data in paymentNote field.
        tokens.add(line);
        String[] values = (String[]) tokens.toArray(new String[tokens.size()]);

        return fieldSetFactory.create(values, names);
    }
}
