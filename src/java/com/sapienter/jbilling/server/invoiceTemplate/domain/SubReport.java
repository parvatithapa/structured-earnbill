package com.sapienter.jbilling.server.invoiceTemplate.domain;

import static com.sapienter.jbilling.server.invoiceTemplate.domain.SubReportDataSourceType.EMPTY;

/**
 * @author elmot
 */
public class SubReport extends DocElement {

    private SubReportDataSourceType subReportDataSourceType = EMPTY;
    private String subReportDataSourcePath = "";

    private boolean pageBreakBefore;
    public String fileName;

    public SubReportDataSourceType getSubReportDataSourceType() {
        return subReportDataSourceType;
    }

    public void setSubReportDataSourceType(SubReportDataSourceType subReportDataSourceType) {
        this.subReportDataSourceType = subReportDataSourceType;
    }

    public String getSubReportDataSourcePath() {
        return subReportDataSourcePath;
    }

    public void setSubReportDataSourcePath(String subReportDataSourcePath) {
        this.subReportDataSourcePath = subReportDataSourcePath;
    }

    public boolean isPageBreakBefore() {
        return pageBreakBefore;
    }

    public void setPageBreakBefore(boolean pageBreakBefore) {
        this.pageBreakBefore = pageBreakBefore;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void visit(Visitor visitor) {
        visitor.accept(this);
    }
}
