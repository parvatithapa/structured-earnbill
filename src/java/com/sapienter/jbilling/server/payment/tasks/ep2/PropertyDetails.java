package com.sapienter.jbilling.server.payment.tasks.ep2;

import java.util.List;


public class PropertyDetails {
    private String propertyName;
    private String propertyValue;
    private String attributeName;
    private String attributeValue;
    private List<PropertyDetails> childProperties;

    public PropertyDetails(String propertyName, String propertyValue, String attributeName, String attributeValue, List<PropertyDetails> childProperties){
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.childProperties = childProperties;
    }

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getAttributeValue() {
		return attributeValue;
	}

	public List<PropertyDetails> getChildProperties() {
		return childProperties;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}

	public void setChildProperties(List<PropertyDetails> childProperties) {
		this.childProperties = childProperties;
	}
    
}
