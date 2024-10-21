/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * ExportType
 *
 * @author Brian Cowdery
 * @since 09/03/11
 */
public enum ReportExportFormat {

    PDF {
        public ReportExportDTO export(JasperPrint print) throws JRException, IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            JasperExportManager.exportReportToPdfStream(print, byteArrayOutputStream);

            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return new ReportExportDTO(print.getName() + ".pdf", "application/pdf", bytes);
        }
    },

    XLS {
        public ReportExportDTO export(JasperPrint print) throws JRException, IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //exclude iReport page footer from excel file
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.xls.exclude.origin.band.2","pageFooter");
            JRXlsExporter exporter = getJrXlsExporter(print, byteArrayOutputStream);

            exporter.exportReport();

            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return new ReportExportDTO(print.getName() + ".xls", "application/vnd.ms-excel", bytes);
        }
    },

    CSV {
        public ReportExportDTO export(JasperPrint print) throws JRException, IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.remove.empty.space.between.rows", "true");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.remove.empty.space.between.columns", "true");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.parameters.override.IgnorePagination", "true");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.1", "pageHeader");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.band.1","columnHeader");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.band.2","columnHeader");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.report.2","*");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.3", "lastPageFooter");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.4", "pageFooter");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.4", "pageFooter");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.page.break.no.pagination", "apply");

            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleWriterExporterOutput(byteArrayOutputStream));
            exporter.exportReport();

            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return new ReportExportDTO(print.getName() + ".csv", "text/csv", bytes);
        }
    };

    private static JRXlsExporter getJrXlsExporter(JasperPrint print, ByteArrayOutputStream byteArrayOutputStream) {
        SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
        configuration.setDetectCellType(Boolean.TRUE);
        configuration.setWhitePageBackground(Boolean.FALSE);
        configuration.setCollapseRowSpan(Boolean.TRUE);
        configuration.setAutoFitPageHeight(Boolean.TRUE);
        configuration.setRemoveEmptySpaceBetweenColumns(Boolean.TRUE);
        configuration.setRemoveEmptySpaceBetweenRows(Boolean.TRUE);

        JRXlsExporter exporter = new JRXlsExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));
        exporter.setConfiguration(configuration);
        return exporter;
    }

    public abstract ReportExportDTO export(JasperPrint print) throws JRException, IOException;

}
