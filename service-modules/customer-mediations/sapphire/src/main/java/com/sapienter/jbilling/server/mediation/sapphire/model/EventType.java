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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Specifies the details representing an event. An EventCdr
 *         represents details of exactly one event.
 *       
 * 
 * <p>Java class for EventType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EventType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="EventType" type="{http://www.metaswitch.com/cfs/billing/V1.0}EventTypeType"/>
 *         &lt;element name="Feature" type="{http://www.metaswitch.com/cfs/billing/V1.0}FeatureType"/>
 *         &lt;element name="FeatureInfo" type="{http://www.metaswitch.com/cfs/billing/V1.0}FeatureInfoType" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="OrigParty" type="{http://www.metaswitch.com/cfs/billing/V1.0}PartyType"/>
 *           &lt;element name="TermParty" type="{http://www.metaswitch.com/cfs/billing/V1.0}PartyType"/>
 *         &lt;/choice>
 *         &lt;element name="EventTime" type="{http://www.metaswitch.com/cfs/billing/V1.0}TimestampType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="seqnum" use="required" type="{http://www.metaswitch.com/cfs/billing/V1.0}CDRSeqNumType" />
 *       &lt;attribute name="error" type="{http://www.metaswitch.com/cfs/billing/V1.0}ErrorType" default="no" />
 *       &lt;attribute name="testcall" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="correlator" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventType", propOrder = {
    "eventType",
    "feature",
    "featureInfo",
    "origParty",
    "termParty",
    "eventTime"
})
public class EventType {

    @XmlElement(name = "EventType", required = true)
    @XmlSchemaType(name = "string")
    protected EventTypeType eventType;
    @XmlElement(name = "Feature", required = true)
    @XmlSchemaType(name = "string")
    protected FeatureType feature;
    @XmlElement(name = "FeatureInfo")
    protected FeatureInfoType featureInfo;
    @XmlElement(name = "OrigParty")
    protected PartyType origParty;
    @XmlElement(name = "TermParty")
    protected PartyType termParty;
    @XmlElement(name = "EventTime", required = true)
    protected String eventTime;
    @XmlAttribute(name = "seqnum", required = true)
    protected int seqnum;
    @XmlAttribute(name = "error")
    protected ErrorType error;
    @XmlAttribute(name = "testcall")
    protected Boolean testcall;
    @XmlAttribute(name = "correlator")
    protected String correlator;

    /**
     * Gets the value of the eventType property.
     * 
     * @return
     *     possible object is
     *     {@link EventTypeType }
     *     
     */
    public EventTypeType getEventType() {
        return eventType;
    }

    /**
     * Sets the value of the eventType property.
     * 
     * @param value
     *     allowed object is
     *     {@link EventTypeType }
     *     
     */
    public void setEventType(EventTypeType value) {
        this.eventType = value;
    }

    /**
     * Gets the value of the feature property.
     * 
     * @return
     *     possible object is
     *     {@link FeatureType }
     *     
     */
    public FeatureType getFeature() {
        return feature;
    }

    /**
     * Sets the value of the feature property.
     * 
     * @param value
     *     allowed object is
     *     {@link FeatureType }
     *     
     */
    public void setFeature(FeatureType value) {
        this.feature = value;
    }

    /**
     * Gets the value of the featureInfo property.
     * 
     * @return
     *     possible object is
     *     {@link FeatureInfoType }
     *     
     */
    public FeatureInfoType getFeatureInfo() {
        return featureInfo;
    }

    /**
     * Sets the value of the featureInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link FeatureInfoType }
     *     
     */
    public void setFeatureInfo(FeatureInfoType value) {
        this.featureInfo = value;
    }

    /**
     * Gets the value of the origParty property.
     * 
     * @return
     *     possible object is
     *     {@link PartyType }
     *     
     */
    public PartyType getOrigParty() {
        return origParty;
    }

    /**
     * Sets the value of the origParty property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartyType }
     *     
     */
    public void setOrigParty(PartyType value) {
        this.origParty = value;
    }

    /**
     * Gets the value of the termParty property.
     * 
     * @return
     *     possible object is
     *     {@link PartyType }
     *     
     */
    public PartyType getTermParty() {
        return termParty;
    }

    /**
     * Sets the value of the termParty property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartyType }
     *     
     */
    public void setTermParty(PartyType value) {
        this.termParty = value;
    }

    /**
     * Gets the value of the eventTime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEventTime() {
        return eventTime;
    }

    /**
     * Sets the value of the eventTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEventTime(String value) {
        this.eventTime = value;
    }

    /**
     * Gets the value of the seqnum property.
     * 
     */
    public int getSeqnum() {
        return seqnum;
    }

    /**
     * Sets the value of the seqnum property.
     * 
     */
    public void setSeqnum(int value) {
        this.seqnum = value;
    }

    /**
     * Gets the value of the error property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorType }
     *     
     */
    public ErrorType getError() {
        if (error == null) {
            return ErrorType.NO;
        } else {
            return error;
        }
    }

    /**
     * Sets the value of the error property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorType }
     *     
     */
    public void setError(ErrorType value) {
        this.error = value;
    }

    /**
     * Gets the value of the testcall property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isTestcall() {
        if (testcall == null) {
            return false;
        } else {
            return testcall;
        }
    }

    /**
     * Sets the value of the testcall property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTestcall(Boolean value) {
        this.testcall = value;
    }

    /**
     * Gets the value of the correlator property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrelator() {
        return correlator;
    }

    /**
     * Sets the value of the correlator property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrelator(String value) {
        this.correlator = value;
    }

}
