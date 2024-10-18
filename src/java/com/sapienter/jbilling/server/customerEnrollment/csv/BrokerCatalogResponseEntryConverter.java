package com.sapienter.jbilling.server.customerEnrollment.csv;

import com.googlecode.jcsv.writer.CSVEntryConverter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class BrokerCatalogResponseEntryConverter implements CSVEntryConverter<BrokerCatalogResponse> {

    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String[] convertEntry(BrokerCatalogResponse brokerCatalogResponse) {
        String[] columns = new String[15];

        columns[0] = brokerCatalogResponse.getProductId();
        columns[1] = brokerCatalogResponse.getProductName();
        columns[2] = brokerCatalogResponse.getActiveStartDate() != null ? dateFormat.format(brokerCatalogResponse.getActiveStartDate()) :  null;
        columns[3] = brokerCatalogResponse.getActiveEndDate() != null ? dateFormat.format(brokerCatalogResponse.getActiveEndDate()) : null;
        columns[4] = brokerCatalogResponse.getDivision();
        columns[5] = brokerCatalogResponse.getLdc();
        columns[6] = brokerCatalogResponse.getCommodity();
        columns[7] = brokerCatalogResponse.getCustomerType();
        columns[8] = brokerCatalogResponse.getBillingModel();
        columns[9] = brokerCatalogResponse.getProductType();
        columns[10] = brokerCatalogResponse.getRate() != null ? brokerCatalogResponse.getRate().toPlainString() : null;
        columns[11] = brokerCatalogResponse.getEtf() != null ? brokerCatalogResponse.getEtf().toPlainString() : null;
        columns[12] = brokerCatalogResponse.getTerm() != null ? brokerCatalogResponse.getTerm().toString() : null;
        columns[13] = brokerCatalogResponse.getDiscountPeriod() != null ? brokerCatalogResponse.getDiscountPeriod().toString() : null;
        columns[14] = brokerCatalogResponse.getBlockSize() != null ? brokerCatalogResponse.getBlockSize().toString() : null;

        return columns;
    }
}