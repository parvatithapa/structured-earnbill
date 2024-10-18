package com.sapienter.jbilling.server.customerInspector.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Column {

    @XmlAttribute
    private String style;

    @XmlElements({
            @XmlElement(name = "basic", type = BasicField.class),
            @XmlElement(name = "metaField", type = MetaFieldField.class),
            @XmlElement(name = "metaFieldType", type = MetaFieldTypeField.class),
            @XmlElement(name = "static", type = StaticField.class),
            @XmlElement(name = "list", type = ListField.class),
            @XmlElement(name = "specific", type = SpecificField.class)
    })
    private AbstractField field;


    public String getStyle() {
        return style;
    }

    public AbstractField getField() {
        return field;
    }
}