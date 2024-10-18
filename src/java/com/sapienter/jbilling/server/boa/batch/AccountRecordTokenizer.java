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
public class AccountRecordTokenizer extends DelimitedLineTokenizer {
    private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

    /**
     * parses record that begins with '03' (Account Identifier and Summary Status record)
     */
    @Override
    public FieldSet tokenize(String line) {
        if (line == null) {
            line = "";
        }

        //Create a FieldSet of only fields that are used in the payment processing by the application. Discard rest of the fields
        List<String> tokens = new ArrayList<>(doTokenize(line));
        tokens = tokens.subList(0, 2);

        String[] values = (String[]) tokens.toArray(new String[tokens.size()]);
        return fieldSetFactory.create(values, names);
    }
}
