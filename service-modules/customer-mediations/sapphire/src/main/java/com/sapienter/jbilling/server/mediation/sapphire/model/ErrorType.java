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
 * <p>Java class for ErrorType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ErrorType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="no"/>
 *     &lt;enumeration value="timing"/>
 *     &lt;enumeration value="short"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ErrorType")
@XmlEnum
public enum ErrorType {


    /**
     * There are no known issues with the data in the CDR.
     *           
     * 
     */
    @XmlEnumValue("no")
    NO("no"),

    /**
     * The timing of events in the CDR is suspect. This may
     *             arise under certain misconfigurations (e.g. NTP not being used.)
     *           
     * 
     */
    @XmlEnumValue("timing")
    TIMING("timing"),

    /**
     * The call duration was less than 500ms so probably
     *             represents the called party answering just as the calling
     *             party cleared the call. It may also arise in some misconfigurations
     *             when the network clears the call immediately after answer.
     *             
     * 
     */
    @XmlEnumValue("short")
    SHORT("short");
    private final String value;

    ErrorType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ErrorType fromValue(String v) {
        for (ErrorType c: ErrorType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
