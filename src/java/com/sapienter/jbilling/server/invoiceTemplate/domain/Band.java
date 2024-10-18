package com.sapienter.jbilling.server.invoiceTemplate.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author elmot
 */
public class Band {
    public List<DocElement> elements = new ArrayList<DocElement>();

    public List<DocElement> getElements() {
        return elements;
    }
}
