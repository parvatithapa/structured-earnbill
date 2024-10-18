package com.sapienter.jbilling.server.customerInspector.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "static")
@XmlAccessorType(XmlAccessType.NONE)
public class StaticField extends AbstractField {

    @XmlAttribute(required = true)
    private String value;

    @XmlAttribute
    private Boolean header;

    @XmlAttribute(required = true)
    protected String label;

    @Override
    public Object getValue(Integer userId) {
        return value;
    }

    public Boolean getHeader() {
        return header;
    }

    public String getLabel() {
        return label;
    }
}