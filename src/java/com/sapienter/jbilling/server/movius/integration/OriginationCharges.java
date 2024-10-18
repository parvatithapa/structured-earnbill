
package com.sapienter.jbilling.server.movius.integration;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="provider" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="country" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="charges" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *                             &lt;element name="org-mapping">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="org" maxOccurs="unbounded">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="org-id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                                 &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                     &lt;/sequence>
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="system-id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "provider"
})
@XmlRootElement(name = "origination-charges")
public class OriginationCharges {

    @XmlElement(required = true)
    protected List<Provider> provider;
    @XmlAttribute(name = "system-id")
    protected String systemId;

    /**
     * Gets the value of the provider property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the provider property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProvider().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OriginationCharges.Provider }
     *
     *
     */
    public List<Provider> getProvider() {
        if (provider == null) {
            provider = new ArrayList<>();
        }
        return this.provider;
    }

    /**
     * Gets the value of the systemId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets the value of the systemId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSystemId(String value) {
        this.systemId = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="country" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                   &lt;element name="charges" type="{http://www.w3.org/2001/XMLSchema}double"/>
     *                   &lt;element name="org-mapping">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="org" maxOccurs="unbounded">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="org-id" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                                       &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                           &lt;/sequence>
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "name",
        "country"
    })
    public static class Provider {

        @XmlElement(required = true)
        protected String name;
        @XmlElement(required = true)
        protected List<Country> country;

        /**
         * Gets the value of the name property.
         *
         * @return
         *     possible object is
         *     {@link String }
         *
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the value of the name property.
         *
         * @param value
         *     allowed object is
         *     {@link String }
         *
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Gets the value of the country property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the country property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getCountry().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link OriginationCharges.Provider.Country }
         *
         *
         */
        public List<Country> getCountry() {
            if (country == null) {
                country = new ArrayList<>();
            }
            return this.country;
        }


        /**
         * <p>Java class for anonymous complex type.
         *
         * <p>The following schema fragment specifies the expected content contained within this class.
         *
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;sequence>
         *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *         &lt;element name="charges" type="{http://www.w3.org/2001/XMLSchema}double"/>
         *         &lt;element name="org-mapping">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="org" maxOccurs="unbounded">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="org-id" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                             &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                 &lt;/sequence>
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         *
         *
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "name",
            "charges",
            "orgMapping"
        })
        public static class Country {

            @XmlElement(required = true)
            protected String name;
            protected double charges;
            @XmlElement(name = "org-mapping", required = true)
            protected OriginationCharges.Provider.Country.OrgMapping orgMapping;

            /**
             * Gets the value of the name property.
             *
             * @return
             *     possible object is
             *     {@link String }
             *
             */
            public String getName() {
                return name;
            }

            /**
             * Sets the value of the name property.
             *
             * @param value
             *     allowed object is
             *     {@link String }
             *
             */
            public void setName(String value) {
                this.name = value;
            }

            /**
             * Gets the value of the charges property.
             *
             */
            public double getCharges() {
                return charges;
            }

            /**
             * Sets the value of the charges property.
             *
             */
            public void setCharges(double value) {
                this.charges = value;
            }

            /**
             * Gets the value of the orgMapping property.
             *
             * @return
             *     possible object is
             *     {@link OriginationCharges.Provider.Country.OrgMapping }
             *
             */
            public OriginationCharges.Provider.Country.OrgMapping getOrgMapping() {
                return orgMapping;
            }

            /**
             * Sets the value of the orgMapping property.
             *
             * @param value
             *     allowed object is
             *     {@link OriginationCharges.Provider.Country.OrgMapping }
             *
             */
            public void setOrgMapping(OriginationCharges.Provider.Country.OrgMapping value) {
                this.orgMapping = value;
            }


            /**
             * <p>Java class for anonymous complex type.
             *
             * <p>The following schema fragment specifies the expected content contained within this class.
             *
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;sequence>
             *         &lt;element name="org" maxOccurs="unbounded">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="org-id" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *                   &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *       &lt;/sequence>
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             *
             *
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "org"
            })
            public static class OrgMapping {

                @XmlElement(required = true)
                protected List<Org> org;

                /**
                 * Gets the value of the org property.
                 *
                 * <p>
                 * This accessor method returns a reference to the live list,
                 * not a snapshot. Therefore any modification you make to the
                 * returned list will be present inside the JAXB object.
                 * This is why there is not a <CODE>set</CODE> method for the org property.
                 *
                 * <p>
                 * For example, to add a new item, do as follows:
                 * <pre>
                 *    getOrg().add(newItem);
                 * </pre>
                 *
                 *
                 * <p>
                 * Objects of the following type(s) are allowed in the list
                 * {@link OriginationCharges.Provider.Country.OrgMapping.Org }
                 *
                 *
                 */
                public List<Org> getOrg() {
                    if (org == null) {
                        org = new ArrayList<>();
                    }
                    return this.org;
                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="org-id" type="{http://www.w3.org/2001/XMLSchema}string"/>
                 *         &lt;element name="count" type="{http://www.w3.org/2001/XMLSchema}int"/>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "orgId",
                    "count"
                })
                public static class Org {

                    @XmlElement(name = "org-id", required = true)
                    protected String orgId;
                    protected int count;

                    /**
                     * Gets the value of the orgId property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getOrgId() {
                        return orgId;
                    }

                    /**
                     * Sets the value of the orgId property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setOrgId(String value) {
                        this.orgId = value;
                    }

                    /**
                     * Gets the value of the count property.
                     * 
                     */
                    public int getCount() {
                        return count;
                    }

                    /**
                     * Sets the value of the count property.
                     * 
                     */
                    public void setCount(int value) {
                        this.count = value;
                    }

                    @Override
                    public String toString() {
                        return "Org [orgId=" + orgId + ", count=" + count + "]";
                    }

                }

            }

        }

    }

}
