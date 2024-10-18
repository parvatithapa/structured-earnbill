package com.sapienter.jbilling.server.customerEnrollment.csv;

import com.googlecode.jcsv.writer.CSVEntryConverter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class CustomerEnrollmentResponseEntryConverter implements CSVEntryConverter<CustomerEnrollmentResponse> {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    @Override
    public String[] convertEntry(CustomerEnrollmentResponse customerEnrollmentResponse) {
        int cols = 5 + customerEnrollmentResponse.getBrokerIds().size();

        String[] columns = new String[cols];

        columns[0] = customerEnrollmentResponse.getLdc();
        columns[1] = customerEnrollmentResponse.getAccountNumber();
        columns[2] = customerEnrollmentResponse.getCode().name();
        columns[3] = customerEnrollmentResponse.getReason();
        columns[4] = dateFormat.format(customerEnrollmentResponse.getTimestamp());

        int i = 5;
        for(String brokerId : customerEnrollmentResponse.getBrokerIds()) {
            columns[i++] = brokerId;
        }
        return columns;
    }
}