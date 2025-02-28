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
 * <p>Java class for RedirectReasonType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="RedirectReasonType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Unknown"/>
 *     &lt;enumeration value="UserBusy"/>
 *     &lt;enumeration value="NoReply"/>
 *     &lt;enumeration value="Unconditional"/>
 *     &lt;enumeration value="Deflection"/>
 *     &lt;enumeration value="FollowMe"/>
 *     &lt;enumeration value="FaxTransfer"/>
 *     &lt;enumeration value="MobileTransfer"/>
 *     &lt;enumeration value="NotReachable"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "RedirectReasonType")
@XmlEnum
public enum RedirectReasonType {


    /**
     * The reason why the call was diverted is not known.
     *           
     * 
     */
    @XmlEnumValue("Unknown")
    UNKNOWN("Unknown"),

    /**
     * The call was diverted because the called party was
     *             busy.
     *           
     * 
     */
    @XmlEnumValue("UserBusy")
    USER_BUSY("UserBusy"),

    /**
     * The call was diverted because the called party was
     *             unable to answer the call.
     *           
     * 
     */
    @XmlEnumValue("NoReply")
    NO_REPLY("NoReply"),

    /**
     * The call was diverted because the called party had
     *             requested that all calls should be diverted.
     *           
     * 
     */
    @XmlEnumValue("Unconditional")
    UNCONDITIONAL("Unconditional"),

    /**
     * The call was diverted because the called party
     *             rejected the call after it was presented.
     *           
     * 
     */
    @XmlEnumValue("Deflection")
    DEFLECTION("Deflection"),

    /**
     * The call was diverted because the called party has
     *             A FollowMe service active.
     *           
     * 
     */
    @XmlEnumValue("FollowMe")
    FOLLOW_ME("FollowMe"),

    /**
     * The call was diverted because the called party has
     *             requested that fax calls are diverted.
     *           
     * 
     */
    @XmlEnumValue("FaxTransfer")
    FAX_TRANSFER("FaxTransfer"),

    /**
     * The call was diverted because the called party has
     *             requested that mobile calls are diverted.
     *           
     * 
     */
    @XmlEnumValue("MobileTransfer")
    MOBILE_TRANSFER("MobileTransfer"),

    /**
     * The call was diverted because the called party was
     *             not reachable.
     * 
     *             Introduced: Version 1.0, Compatibility Level 7.
     *           
     * 
     */
    @XmlEnumValue("NotReachable")
    NOT_REACHABLE("NotReachable");
    private final String value;

    RedirectReasonType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RedirectReasonType fromValue(String v) {
        for (RedirectReasonType c: RedirectReasonType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
