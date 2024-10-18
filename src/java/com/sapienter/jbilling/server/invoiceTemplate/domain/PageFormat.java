package com.sapienter.jbilling.server.invoiceTemplate.domain;

/**
 * @author elmot
 */
public enum PageFormat {
    A4(595, 841),
    A5(421, 595),
    Letter(612, 792),
    Legal(612, 1008);
    final public int width, height;

    /**
     * size in points with 72dpi
     */
    private PageFormat(int width, int height) {
        this.width = width;
        this.height = height;
    }


}
