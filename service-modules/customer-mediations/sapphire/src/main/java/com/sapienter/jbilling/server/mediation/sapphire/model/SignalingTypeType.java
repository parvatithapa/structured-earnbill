//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.07.09 at 02:46:25 PM IST 
//


package com.sapienter.jbilling.server.mediation.sapphire.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Represents the signaling type used by one party on a call.
 * 
 *         Introduced: Version 1.0, Compatibility Level 8.
 *         
 * 
 * <p>Java class for SignalingTypeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SignalingTypeType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.metaswitch.com/cfs/billing/V1.0>SignalingTypeBasicType">
 *       &lt;attribute name="variant" type="{http://www.metaswitch.com/cfs/billing/V1.0}SignalingVariantType" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignalingTypeType", propOrder = {
    "value"
})
public class SignalingTypeType {

    @XmlValue
    protected SignalingTypeBasicType value;
    @XmlAttribute(name = "variant")
    protected String variant;

    /**
     * This indicates the type of signaling used.
     * 
     *         Introduced: Version 1.0, Compatibility Level 8.
     *       
     * 
     * @return
     *     possible object is
     *     {@link SignalingTypeBasicType }
     *     
     */
    public SignalingTypeBasicType getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link SignalingTypeBasicType }
     *     
     */
    public void setValue(SignalingTypeBasicType value) {
        this.value = value;
    }

    /**
     * Gets the value of the variant property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVariant() {
        return variant;
    }

    /**
     * Sets the value of the variant property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVariant(String value) {
        this.variant = value;
    }

}
