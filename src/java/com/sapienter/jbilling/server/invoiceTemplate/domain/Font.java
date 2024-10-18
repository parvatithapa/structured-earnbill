package com.sapienter.jbilling.server.invoiceTemplate.domain;

/**
 * @author elmot
 */
public class Font {
    private FontFace face = FontFace.DEFAULT;
    private int size = 16;
    private boolean bold = false;
    private boolean italic = false;
    private String color= "#000000";

    public FontFace getFace() {
        return face;
    }

    public void setFace(FontFace face) {
        this.face = face;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
