package com.sapienter.jbilling.server.invoiceTemplate.domain;

import com.sapienter.jbilling.server.invoiceTemplate.report.FieldSetup;
import com.sapienter.jbilling.server.invoiceTemplate.report.FieldType;
import net.sf.jasperreports.engine.component.ComponentKey;
import net.sf.jasperreports.engine.type.PrintOrderEnum;

import java.util.ArrayList;
import java.util.Collections;

import static com.sapienter.jbilling.server.invoiceTemplate.domain.List.Orientation.Horizontal;
import static com.sapienter.jbilling.server.invoiceTemplate.domain.List.Source.InvoiceLines;
import static com.sapienter.jbilling.server.invoiceTemplate.report.FieldType.Field;
import static net.sf.jasperreports.engine.type.PrintOrderEnum.HORIZONTAL;
import static net.sf.jasperreports.engine.type.PrintOrderEnum.VERTICAL;

/**
 * Created by Klim on 09.02.14.
 */
public class List extends CommonLines {

    public static final ComponentKey COMPONENT_KEY = new ComponentKey("http://jasperreports.sourceforge.net/jasperreports/components", "jr", "list");

    private java.util.List<DocElement> elements = new ArrayList<DocElement>();
    private boolean ignoreWidth = false;
    private Source source = InvoiceLines;
    private Orientation orientation = Horizontal;
    private String sortBy = null;
    private String filterExpr = null;

    private int padding = 0;
    private int thickness = 0;
    private String borderColor = "#808080";

    private boolean transparent;
    private String bgColor = "#A0A0A0";

    private Text noDataText;

    public java.util.List<DocElement> getElements() {
        return elements;
    }

    public void setElements(java.util.List<DocElement> elements) {
        this.elements = elements;
    }

    public boolean isIgnoreWidth() {
        return ignoreWidth;
    }

    public void setIgnoreWidth(boolean ignoreWidth) {
        this.ignoreWidth = ignoreWidth;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getFilterExpr() {
        return filterExpr;
    }

    public void setFilterExpr(String filterExpr) {
        this.filterExpr = filterExpr;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public Text getNoDataText() {
        return noDataText;
    }

    public void setNoDataText(Text noDataText) {
        this.noDataText = noDataText;
    }

    @Override
    public int getWidth() {
        int width = super.getWidth();
        return width > 0 ? width : 1;
    }

    @Override
    public FieldSetup getSortCriterion() {
        FieldSetup[] sortCriteria = getSortCriteria();
        if (sortCriteria.length == 0) {
            return null;
        }
        return sortCriteria[0];
    }

    @Override
    public FieldSetup[] getSortCriteria() {
        if (sortBy == null || sortBy.isEmpty()) {
            return new FieldSetup[0];
        }
        String[] sortByFields = sortBy.split(",");
        FieldSetup[] result = new FieldSetup[sortByFields.length];
        for (int i = 0; i < sortByFields.length; i++) {
            String sortByField = sortByFields[i];
            result[i] = new FieldSetup(sortByField, sortByField, Field, String.class);
        }
        return result;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.accept(this);
    }

    public enum Source {InvoiceLines, CDREvents, Assets}

    public enum Orientation {
        Horizontal(HORIZONTAL),
        Vertical(VERTICAL);

        private final PrintOrderEnum printOrder;

        Orientation(PrintOrderEnum printOrder) {
            this.printOrder = printOrder;
        }

        public PrintOrderEnum getPrintOrder() {
            return printOrder;
        }
    }
}
