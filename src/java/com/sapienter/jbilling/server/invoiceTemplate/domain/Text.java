package com.sapienter.jbilling.server.invoiceTemplate.domain;

import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;

import static net.sf.jasperreports.engine.type.EvaluationTimeEnum.AUTO;
import static net.sf.jasperreports.engine.type.HorizontalAlignEnum.LEFT;

/**
 * @author Klim
 */
public class Text extends DocElement {

    private int thickness;

    private Font font;

    private int padding;

    private String expr;

    private String borderColor = "#808080";

    private String bgColor = "#A0A0A0";

    private HorizontalAlignEnum alignment;

    private boolean transparent;

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public void visit(Visitor visitor) {
        visitor.accept(this);
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public HorizontalAlignEnum getAlignment() {
        return alignment == null ? LEFT : alignment;
    }

    public void setAlignment(HorizontalAlignEnum alignment) {
        this.alignment = alignment;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }
}
