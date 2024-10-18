package com.sapienter.jbilling.server.invoiceTemplate.domain;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;

/**
 * Created by Klim on 04.06.2014.
 */
public enum SubReportDataSourceType {

    EMPTY,

    JBILLING,

    XML {
        @Override
        public JRDataSource toJRDataSource (final String expression) throws JRException {
            return new JRXmlDataSource(expression);
        }
    },

    CSV {
        @Override
        public JRDataSource toJRDataSource (final String expression) throws JRException {
            return new JRCsvDataSource(expression);
        }
    };

    public JRDataSource toJRDataSource (final String expression) throws JRException {
        return new JREmptyDataSource();
    }
}
