package com.sapienter.jbilling.server.invoiceTemplate.domain;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;

/**
 * Created by Klim on 04.06.2014.
 */
public class SubReportDataSource {

    private final SubReportDataSourceType type;
    private final String path;

    public SubReportDataSource(SubReportDataSourceType type, String path) {
        this.type = type;
        this.path = path;
    }

    public JRDataSource toJRDataSource () {
        try {
            return type.toJRDataSource(path);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

}
