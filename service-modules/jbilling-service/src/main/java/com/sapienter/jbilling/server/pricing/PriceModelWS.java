/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.cxf.CxfSMapStringStringAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Brian Cowdery
 * @since 06-08-2010
 */
@ApiModel(value = "Price Model Data", description = "PriceModelWS model")
public class PriceModelWS implements Serializable {

    private static final long serialVersionUID = 20130704L;

    public static final String ATTRIBUTE_WILDCARD = "*";

    private Integer id;
    private String type;
    private Map<String, String> attributes = new LinkedHashMap<String, String>();
    private String rate;
    private Integer currencyId;
    private PriceModelWS next;
    private String currencySymbol;

    public PriceModelWS() {
    }

    public PriceModelWS(String type) {
        this.type = type;
    }

    public PriceModelWS(String type, BigDecimal rate, Integer currencyId) {
        this.type = type;
        this.rate = (rate != null ? rate.toString() : null);
        this.currencyId = currencyId;
    }

    public PriceModelWS(String type, BigDecimal rate, Integer currencyId, String currencySymbol) {
        this.type = type;
        this.rate = (rate != null ? rate.toString() : null);
        this.currencyId = currencyId;
        this.currencySymbol = currencySymbol;
    }


    public PriceModelWS(PriceModelWS ws) {
        this.id = ws.getId();
        this.type = ws.getType();
        this.attributes = new LinkedHashMap<>(ws.getAttributes());
        this.rate = ws.getRate();
        this.currencyId = ws.getCurrencyId();
        this.currencySymbol = ws.getCurrencySymbol();
        if (ws.getNext() != null) this.next = new PriceModelWS(ws.getNext());
    }

    @ApiModelProperty(value = "A unique number that identifies this record")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "String value representing the pricing strategy type name", required = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlJavaTypeAdapter(CxfSMapStringStringAdapter.class)
    @ApiModelProperty(value = "A map of price model attributes. Different attributes (name, value pairs) are required by individual pricing strategy types for use in the calculation of price")
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String name, String value) {
        this.attributes.put(name, value);
    }

    @JsonIgnore
    public String getRate() {
        return rate;
    }

    @JsonProperty(value = "rate")
    @ApiModelProperty(value = "A decimal value of the available rate. This is a default rate that is used for price calculation, unless overridden by a pricing strategy")
    public BigDecimal getRateAsDecimal() {
        return Util.string2decimal(rate);
    }

    @JsonIgnore
    public void setRateAsDecimal(BigDecimal rate) {
        setRate(rate);
    }

    @JsonIgnore
    public void setRate(String rate) {
        this.rate = rate;
    }

    @JsonProperty(value = "rate")
    public void setRate(BigDecimal rate) {
        this.rate = (rate != null ? rate.toString() : null);
    }

    @ApiModelProperty(value = "The currency used for this price model", required = true)
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "The next price model for the plan that references this price model")
    public PriceModelWS getNext() {
        return next;
    }

    public void setNext(PriceModelWS next) {
        this.next = next;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    @ApiModelProperty(value = "The currency symbol")
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    @Override
    public String toString() {
        return "PriceModelWS{"
                + "id=" + id
                + ", type='" + type + '\''
                + ", attributes=" + attributes
                + ", rate=" + rate
                + ", currencyId=" + currencyId
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceModelWS)) return false;

        PriceModelWS that = (PriceModelWS) o;
        return nullSafeEquals(id, that.id) &&
                nullSafeEquals(type, that.type) &&
                nullSafeEquals(attributes, that.attributes) &&
                Util.decimalEquals(getRateAsDecimal(), that.getRateAsDecimal()) &&
                nullSafeEquals(currencyId, that.currencyId) &&
                nullSafeEquals(next, that.next);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(type);
        result = 31 * result + nullSafeHashCode(attributes);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getRateAsDecimal()));
        result = 31 * result + nullSafeHashCode(currencyId);
        result = 31 * result + nullSafeHashCode(next);
        return result;
    }
}
