//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.07.09 at 02:46:25 PM IST 
//


package com.sapienter.jbilling.server.mediation.sapphire.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AddressFamilyType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AddressFamilyType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="e164"/>
 *     &lt;enumeration value="national"/>
 *     &lt;enumeration value="subscriber"/>
 *     &lt;enumeration value="sip_uri"/>
 *     &lt;enumeration value="tel_uri"/>
 *     &lt;enumeration value="unknown"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "AddressFamilyType")
@XmlEnum
public enum AddressFamilyType {


    /**
     * An E.164 number (e.g. +442476992023)
     * 
     */
    @XmlEnumValue("e164")
    E_164("e164"),

    /**
     * A national number (e.g. 2476992023)
     * 
     */
    @XmlEnumValue("national")
    NATIONAL("national"),

    /**
     * A subscriber number (e.g. 2476992023)
     *           Introduced: Version 1.0, Compatibility Level 5.
     *           
     * 
     */
    @XmlEnumValue("subscriber")
    SUBSCRIBER("subscriber"),

    /**
     * A SIP URI (e.g. sip:alice@example.com)
     * 
     */
    @XmlEnumValue("sip_uri")
    SIP_URI("sip_uri"),

    /**
     * A TEL URI (e.g. tel:+442476992023)
     * 
     */
    @XmlEnumValue("tel_uri")
    TEL_URI("tel_uri"),
    @XmlEnumValue("unknown")
    UNKNOWN("unknown");
    private final String value;

    AddressFamilyType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AddressFamilyType fromValue(String v) {
        for (AddressFamilyType c: AddressFamilyType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
