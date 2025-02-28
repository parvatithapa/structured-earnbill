//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.07.09 at 02:46:25 PM IST 
//


package com.sapienter.jbilling.server.mediation.sapphire.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * Specifies the details of an application server that was
 *         a party to the call.
 * 
 *         This PartyType gets used in two distinct scenarios. If a call
 *         terminates on an application server, for example a voicemail server,
 *         the terminating party will be the appropriate AppServerPartyType.
 * 
 *         Secondly, this type is used when an application server originates a
 *         call on behalf of the served user. For example, if A calls B and a
 *         terminating application server originates a call to C there will be CDR
 *         with individual line parties for the A-B call and a separate CDR with
 *         an originating AppServerPartytype containing a ServedParty of B and a
 *         terminating party of a type appropriate to C's line. If the call does
 *         not originate the call on behalf of another user, the ServedParty
 *         represents the application server itself and will also be of type
 *         AppServerPartyType.
 * 
 *         This is a concrete derivation of the abstract PartyType. Parsers may
 *         use the value of the attribute xsi:type, which will be set to
 *         "AppServerPartyType" to determine what kind of party is being
 *         described.
 *       
 * 
 * <p>Java class for AppServerPartyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AppServerPartyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.metaswitch.com/cfs/billing/V1.0}PartyType">
 *       &lt;sequence>
 *         &lt;element name="SubscriberAddr" type="{http://www.metaswitch.com/cfs/billing/V1.0}AddressType" minOccurs="0"/>
 *         &lt;element name="CallingPartyAddr" type="{http://www.metaswitch.com/cfs/billing/V1.0}AddressType" minOccurs="0"/>
 *         &lt;element name="ChargeAddr" type="{http://www.metaswitch.com/cfs/billing/V1.0}AddressType" minOccurs="0"/>
 *         &lt;element name="AppServerAddr" type="{http://www.metaswitch.com/cfs/billing/V1.0}AddressType" minOccurs="0"/>
 *         &lt;element name="ServedParty" type="{http://www.metaswitch.com/cfs/billing/V1.0}PartyType" minOccurs="0"/>
 *         &lt;element name="SIPCallId" type="{http://www.metaswitch.com/cfs/billing/V1.0}SIPCallIdType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="privacy" use="required" type="{http://www.metaswitch.com/cfs/billing/V1.0}PrivacyType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AppServerPartyType")
public class AppServerPartyType
    extends PartyType
{


}
