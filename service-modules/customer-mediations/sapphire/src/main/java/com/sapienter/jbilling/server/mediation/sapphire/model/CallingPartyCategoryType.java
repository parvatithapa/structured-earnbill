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
 * <p>Java class for CallingPartyCategoryType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CallingPartyCategoryType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="unknown"/>
 *     &lt;enumeration value="normal"/>
 *     &lt;enumeration value="priority"/>
 *     &lt;enumeration value="operator"/>
 *     &lt;enumeration value="payphone"/>
 *     &lt;enumeration value="test call"/>
 *     &lt;enumeration value="emergency"/>
 *     &lt;enumeration value="priority emergency"/>
 *     &lt;enumeration value="ns/ep call"/>
 *     &lt;enumeration value="oss operator"/>
 *     &lt;enumeration value="admin diversion - normal"/>
 *     &lt;enumeration value="admin diversion - priority"/>
 *     &lt;enumeration value="admin diversion - payphone"/>
 *     &lt;enumeration value="admin diversion - payphone priority"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CallingPartyCategoryType")
@XmlEnum
public enum CallingPartyCategoryType {


    /**
     * The category is Unknown as defined in ITU-T Q.763.
     *             See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("unknown")
    UNKNOWN("unknown"),

    /**
     * The category is Ordinary Calling Subscriber as defined
     *             in ITU-T Q.763.
     *             See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("normal")
    NORMAL("normal"),

    /**
     * The category is Calling Subscriber with Priority as
     *             defined in ITU-T Q.763.
     *             See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("priority")
    PRIORITY("priority"),

    /**
     * The category is Operator Call as defined in ITU-T
     *             Q.763. See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("operator")
    OPERATOR("operator"),

    /**
     * The category is Payphone as defined in ITU-T Q.763.
     *             See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("payphone")
    PAYPHONE("payphone"),

    /**
     * The category is Test Call as defined in ITU-T Q.763.
     *             See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("test call")
    TEST_CALL("test call"),

    /**
     * The category is Emergency service call as defined in
     *             ANSI T1.113.
     *           
     * 
     */
    @XmlEnumValue("emergency")
    EMERGENCY("emergency"),

    /**
     * The category is Emergency (High Priority) service call
     *             as defined in ANSI T1.113
     *           
     * 
     */
    @XmlEnumValue("priority emergency")
    PRIORITY_EMERGENCY("priority emergency"),

    /**
     * The category is National Security and Emergency
     *             Preparedness (NS/EP) service call as defined in ANSI T1.113 and
     *             GR-2931. This category is used for priority users during GETS
     *             working in North America.
     *           
     * 
     */
    @XmlEnumValue("ns/ep call")
    NS_EP_CALL("ns/ep call"),

    /**
     * The category is OSS Operator as defined in ND1007.
     *             See http://www.niccstandards.org.uk/files/current/nd1007_2007_01.pdf?type=pdf
     *           
     * 
     */
    @XmlEnumValue("oss operator")
    OSS_OPERATOR("oss operator"),

    /**
     * The category is Admin Diversion - Ordinary as defined
     *             in ND1007.
     *             See http://www.niccstandards.org.uk/files/current/nd1007_2007_01.pdf?type=pdf
     *           
     * 
     */
    @XmlEnumValue("admin diversion - normal")
    ADMIN_DIVERSION_NORMAL("admin diversion - normal"),

    /**
     * The category is Admin Diversion - Ordinary with
     *             Priority as defined in ND1007.
     *             See http://www.niccstandards.org.uk/files/current/nd1007_2007_01.pdf?type=pdf
     *           
     * 
     */
    @XmlEnumValue("admin diversion - priority")
    ADMIN_DIVERSION_PRIORITY("admin diversion - priority"),

    /**
     * The category is Admin Diversion - Payphone as defined
     *             in ND1007.
     *             See http://www.niccstandards.org.uk/files/current/nd1007_2007_01.pdf?type=pdf
     *           
     * 
     */
    @XmlEnumValue("admin diversion - payphone")
    ADMIN_DIVERSION_PAYPHONE("admin diversion - payphone"),

    /**
     * The category is Admin Diversion - Payphone with
     *             Priority as defined in ND1007.
     *             See http://www.niccstandards.org.uk/files/current/nd1007_2007_01.pdf?type=pdf
     *           
     * 
     */
    @XmlEnumValue("admin diversion - payphone priority")
    ADMIN_DIVERSION_PAYPHONE_PRIORITY("admin diversion - payphone priority"),

    /**
     * The category is unrecognised.
     *             The Metasphere CFS also interprets a Data call as defined in ITU-T
     *             Q.763 as other.
     *             See http://www.itu.int/rec/T-REC-Q.763
     *           
     * 
     */
    @XmlEnumValue("other")
    OTHER("other");
    private final String value;

    CallingPartyCategoryType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CallingPartyCategoryType fromValue(String v) {
        for (CallingPartyCategoryType c: CallingPartyCategoryType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
