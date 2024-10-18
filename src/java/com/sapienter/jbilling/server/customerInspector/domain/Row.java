package com.sapienter.jbilling.server.customerInspector.domain;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Row {

    @XmlAttribute
    private String style;

    @XmlElement(name = "column", required = true)
    private List<Column> columns;


    public String getStyle() {
        return style;
    }

    public List<Column> getColumns() {
        return columns;
    }
}