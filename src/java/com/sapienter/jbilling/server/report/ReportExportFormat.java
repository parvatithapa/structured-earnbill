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
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            JasperExportManager.exportReportToPdfStream(print, baos);

            byte[] bytes = baos.toByteArray();
            baos.close();

            return new ReportExportDTO(print.getName() + ".pdf", "application/pdf", bytes);
        }
    },

    XLS {
        public ReportExportDTO export(JasperPrint print) throws JRException, IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            JRXlsExporter exporter = new JRXlsExporter();
            exporter.setParameter(JRXlsExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, false);
            exporter.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, false);
            exporter.setParameter(JRXlsExporterParameter.IS_COLLAPSE_ROW_SPAN, true);
            exporter.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_COLUMNS, true);
            exporter.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, true);
            exporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, baos);

            exporter.exportReport();

            byte[] bytes = baos.toByteArray();
            baos.close();

            return new ReportExportDTO(print.getName() + ".xls", "application/vnd.ms-excel", bytes);
        }
    },

    CSV {
        public ReportExportDTO export(JasperPrint print) throws JRException, IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.remove.empty.space.between.rows", "true");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.remove.empty.space.between.columns", "true");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.parameters.override.IgnorePagination", "true");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.*", "title");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.1", "pageHeader");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.band.1","columnHeader");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.band.2","columnHeader");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.report.2","*");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.3", "lastPageFooter");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.4", "pageFooter");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.4", "pageFooter");
            print.getPropertiesMap().setProperty("net.sf.jasperreports.page.break.no.pagination", "apply");

            JRCsvExporter exporter = new JRCsvExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);           
            exporter.exportReport();

            byte[] bytes = baos.toByteArray();
            baos.close();

            return new ReportExportDTO(print.getName() + ".csv", "text/csv", bytes);
        }
    };

    public abstract ReportExportDTO export(JasperPrint print) throws JRException, IOException;

}
