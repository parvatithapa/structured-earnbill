package com.sapienter.jbilling.server.invoiceTemplate.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author elmot
 */
public class Section extends DocElement {

    private boolean pageBreakBefore;

    public Band header = new Band();
    public Band footer = new Band();

    public CommonLines content;
    public DocElement subReport;
    public int columns = 1;

    public boolean isPageBreakBefore() {
        return pageBreakBefore;
    }

    public Band getHeader() {
        return header;
    }

    public void setPageBreakBefore(boolean pageBreakBefore) {
        this.pageBreakBefore = pageBreakBefore;
    }

    public Band getFooter() {
        return footer;
    }

    public CommonLines getContent() {
        return content;
    }

    public void setContent(CommonLines content) {
        this.content = content;
    }

    public DocElement getSubReport() {
        return subReport;
    }

    public void setSubReport(SubReport subReport) {
        this.subReport = subReport;
    }

    public int getColumns() {
        return columns;
    }

    public void visit(Visitor visitor) {
        visitor.accept(this);
    }

    public DocElement detailElement() {
        return content == null ? subReport : content;
    }

}
