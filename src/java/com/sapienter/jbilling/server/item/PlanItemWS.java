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

package com.sapienter.jbilling.server.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.util.cxf.CxfSMapDatePriceModelAdapter;
import com.sapienter.jbilling.server.util.json.deserilizers.PricesTimeLineDeserializer;
import com.sapienter.jbilling.server.util.json.serializers.PriceTimeLineSerializer;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Brian Cowdery
 * @since 20-09-2010
 */
@ApiModel(value = "Plan Item Data", description = "PlanItemWS model")
public class PlanItemWS implements Serializable {

    public static final Integer DEFAULT_PRECEDENCE = -1;

    private Integer id;
    private Integer itemId; // affected item
    private SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
    private PriceModelWS model;
    private PlanItemBundleWS bundle;
    private Integer precedence = DEFAULT_PRECEDENCE;

    public PlanItemWS() {
    }

    public PlanItemWS(Integer itemId, PriceModelWS model, PlanItemBundleWS bundle) {
        this.itemId = itemId;
        this.bundle = bundle;

        this.models.put(CommonConstants.EPOCH_DATE, model);
        this.model = model;
    }

    public PlanItemWS(Integer itemId, SortedMap<Date, PriceModelWS> models, PlanItemBundleWS bundle) {
        this.itemId = itemId;
        this.models = models;
        this.bundle = bundle;
    }

    @ApiModelProperty(value = "Plan item unique identifier")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Id of the item used as a plan item", required = true)
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @JsonIgnore
    public Integer getAffectedItemId() {
        return getItemId();
    }

    @JsonIgnore
    public void setAffectedItemId(Integer affectedItemId) {
        setItemId(affectedItemId);
    }

    @XmlJavaTypeAdapter(CxfSMapDatePriceModelAdapter.class)
    @JsonSerialize(using = PriceTimeLineSerializer.class)
    @JsonDeserialize(using = PricesTimeLineDeserializer.class)
    @ApiModelProperty(value = "Price model of the plan item", required = true)
    public SortedMap<Date, PriceModelWS> getModels() {
        return models;
    }

    public void setModels(SortedMap<Date, PriceModelWS> models) {
        this.models = models;
    }

    public void addModel(Date date, PriceModelWS model) {
        getModels().put(date, model);
    }

    public void removeModel(Date date) {
        getModels().remove(date);
    }

    /**
     * Get the current price model for today.
     *
     * @return today's price
     */
    @ApiModelProperty(value = "Get the current price model for today")
    public PriceModelWS getModel() {
        return model;
    }
    
    public void setModel(PriceModelWS model){
    	this.model = model;
    }

    @ApiModelProperty(value = "Plan item bundle", required = true)
    public PlanItemBundleWS getBundle() {
        return bundle;
    }

    public void setBundle(PlanItemBundleWS bundle) {
        this.bundle = bundle;
    }

    @ApiModelProperty(value = "Plan item display position? TODO - revisit this, it is not totally accurate, it is related to plan prices.")
    public Integer getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Integer precedence) {
        this.precedence = precedence;
    }

    @Override
    public String toString() {
        return "PlanItemWS{"
               + "id=" + id
               + ", itemId=" + itemId
               + ", models=" + models
               + ", bundle=" + bundle
               + ", precedence=" + precedence
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanItemWS)) return false;

        PlanItemWS that = (PlanItemWS) o;
        return nullSafeEquals(id, that.id) &&
                nullSafeEquals(itemId, that.itemId) &&
                nullSafeEquals(models, that.models) &&
                nullSafeEquals(model, that.model) &&
                nullSafeEquals(bundle, that.bundle) &&
                nullSafeEquals(precedence, that.precedence);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(itemId);
        result = 31 * result + nullSafeHashCode(models);
        result = 31 * result + nullSafeHashCode(model);
        result = 31 * result + nullSafeHashCode(bundle);
        result = 31 * result + nullSafeHashCode(precedence);
        return result;
    }

    public static Comparator<PlanItemWS> defaultComparator() {
        return new Comparator<PlanItemWS>() {
            @Override
            public int compare(PlanItemWS o1, PlanItemWS o2) {
                int result = o1.getItemId().compareTo(o2.getItemId());
                if(result == 0) {
                    if(o1.getPrecedence() != null && o2.getPrecedence() != null) {
                        result = o1.getPrecedence().compareTo(o2.getPrecedence());
                        if(result == 0) {
                            result = o1.getId().compareTo(o2.getId());
                        }
                    }
                }
                return result;
            }
        };
    }
}

