package com.sapienter.jbilling.server.invoiceTemplate.domain;

import net.sf.jasperreports.engine.type.OrientationEnum;

import java.util.ArrayList;
import java.util.List;

import static net.sf.jasperreports.engine.type.OrientationEnum.LANDSCAPE;
import static net.sf.jasperreports.engine.type.OrientationEnum.PORTRAIT;

/**
 * @author elmot
 */
public class DocDesign {
    private List<Section> sections = new ArrayList<Section>();

    private String name;
    private PageFormat format = PageFormat.A4;
    private int marginLeft;
    private int marginRight;
    private int marginTop;
    private int marginBottom;
    public Band pageHeader = new Band();
    public Band pageFooter = new Band();
    private boolean landscape;
    private List<SqlField> sqlFields = new ArrayList<SqlField>();

    public List<Section> getSections() {
        return sections;
    }

    public void visit(Visitor visitor) {
        visitor.accept(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public void setMarginLeft(int marginLeft) {
        this.marginLeft = marginLeft;
    }

    public int getMarginRight() {
        return marginRight;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
    }

    public int getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(int marginTop) {
        this.marginTop = marginTop;
    }

    public int getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
    }

    public int columnWidth() {
        return pageWidth() - (marginLeft + marginRight);
    }

    public Band getPageHeader() {
        return pageHeader;
    }

    public Band getPageFooter() {
        return pageFooter;
    }

    public PageFormat getFormat() {
        return format;
    }

    public void setFormat(PageFormat format) {
        this.format = format;
    }

    public boolean isLandscape() {
        return landscape;
    }

    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    public int pageHeight() {
        return landscape ? format.width : format.height;
    }

    public int pageWidth() {
        return landscape ? format.height : format.width;
    }

    public OrientationEnum orientation() {
        return landscape ? LANDSCAPE : PORTRAIT;
    }

    public List<SqlField> getSqlFields() {
        return sqlFields;
    }

    public void setSqlFields(List<SqlField> sqlFields) {
        this.sqlFields = sqlFields;
    }
}
