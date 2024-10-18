package com.sapienter.jbilling.server.invoiceTemplate.domain;

/**
 * Created by andres on 26/11/18.
 */
public class CellStyle {

    private int horizontalBorderThickness;
    private int verticalBorderThickness;
    private String horizontalBorderColor;
    private String verticalBorderColor;

    public int getHorizontalBorderThickness() {
        return horizontalBorderThickness;
    }

    public void setHorizontalBorderThickness(int horizontalBorderThickness) {
        this.horizontalBorderThickness = horizontalBorderThickness;
    }

    public int getVerticalBorderThickness() {
        return verticalBorderThickness;
    }

    public void setVerticalBorderThickness(int verticalBorderThickness) {
        this.verticalBorderThickness = verticalBorderThickness;
    }

    public String getHorizontalBorderColor() {
        return horizontalBorderColor;
    }

    public void setHorizontalBorderColor(String horizontalBorderColor) {
        this.horizontalBorderColor = horizontalBorderColor;
    }

    public String getVerticalBorderColor() {
        return verticalBorderColor;
    }

    public void setVerticalBorderColor(String verticalBorderColor) {
        this.verticalBorderColor = verticalBorderColor;
    }
}
