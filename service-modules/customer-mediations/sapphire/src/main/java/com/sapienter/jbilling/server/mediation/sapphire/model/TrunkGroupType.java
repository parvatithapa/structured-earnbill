//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.07.09 at 02:46:25 PM IST 
//


package com.sapienter.jbilling.server.mediation.sapphire.model;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Specifies the characteristics of a trunk group that was
 *         used in the routing of a call.
 *       
 * 
 * <p>Java class for TrunkGroupType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TrunkGroupType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TrunkGroupId" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkGroupIdType"/>
 *         &lt;element name="TrunkMemberId" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkMemberIdType"/>
 *         &lt;element name="OrigTrunkGroupLabel" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkGroupLabelType" minOccurs="0"/>
 *         &lt;element name="OrigTrunkContext" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkContextType" minOccurs="0"/>
 *         &lt;element name="DestTrunkGroupLabel" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkGroupLabelType" minOccurs="0"/>
 *         &lt;element name="DestTrunkContext" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkContextType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="type" use="required" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkTypeType" />
 *       &lt;attribute name="trunkaccounting" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkAccountingType" />
 *       &lt;attribute name="trunkname" type="{http://www.metaswitch.com/cfs/billing/V1.0}TrunkNameType" />
 *       &lt;attribute name="dup" type="{http://www.metaswitch.com/cfs/billing/V1.0}DupType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TrunkGroupType", propOrder = {
    "trunkGroupId",
    "trunkMemberId",
    "origTrunkGroupLabel",
    "origTrunkContext",
    "destTrunkGroupLabel",
    "destTrunkContext"
})
public class TrunkGroupType {

    @XmlElement(name = "TrunkGroupId")
    @XmlSchemaType(name = "unsignedLong")
    protected int trunkGroupId;
    @XmlElement(name = "TrunkMemberId", required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger trunkMemberId;
    @XmlElement(name = "OrigTrunkGroupLabel")
    protected String origTrunkGroupLabel;
    @XmlElement(name = "OrigTrunkContext")
    protected String origTrunkContext;
    @XmlElement(name = "DestTrunkGroupLabel")
    protected String destTrunkGroupLabel;
    @XmlElement(name = "DestTrunkContext")
    protected String destTrunkContext;
    @XmlAttribute(name = "type", required = true)
    protected TrunkTypeType type;
    @XmlAttribute(name = "trunkaccounting")
    protected String trunkaccounting;
    @XmlAttribute(name = "trunkname")
    protected String trunkname;
    @XmlAttribute(name = "dup")
    protected DupType dup;

    /**
     * Gets the value of the trunkGroupId property.
     * 
     */
    public int getTrunkGroupId() {
        return trunkGroupId;
    }

    /**
     * Sets the value of the trunkGroupId property.
     * 
     */
    public void setTrunkGroupId(int value) {
        this.trunkGroupId = value;
    }

    /**
     * Gets the value of the trunkMemberId property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getTrunkMemberId() {
        return trunkMemberId;
    }

    /**
     * Sets the value of the trunkMemberId property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setTrunkMemberId(BigInteger value) {
        this.trunkMemberId = value;
    }

    /**
     * Gets the value of the origTrunkGroupLabel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrigTrunkGroupLabel() {
        return origTrunkGroupLabel;
    }

    /**
     * Sets the value of the origTrunkGroupLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrigTrunkGroupLabel(String value) {
        this.origTrunkGroupLabel = value;
    }

    /**
     * Gets the value of the origTrunkContext property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrigTrunkContext() {
        return origTrunkContext;
    }

    /**
     * Sets the value of the origTrunkContext property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrigTrunkContext(String value) {
        this.origTrunkContext = value;
    }

    /**
     * Gets the value of the destTrunkGroupLabel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDestTrunkGroupLabel() {
        return destTrunkGroupLabel;
    }

    /**
     * Sets the value of the destTrunkGroupLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDestTrunkGroupLabel(String value) {
        this.destTrunkGroupLabel = value;
    }

    /**
     * Gets the value of the destTrunkContext property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDestTrunkContext() {
        return destTrunkContext;
    }

    /**
     * Sets the value of the destTrunkContext property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDestTrunkContext(String value) {
        this.destTrunkContext = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link TrunkTypeType }
     *     
     */
    public TrunkTypeType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link TrunkTypeType }
     *     
     */
    public void setType(TrunkTypeType value) {
        this.type = value;
    }

    /**
     * Gets the value of the trunkaccounting property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrunkaccounting() {
        return trunkaccounting;
    }

    /**
     * Sets the value of the trunkaccounting property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrunkaccounting(String value) {
        this.trunkaccounting = value;
    }

    /**
     * Gets the value of the trunkname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrunkname() {
        return trunkname;
    }

    /**
     * Sets the value of the trunkname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrunkname(String value) {
        this.trunkname = value;
    }

    /**
     * Gets the value of the dup property.
     * 
     * @return
     *     possible object is
     *     {@link DupType }
     *     
     */
    public DupType getDup() {
        return dup;
    }

    /**
     * Sets the value of the dup property.
     * 
     * @param value
     *     allowed object is
     *     {@link DupType }
     *     
     */
    public void setDup(DupType value) {
        this.dup = value;
    }

}
