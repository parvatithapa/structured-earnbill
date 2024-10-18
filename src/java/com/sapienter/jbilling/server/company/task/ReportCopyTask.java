package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.server.report.db.ReportDAS;
import com.sapienter.jbilling.server.report.db.ReportDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import java.util.List;

/**
 * @author Javier Rivero
 * @since 19/01/16.
 */
public class ReportCopyTask extends AbstractCopyTask {
    public static final String INSERT_ENTITY_REPORT_MAP = "INSERT INTO entity_report_map (report_id, entity_id) VALUES (%s, %s)";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Class dependencies[] = new Class[]{};
    ReportDAS reportDAS;
    CompanyDAS companyDAS;

    public ReportCopyTask() {
        reportDAS = new ReportDAS();
        companyDAS = new CompanyDAS();
    }

    @Override
    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);
        List<ReportDTO> reports = reportDAS.findAllReportsByCompany(entityId);
        List<ReportDTO> copyReports = reportDAS.findAllReportsByCompany(targetEntityId);
        JdbcTemplate jdbcTemplate = Context.getBean(Context.Name.JDBC_TEMPLATE);

        if (copyReports.isEmpty()) {
            for (ReportDTO report : reports) {
                String query = String.format(INSERT_ENTITY_REPORT_MAP, report.getId(), targetEntityId);
                jdbcTemplate.execute(query);
            }
        }

        logger.debug("ReportCopyTask has been completed");
    }

    @Override
    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<ReportDTO> reports = reportDAS.findAllReportsByCompany(targetEntityId);
        return CollectionUtils.isNotEmpty(reports);
    }

    @Override
    public Class[] getDependencies() {
        return dependencies;
    }
}
