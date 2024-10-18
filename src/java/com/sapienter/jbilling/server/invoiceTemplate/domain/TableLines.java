package com.sapienter.jbilling.server.invoiceTemplate.domain;

import com.sapienter.jbilling.server.invoiceTemplate.report.ColumnSettings;
import com.sapienter.jbilling.server.invoiceTemplate.report.FieldSetup;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Klim
 */
public abstract class TableLines extends CommonLines {

    private Font font;

    private Font headerFont;
    private String headerBgColor;
    private String contentBgColor;

    private FieldSetup groupCriteria;
    private FieldSetup addGroupCriteria;

    private Font groupHeaderFont;
    private String groupBgColor;

    private Font addGroupHeaderFont;
    private String addGroupBgColor;

    private SeparationType recordSeparation;
    private int recordSeparationThickness;
    private String recordSeparationColor;

    private CellStyle headerCellStyle;
    private CellStyle contentCellStyle;

    private ColumnSettings[] columns;

    public ColumnSettings findColumn(FieldSetup field) {
        for (ColumnSettings cs : columns) {
            if (cs.field.equals(field)) {
                return cs;
            }
        }
        return null;
    }

    public ColumnSettings[] getColumns() {
        return columns;
    }

    public void setColumns(ColumnSettings[] columns) {
        this.columns = columns;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Font getHeaderFont() {
        return headerFont;
    }

    public void setHeaderFont(Font headerFont) {
        this.headerFont = headerFont;
    }

    public String getHeaderBgColor() {
        return headerBgColor;
    }

    public void setHeaderBgColor(String headerBgColor) {
        this.headerBgColor = headerBgColor;
    }

    public SeparationType getRecordSeparation() {
        return recordSeparation;
    }

    public void setRecordSeparation(SeparationType recordSeparation) {
        this.recordSeparation = recordSeparation;
    }

    public int getRecordSeparationThickness() {
        return recordSeparationThickness;
    }

    public void setRecordSeparationThickness(int recordSeparationThickness) {
        this.recordSeparationThickness = recordSeparationThickness;
    }

    public String getRecordSeparationColor() {
        return recordSeparationColor;
    }

    public void setRecordSeparationColor(String recordSeparationColor) {
        this.recordSeparationColor = recordSeparationColor;
    }

    public FieldSetup getGroupCriteria() {
        return groupCriteria;
    }

    public void setGroupCriteria(FieldSetup groupCriteria) {
        this.groupCriteria = groupCriteria;
    }

    public FieldSetup getAddGroupCriteria() {
        return addGroupCriteria;
    }

    public void setAddGroupCriteria(FieldSetup addGroupCriteria) {
        this.addGroupCriteria = addGroupCriteria;
    }

    public Font getGroupHeaderFont() {
        return groupHeaderFont;
    }

    public void setGroupHeaderFont(Font groupHeaderFont) {
        this.groupHeaderFont = groupHeaderFont;
    }

    public String getGroupBgColor() {
        return groupBgColor;
    }

    public void setGroupBgColor(String groupBgColor) {
        this.groupBgColor = groupBgColor;
    }

    public Font getAddGroupHeaderFont() {
        return addGroupHeaderFont == null ? groupHeaderFont : addGroupHeaderFont;
    }

    public void setAddGroupHeaderFont(Font addGroupHeaderFont) {
        this.addGroupHeaderFont = addGroupHeaderFont;
    }

    public String getAddGroupBgColor() {
        return addGroupBgColor == null ? groupBgColor : addGroupBgColor;
    }

    public void setAddGroupBgColor(String addGroupBgColor) {
        this.addGroupBgColor = addGroupBgColor;
    }

    public List<FieldSetup> getGroupCriteriaList() {
        return asList(groupCriteria, addGroupCriteria);
    }

    public String getContentBgColor() {
        return contentBgColor;
    }

    public void setContentBgColor(String contentBgColor) {
        this.contentBgColor = contentBgColor;
    }

    public CellStyle getHeaderCellStyle() {
        return headerCellStyle;
    }

    public void setHeaderCellStyle(CellStyle headerCellStyle) {
        this.headerCellStyle = headerCellStyle;
    }

    public CellStyle getContentCellStyle() {
        return contentCellStyle;
    }

    public void setContentCellStyle(CellStyle contentCellStyle) {
        this.contentCellStyle = contentCellStyle;
    }

    // Tables fill the whole space, so we can ignore their positions

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getLeft() {
        return 0;
    }

    @Override
    public int getTop() {
        return 0;
    }
}
