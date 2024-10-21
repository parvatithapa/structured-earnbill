package com.sapienter.jbilling.server.report;

import com.sapienter.jbilling.server.report.db.ReportTypeDTO;
import grails.plugin.springsecurity.SpringSecurityUtils;

import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_BANK_USER_REPORT;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_CRM_USER_REPORT;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_FINANCE_TOTAL_REPORT;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_INACTIVE_SUBSCRIBER_NUMBERS_REPORT;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.REPORT_TYPE_ADENNET;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_AUDIT_LOG_REPORT;

/**
 * ReportTypeBL class.
 * 
 * @author Leandro Bagur
 * @since 10/11/17.
 */
public class ReportTypeBL {

    /**
     * Get amount of reports of one report type filtered by entity.
     * @param reportTypeDTO report type dto
     * @param entityId entity id
     * @return long
     */
    public long getTotalReportsByEntity(ReportTypeDTO reportTypeDTO, Integer entityId) {
        long totalReports = reportTypeDTO.getReports().stream()
                                         .filter(report -> report.getEntities().stream()
                                                                               .anyMatch(entity -> entityId.equals(entity.getId())))
                                         .count();
        if(reportTypeDTO.getName().equals(REPORT_TYPE_ADENNET)) {
            if(SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_BANK_USER_REPORT))
                totalReports --;
            if(SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_CRM_USER_REPORT))
                totalReports --;
            if(SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_FINANCE_TOTAL_REPORT))
                totalReports --;
            if(SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_INACTIVE_SUBSCRIBER_NUMBERS_REPORT))
                totalReports --;
            if(SpringSecurityUtils.ifNotGranted(PERMISSION_VIEW_AUDIT_LOG_REPORT))
                totalReports --;
        }
        return totalReports;
    }
}
