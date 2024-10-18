package com.sapienter.jbilling.server.customerInspector.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class CustomerInformation {

    @XmlElement(name = "row", required = true)
    private List<Row> rows;

    public List<Row> getRows() {
        return rows;
    }
}