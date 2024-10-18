package com.sapienter.jbilling.server.invoiceTemplate.domain;

import static java.lang.Math.max;

/**
 * @author elmot
 */
public abstract class DocElement {

    private String name;

    private int left;
    private int top;
    private int width;
    private int height;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void visit(Visitor visitor);

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = max(0, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height > 0 ? height : 0;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }
}
