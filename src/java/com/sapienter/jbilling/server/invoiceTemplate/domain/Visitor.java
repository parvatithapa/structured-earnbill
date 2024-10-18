package com.sapienter.jbilling.server.invoiceTemplate.domain;

/**
 * @author elmot
 */
public interface Visitor {

    void accept(Text text);

    void accept(TextBox box);

    void accept(InvoiceLines invoiceLines);

    void accept(EventLines eventLines);

    void accept(Image image);

    void accept(Section section);

    void accept(SubReport subReport);

    void accept(List list);

    void accept(DocDesign docDesign);
}
