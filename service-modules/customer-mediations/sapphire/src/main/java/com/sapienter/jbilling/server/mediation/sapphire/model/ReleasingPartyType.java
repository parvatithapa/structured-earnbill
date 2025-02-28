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
 * <p>Java class for ReleasingPartyType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ReleasingPartyType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Orig"/>
 *     &lt;enumeration value="Term"/>
 *     &lt;enumeration value="Network"/>
 *     &lt;enumeration value="User"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ReleasingPartyType")
@XmlEnum
public enum ReleasingPartyType {


    /**
     * The originating party was responsible for the call
     *             clearing.
     *           
     * 
     */
    @XmlEnumValue("Orig")
    ORIG("Orig"),

    /**
     * The terminating party was responsible for the call
     *             clearing.
     *           
     * 
     */
    @XmlEnumValue("Term")
    TERM("Term"),

    /**
     * The network was responsible for the call clearing, for
     *             example if the call was unconnected due to a routing failure.
     *           
     * 
     */
    @XmlEnumValue("Network")
    NETWORK("Network"),

    /**
     * A user was responsible for the call clearing, but it
     *             was not possible to tell whether it was the originating or
     *             terminating party.
     *           
     * 
     */
    @XmlEnumValue("User")
    USER("User");
    private final String value;

    ReleasingPartyType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ReleasingPartyType fromValue(String v) {
        for (ReleasingPartyType c: ReleasingPartyType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
