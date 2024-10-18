package com.sapienter.jbilling.server.report;

import com.sapienter.jbilling.server.report.db.ReportTypeDTO;

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
        return reportTypeDTO.getReports().stream()
                                         .filter(report -> report.getEntities().stream()
                                                                               .anyMatch(entity -> entityId.equals(entity.getId())))
                                         .count();
    }
}
