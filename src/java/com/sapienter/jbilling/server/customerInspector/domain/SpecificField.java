package com.sapienter.jbilling.server.customerInspector.domain;

import com.sapienter.jbilling.server.customerInspector.SpecificCustomerInformationHelper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by pablo123 on 01/03/2017.
 */
@XmlRootElement(name = "specific")
@XmlAccessorType(XmlAccessType.NONE)
public class SpecificField extends AbstractField {

    @XmlAttribute(required = true)
    private SpecificType type;

    @XmlAttribute(required = true)
    protected String label;

    @XmlAttribute(required = true)
    protected String name;

    @XmlAttribute(required = true)
    protected String target;

    public SpecificType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Object getValue(Integer userId) {
        SpecificCustomerInformationHelper specificCIHelper = new SpecificCustomerInformationHelper(userId, name);
        return specificCIHelper.getValue();
    }
    public enum SpecificType {
        TEXT,
        BUTTON,
        LINK
    }
}
