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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;

import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.CollectionUtils;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.report.db.ReportDAS;
import com.sapienter.jbilling.server.report.db.ReportDTO;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;

/**
 * ReportBL
 *
 * @author Brian Cowdery
 * @since 08/03/11
 */
public class ReportBL {

    public static final String SESSION_IMAGE_MAP = "jasper_images";
    public static final String PARAMETER_ENTITY_ID = "entity_id";
    public static final String PARAMETER_ENTITY_TIMEZONE = "entity_timezone";
    public static final String PARAMETER_SUBREPORT_DIR = "SUBREPORT_DIR";
    public static final String CHILD_ENTITIES = "child_entities";
    public static final String PARAMETER_USER_IS_ADMIN = "user_is_admin";
    public static final String PARAMETER_FORMAT = "format";
    public static final String BASE_PATH = Util.getSysProp("base_dir") + File.separator + "reports" + File.separator;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ReportBL.class));
    public static final String PARAMETER_CATEGORY_ID = "category_id";
    public static final String PARAMETER_PHONE_NUMBER_META_FIELD_NAME = "phone_number";
    public static final String PARAMETER_SPC_COLUMN_BEFORE_TAX = "spc_column_before_tax";
    public static final String PARAMETER_SPC_COLUMN_TAX = "spc_column_tax";
    public static final String PARAMETER_SPC_COLUMN_AFTER_TAX = "spc_column_after_tax";
    private ReportDTO report;
    private Locale locale;
    private Integer entityId;
    private String entityTimezone;
    private Boolean userIsAdmin;
    private String format;

    private ReportDAS reportDas;

    public ReportBL() {
        init();
    }

    public ReportBL(Integer id, Integer userId, Integer entityId) {
        init();
        set(id);
        setLocale(userId);
        this.entityId = entityId;
    }

    public ReportBL(ReportDTO report, Locale locale, Integer entityId, String entityTimezone) {
        init();
        this.report = report;
        this.locale = locale;
        this.entityId = entityId;
        this.entityTimezone = entityTimezone;
    }

    public void updateAdminRole(Integer userId) {
        UserDTO user = new UserDAS().find(userId);
        userIsAdmin = user.getRoles().stream().anyMatch(it -> it.getRoleTypeId().equals(Constants.TYPE_SYSTEM_ADMIN) || it.getRoleTypeId().equals(Constants.TYPE_ROOT));
    }
    

    /**
     * Run the given report design file with the given parameter list.
     *
     * @param reportName report name
     * @param report     report design file
     * @param baseDir    report base directory
     * @param parameters report parameters
     * @param locale     user locale
     * @param entityId   entity ID
     * @return JasperPrint output file
     */
    public static JasperPrint run(String reportName, File report, String baseDir, Map<String, Object> parameters,
                                  Locale locale, Integer entityId, String format, String entityTimezone, List<Integer> childs, Boolean userIsAdmin) {

        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);

        // add user locale, entity id and sub report directory
        parameters.put(JRParameter.REPORT_LOCALE, locale);
        parameters.put(PARAMETER_ENTITY_ID, entityId);
        parameters.put(PARAMETER_ENTITY_TIMEZONE, entityTimezone);
        parameters.put(PARAMETER_SUBREPORT_DIR, baseDir);
        parameters.put(PARAMETER_USER_IS_ADMIN, userIsAdmin);
        parameters.put(PARAMETER_FORMAT, format);
        parameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, bundle);
        if (ReportExportFormat.CSV.name().equals(format)) {
            parameters.put(JRParameter.IS_IGNORE_PAGINATION, Boolean.TRUE);
        }
        if(reportName.equals("sales_tax_ato_report")) {
            parameters.put(PARAMETER_SPC_COLUMN_BEFORE_TAX, SPCConstants.SPC_COLUMN_BEFORE_TAX);
            parameters.put(PARAMETER_SPC_COLUMN_TAX, SPCConstants.SPC_COLUMN_TAX);
            parameters.put(PARAMETER_SPC_COLUMN_AFTER_TAX, SPCConstants.SPC_COLUMN_AFTER_TAX);
        }

        CompanyDTO companyDTO = new CompanyDAS().find(entityId);
        MetaFieldValue metaFieldValue = companyDTO.getMetaField(SpaConstants.CATEGORY_ID);
        if(null != metaFieldValue){
            parameters.put(PARAMETER_CATEGORY_ID, metaFieldValue.getValue());
        }

        metaFieldValue = companyDTO.getMetaField(SpaConstants.PHONE_NUMBER_META_FIELD_NAME);
        if(null != metaFieldValue){
            parameters.put(PARAMETER_PHONE_NUMBER_META_FIELD_NAME, metaFieldValue.getValue());
        }

        List<Integer> ids = new ArrayList<>(0);
        ids.add(0);
        metaFieldValue = companyDTO.getMetaField(SpaConstants.CATEGORY_IDS);
        if(null != metaFieldValue){
            String categoryIds = metaFieldValue.getValue().toString().trim();
            ids = Stream.of(categoryIds.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
        }
        parameters.put(SpaConstants.INCLUDE_ITEMS, ids);

        if (CollectionUtils.isEmpty(childs)) {
            childs = new ArrayList<>(0);
            childs.add(0);
        }
        parameters.put(CHILD_ENTITIES, childs);

        LOG.debug("Generating report " + report.getPath() + " ...");
        LOG.debug(parameters.toString());

        // run report
        JasperPrint print = null;

        try (FileInputStream inputStream = new FileInputStream(report)) {
            ReportBuilder builder = ReportBuilder.getReport(reportName);
            if (builder != null) {
                LOG.debug("Filling report builder");
                print = JasperFillManager.fillReport(inputStream, parameters, new JRMapCollectionDataSource(builder.getData(entityId, childs, parameters)));        
            } else {
                DataSource dataSource = Context.getBean(Context.Name.DATA_SOURCE);
                // If tx is active then DataSourceUtils  returns tx bind connection
                // else it will get it from Connection Pool. DataSourceUtils will not always get connection from pool,
                // so code will not use additional connection from pool when tx is active,
                // so it will reduce number of busy connection at a time.
                Connection connection = DataSourceUtils.getConnection(dataSource);
                try {
                    print = JasperFillManager.fillReport(inputStream, parameters, connection);
                } finally {
                    // Release Connection immediately if tx is not active else tx is active then spring
                    // will manage connection and return it to pool once tx roll back or commit.
                    DataSourceUtils.releaseConnection(connection, dataSource);
                }
            }
            print.setName(reportName);
            return print;
        } catch (FileNotFoundException e) {
            LOG.error("Report design file " + report.getPath() + " not found.", e);
            throw new SessionInternalError("Report design file not found.", e);
        } catch (JRException | IOException e) {
            LOG.error("Exception occurred generating jasper report.", e);
            throw new SessionInternalError("Exception occurred generating jasper report.", e);
        } catch (RuntimeException e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Returns the base path for this Jasper Report file on disk.
     *
     * @return base path for the Jasper Report file
     */
    public static String getReportBaseDir(ReportDTO report) {
        return BASE_PATH + report.getType().getName() + File.separator;
    }

    public static File getReportFile(ReportDTO report) {
        return report.getFileName() != null ? new File(getReportFilePath(report)) : null;
    }

    public static String getReportFilePath(ReportDTO report) {
        return getReportBaseDir(report) + report.getFileName();
    }

    public static File getReportFile(ReportDTO report, String reportFileName) {
        return reportFileName != null ? new File(getReportFilePath(report, reportFileName)) : null;
    }

    public static String getReportFilePath(ReportDTO report, String reportFileName) {
        return getReportBaseDir(report) + reportFileName;
    }

    private void init() {
        this.reportDas = new ReportDAS();
    }

    public void set(Integer id) {
        this.report = reportDas.find(id);
    }

    public void setLocale(Integer userId) {
        this.locale = new UserBL(userId).getLocale();
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public ReportDTO getEntity() {
        return this.report;
    }

    /**
     * Render report as HTML to the given HTTP response stream. This method also dumps
     * the generated report image files into a session Map (<code>Map<String, byte[]></code>)
     * so that they can be retrieved and rendered.
     *
     * @param response response stream
     * @param session session to place images map
     * @param imagesUrl the URL of the action where the image map can be accessed by name - e.g., "images?image="
     */
    public void renderHtml(HttpServletResponse response, HttpSession session, String imagesUrl) {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        try (PrintWriter writer = response.getWriter()) {
            JasperPrint print = run();

            if (print != null) {
                Map<String, byte[]> images = new HashMap<>();
                session.setAttribute(SESSION_IMAGE_MAP, images);

                HtmlExporter exporter = new HtmlExporter();
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
                exporter.setParameter(JRExporterParameter.OUTPUT_WRITER, writer);
                exporter.setParameter(JRHtmlExporterParameter.IMAGES_MAP, images);
                exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, imagesUrl);
                exporter.exportReport();
            }
        } catch (IOException | JRException e) {
            LOG.error("Exception occurred exporting jasper report to HTML.", e);
            throw new SessionInternalError(e);
        }

    }

    /**
     * Exports a report using the given format.
     *
     * @param format export type
     * @return exported report
     * @throws JRException 
     */
    public ReportExportDTO export(ReportExportFormat format) {
        LOG.debug("Exporting report to " + format.name() + " ...");
        this.format = format.name();
        JasperPrint print = run();

        ReportExportDTO export = null;
        if (print != null) {
            try {
                export = format.export(print);
            } catch (JRException e) {
                LOG.error("Exception occurred exporting jasper report to " + format.name(), e);
            } catch (IOException e) {
                LOG.error("Exception occurred getting exported bytes", e);
            }
        }

        return export;
    }

    /**
     * Run this report.
     * <p/>
     * This method assumes that the report object contains parameters that have been populated
     * with a value to use when running the report.
     *
     * @return JasperPrint output file
     */
    public JasperPrint run() {
        return run(report.getName(),
                getReportFile(report),
                getReportBaseDir(report),
                report.getParameterMap(),
                locale,
                entityId,
                format,
                entityTimezone,
                report.getChildEntities(),
                userIsAdmin);
    }

    public void addAditionalParameters(Map<String, Object> parameters) {
        if(parameters != null && !parameters.isEmpty()) {
            report.getAditionalReportParameters().putAll(parameters);
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public String getEntityTimezone() {
        return entityTimezone;
    }

    public Integer getEntityId() {
        return entityId;
    }


}
