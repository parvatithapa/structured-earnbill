package com.sapienter.jbilling.server.customerEnrollment.csv;

import com.googlecode.jcsv.CSVStrategy;

public class CustomerEnrollmentCSVStrategy extends CSVStrategy {

    public CustomerEnrollmentCSVStrategy() {
        super(',', '"', '#', true, true);
    }

    public CustomerEnrollmentCSVStrategy(char delimiter, char quoteCharacter, char commentIndicator, boolean skipHeader, boolean ignoreEmptyLines) {
        super(delimiter, quoteCharacter, commentIndicator, skipHeader, ignoreEmptyLines);
    }
}
