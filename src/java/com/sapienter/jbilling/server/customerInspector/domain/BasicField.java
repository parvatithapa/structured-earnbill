package com.sapienter.jbilling.server.customerInspector.domain;

import com.sapienter.jbilling.server.customerInspector.CustomerInformationHelper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "basic")
@XmlAccessorType(XmlAccessType.NONE)
public class BasicField extends AbstractField {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private Entity entity;

    @XmlAttribute(required = true)
    protected String label;

    public String getName() {
        return name;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public Object getValue(Integer userId) {
        return new CustomerInformationHelper(userId, name, getApi()).getValue();
    }

    public enum Entity {
        USER,
        CUSTOMER
    }

    public String getLabel() {
        return label;
    }
}