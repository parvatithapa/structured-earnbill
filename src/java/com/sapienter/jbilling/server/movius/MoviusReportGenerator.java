package com.sapienter.jbilling.server.movius;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JasperPrint;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.movius.CDRType;
import com.sapienter.jbilling.server.report.BackgroundReportExportUtil;
import com.sapienter.jbilling.server.report.ReportBL;
import com.sapienter.jbilling.server.report.ReportExportFormat;
import com.sapienter.jbilling.server.report.db.ReportDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.LogoType;

public class MoviusReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(MoviusReportGenerator.class); 
    private static final String USER_NAME = "user_name";
    private static final String LOGO = "logo";
    private static final String CURRENCY_SYMBOL = "currency_symbol";
    private static final String EXPORT_FORMAT = "export_format";
    private static final String SUB_ACCOUNT_LIST = "sub_account_list";
    private static final String CDR_TYPE = "cdrType";
    
    private ReportBL reportBL;
    
    private MoviusReportGenerator(ReportBL reportBL) {
        this.reportBL = reportBL;
    }
    
    public static MoviusReportGenerator of(ReportBL reportBL) {
        return new MoviusReportGenerator(reportBL);
    }
    
    public void exportBackgroundReport(ReportExportFormat format, CDRType cdrType, Integer userId) {
        ReportDTO report = reportBL.getEntity();
        String baseDir = ReportBL.getReportBaseDir(report);
        Locale locale = reportBL.getLocale();
        Integer entityId = reportBL.getEntityId();
        String timeZone = reportBL.getEntityTimezone();
        if (ReportExportFormat.CSV.equals(format)) {
            if(Objects.isNull(cdrType)) {
                throw new SessionInternalError("Please Select CDR Type from Select Box For CSV Export");
            }
            // Adding Movius Specific parameters
            Map<String, Object> aditionalParameters = buildParameters(format, cdrType);
            logger.debug("Build Parameters {}", aditionalParameters);
            reportBL.addAditionalParameters(aditionalParameters);
            String reportFileName = cdrType.getReportName();
            logger.debug("Exporting  cdr {} report in format CSV", cdrType);
            JasperPrint jasperPrint = ReportBL.run(reportFileName, ReportBL.getReportFile(report, reportFileName), baseDir,
                    report.getParameterMap(), locale, entityId, format.name(), timeZone, report.getChildEntities(), null);
            if(Objects.nonNull(jasperPrint)) {
                BackgroundReportExportUtil.generateCSVReportBackground(jasperPrint, userId, entityId, report.getParameterMap());
            }
        } else if (ReportExportFormat.XLS.equals(format)) {
            logger.debug("Exporting  cdr {} report in XLS format", Arrays.asList(CDRType.values()));
            List<JasperPrint> prints = Arrays.stream(CDRType.values())
                    .map(type -> {
                        // Adding Movius Specific parameters for each cdr type
                        Map<String, Object> aditionalParameters = buildParameters(format, type);
                        logger.debug("Build Parameters {}", aditionalParameters);
                        reportBL.addAditionalParameters(aditionalParameters);
                        String reportFileName = type.getReportName();
                        return  ReportBL.run(reportFileName, ReportBL.getReportFile(report, reportFileName), baseDir,
                                report.getParameterMap(), locale, entityId, format.name(), timeZone, report.getChildEntities(), null);
                    }).collect(Collectors.toList());
            BackgroundReportExportUtil.generateXLSReportBackground(prints, userId, entityId,report.getParameterMap());
        }

    }

    private Map<String, Object> buildParameters(ReportExportFormat format, CDRType cdrType) {
        Map<String, Object> parameters = new HashMap<>();
        Integer userId = (Integer) reportBL.getEntity().getParameterMap().get("user_id");
        if (userId != null) {
            UserDTO userDTO = new UserBL(userId).getEntity();
            parameters.put(USER_NAME, userDTO.getUserName());
            parameters.put(SUB_ACCOUNT_LIST, BackgroundReportExportUtil.getCustomersInHierarchy(userId));
            parameters.put(LOGO, LogoType.INVOICE.getFile(reportBL.getEntityId()));

            String currencySymbol = StringEscapeUtils.unescapeHtml(userDTO.getCurrency().getSymbol());
            parameters.put(CURRENCY_SYMBOL, currencySymbol);
        }
        parameters.put(CDR_TYPE, cdrType.name());
        logger.debug("Export format [{}]", format);
        parameters.put(EXPORT_FORMAT, format.name());
        return parameters;
    }
    
}
