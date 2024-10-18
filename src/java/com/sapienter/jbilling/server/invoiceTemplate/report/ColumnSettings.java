package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.invoiceTemplate.domain.CellStyle;
import com.sapienter.jbilling.server.invoiceTemplate.domain.Font;
import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.JRPen;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;

import java.awt.Color;

import static com.sapienter.jbilling.server.invoiceTemplate.report.ConvertUtils.convertColor;

/**
 * Created by Klim on 01.12.13.
 */
public class ColumnSettings {

    public boolean show;
    public String title;
    public FieldSetup field;
    public int width;
    public String formatter;
    public HorizontalAlignEnum alignment;

    public static void setupLines(TableLines invoiceLines, JRDesignBand columnHeader, JRDesignBand details, int availableTableWidth, String locale) {
        int baseX = 0;
        int headerHeight = invoiceLines.getHeaderFont().getSize() * 2;
        columnHeader.setHeight(headerHeight);

        Font font = invoiceLines.getFont();

        int cellHeight = font.getSize() * 5 / 2;
        details.setHeight(cellHeight);

        ColumnSettings[] columnSettings = invoiceLines.getColumns();
        if (columnSettings == null) {
            columnSettings = new ColumnSettings[0];
        }

        double widthFactor;
        int tableWidth = 0;
        for (ColumnSettings cs : columnSettings) {
            tableWidth += cs.show ? cs.width : 0;
        }
        widthFactor = tableWidth <= availableTableWidth ? 1.0 : ((double) availableTableWidth) / ((double) tableWidth);

        for (ColumnSettings cs : columnSettings) {
            if (cs.show) {
                JRDesignTextField textField = cs.makeTextField(invoiceLines, font, baseX, cellHeight, widthFactor,  locale);
                addElementToBand(details, textField);
                JRDesignTextElement headerField = cs.makeHeader(invoiceLines, baseX, headerHeight, widthFactor);
                addElementToBand(columnHeader, headerField);
                baseX += textField.getWidth();
            }
        }
        if (invoiceLines.getRecordSeparation() != null) {
            setupRowSeparator(invoiceLines, details, baseX, details.getHeight());
        }
    }

    public JRDesignExpression createExpression(TableLines cl , String locale) {
        return field.createExpression(cl, formatter, locale);
    }

    private static void addElementToBand(JRDesignBand jrBand, JRDesignElement jrElement) {
        jrBand.addElement(jrElement);
        jrBand.setHeight(Math.max(jrBand.getHeight(), jrElement.getY() + jrElement.getHeight()));
    }

    private static void setupRowSeparator(TableLines invoiceLines, JRDesignBand details, int baseX, int cellHeight) {
        JRPen linePen;
        JRDesignFrame separator = new JRDesignFrame();
        JRLineBox box = separator.getLineBox();
        switch (invoiceLines.getRecordSeparation()) {
            case Box:
                separator.setX(invoiceLines.getRecordSeparationThickness() / 2);
                separator.setY(invoiceLines.getRecordSeparationThickness() / 2);
                separator.setWidth(baseX - invoiceLines.getRecordSeparationThickness());
                separator.setHeight(cellHeight - invoiceLines.getRecordSeparationThickness());
                linePen = box.getPen();
                linePen.setLineColor(ConvertUtils.convertColor(invoiceLines.getRecordSeparationColor()));
                linePen.setLineWidth(invoiceLines.getRecordSeparationThickness());
                break;
            case Line:
                separator.setX(0);
                separator.setY(0);
                separator.setWidth(baseX);
                separator.setHeight(cellHeight);
                linePen = box.getBottomPen();
                linePen.setLineColor(ConvertUtils.convertColor(invoiceLines.getRecordSeparationColor()));
                linePen.setLineWidth(invoiceLines.getRecordSeparationThickness());
                box.getTopPen().setLineWidth(0);
                box.getLeftPen().setLineWidth(0);
                box.getRightPen().setLineWidth(0);
                break;
            default:
                return;
        }
        separator.setStretchType(StretchTypeEnum.ELEMENT_GROUP_HEIGHT);
        addElementToBand(details, separator);
    }

    private JRDesignTextField makeTextField(TableLines commonLines, Font font, int shiftX, int cellHeight, double widthFactor, String locale) {
        JRDesignTextField textField = new JRDesignTextField();
        ConvertUtils.setupFont(font, textField);
        textField.setExpression(createExpression(commonLines, locale));
        textField.setWidth(widthFactor == 1.0 ? width : (int) Math.round(widthFactor * (double) width));
        textField.setX(shiftX);
        textField.setY(0);
        textField.setHeight(cellHeight);
        textField.setBlankWhenNull(true);
        textField.setMode(ModeEnum.OPAQUE);
        setCellStyle(commonLines.getContentCellStyle(), textField.getLineBox(), false);
        textField.setBackcolor(convertColor(commonLines.getContentBgColor(), Color.WHITE));
        textField.getLineBox().setPadding(cellHeight / 5);
        textField.setStretchType(StretchTypeEnum.ELEMENT_GROUP_HEIGHT);
        textField.setPrintRepeatedValues(true);
        textField.setPrintWhenDetailOverflows(true);
        textField.setStretchWithOverflow(true);
        textField.setHorizontalAlignment(alignment == null ? HorizontalAlignEnum.LEFT : alignment);
        return textField;
    }

    private JRDesignTextElement makeHeader(TableLines commonLines, int baseX, int height, double widthFactor) {
        JRDesignTextElement header = ReportBuildVisitor.createTextElement(title);
        header.setMode(ModeEnum.OPAQUE);
        ConvertUtils.setupFont(commonLines.getHeaderFont(), header);
        header.setBackcolor(ConvertUtils.convertColor(commonLines.getHeaderBgColor()));
        header.setY(0);
        header.setX(baseX);
        header.setHeight(height);
        header.getLineBox().setPadding(height / 10);
        header.setWidth(widthFactor == 1.0 ? width : (int) Math.round(widthFactor * (double) width));
        header.setStretchType(StretchTypeEnum.ELEMENT_GROUP_HEIGHT);
        setCellStyle(commonLines.getHeaderCellStyle(), header.getLineBox(), true);
        return header;
    }

    private void setCellStyle(CellStyle cellStyle, JRLineBox lineBox, boolean header) {
        if(cellStyle == null) {
            cellStyle = new CellStyle();
        }

        int horizontalThickness = cellStyle.getHorizontalBorderThickness();
        if(horizontalThickness > 0) {
            if(header) {
                lineBox.getBottomPen().setLineWidth(horizontalThickness + 1);
            } else {
                lineBox.getBottomPen().setLineWidth(horizontalThickness);
            }
            lineBox.getTopPen().setLineWidth(horizontalThickness);
            Color xColor = convertColor(cellStyle.getHorizontalBorderColor(), Color.WHITE);
            lineBox.getBottomPen().setLineColor(xColor);
            lineBox.getTopPen().setLineColor(xColor);
        }

        int verticalThickness = cellStyle.getVerticalBorderThickness();
        if(verticalThickness > 0){
            lineBox.getLeftPen().setLineWidth(verticalThickness);
            lineBox.getRightPen().setLineWidth(verticalThickness);
            Color yColor = convertColor(cellStyle.getVerticalBorderColor(), Color.WHITE);
            lineBox.getLeftPen().setLineColor(yColor);
            lineBox.getRightPen().setLineColor(yColor);
        }
    }


}
