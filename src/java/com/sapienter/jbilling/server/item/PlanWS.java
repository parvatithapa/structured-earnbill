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
import com.sapienter.jbilling.common.CollectionUtil;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.cxf.CxfSMapIntMetafieldsAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.*;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Brian Cowdery
 * @since 20-09-2010
 */
@ApiModel(value = "Plan Data", description = "PlanWS model")
public class PlanWS implements WSSecured, Serializable {

    private Integer id;
    private Integer itemId; // plan subscription item
    private Integer periodId; // plan item period
    @Size (min=0,max=255, message="validation.error.size,1,255")
    private String description;
    private int editable = 0;
    private List<PlanItemWS> planItems = new ArrayList<PlanItemWS>();
    private Integer[] usagePoolIds;

    private boolean freeTrial;

    @Valid
    private MetaFieldValueWS[] metaFields;
    private SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap = new TreeMap<Integer, MetaFieldValueWS[]>();

    public PlanWS() {
    }

    @ApiModelProperty(value = "Plan unique identifier")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Id of the item used as a subscription item", required = true)
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @JsonIgnore
    public Integer getPlanSubscriptionItemId() {
        return getItemId();
    }

    @JsonIgnore
    public void setPlanSubscriptionItemId(Integer planSubscriptionItemId) {
        setItemId(planSubscriptionItemId);
    }

    @ApiModelProperty(value = "Id of the period used for the plan", required = true)
    public Integer getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Integer periodId) {
        this.periodId = periodId;
    }

    @ApiModelProperty(value = "Description of the plan")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Is the plan offers free trial")
    public boolean isFreeTrial() {
        return freeTrial;
    }

    public void setFreeTrial(boolean freeTrial) {
        this.freeTrial = freeTrial;
    }

	@JsonIgnore
    public int getEditable() {
        return editable;
    }

    @XmlTransient
    @JsonIgnore
    public void setEditable(int editable) {
        this.editable = editable;
    }

    @ApiModelProperty(value = "List of plan items included in the plan")
   	public List<PlanItemWS> getPlanItems() {
        return planItems;
    }

    public void setPlanItems(List<PlanItemWS> planItems) {
        this.planItems = planItems;
    }

    public void addPlanItem(PlanItemWS planItem) {
        getPlanItems().add(planItem);
    }

    @ApiModelProperty(value = "Defined meta fields for the plan")
    public MetaFieldValueWS[] getMetaFields() {
    	return metaFields;
    }

	public void setMetaFields(MetaFieldValueWS[] metaFields) {
    	this.metaFields = metaFields;
    }

    @XmlJavaTypeAdapter(CxfSMapIntMetafieldsAdapter.class)
    @ApiModelProperty(value = "Meta field values sorted map")
    public SortedMap <Integer, MetaFieldValueWS[]> getMetaFieldsMap() {
		return metaFieldsMap;
	}
	
	public void setMetaFieldsMap(SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap) {
		this.metaFieldsMap = metaFieldsMap;
	}

    @ApiModelProperty(value = "Array of usage pool ids")
	public Integer[] getUsagePoolIds() {
        return this.usagePoolIds;
    }

    public void setUsagePoolIds(Integer[] usagePoolIds) {
        this.usagePoolIds = usagePoolIds;
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return itemId != null ? new ItemBL(itemId).getEntity().getEntity().getId() : null;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

	@Override
    public String toString() {
		String strFUPIds = "";
		if (null != usagePoolIds && usagePoolIds.length > 0) {
			for (Integer fupId : usagePoolIds) {
				strFUPIds += fupId + ":";
			}
		}
        return "PlanWS{"
               + "id=" + id
               + ", itemId=" + itemId
               + ", periodId=" + periodId
               + ", description='" + description + '\''
                + ", editable=" + editable
               + ", planItems=" + planItems
               + ", usagePoolIds=" + strFUPIds
               + ", freeTrial=" + freeTrial
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanWS)) return false;

        PlanWS planWS = (PlanWS) o;
        return editable == planWS.editable &&
                nullSafeEquals(id, planWS.id) &&
                nullSafeEquals(itemId, planWS.itemId) &&
                nullSafeEquals(periodId, planWS.periodId) &&
                nullSafeEquals(description, planWS.description) &&
                nullSafeEquals(freeTrial, planWS.freeTrial) &&
                nullSafeEquals(CollectionUtil.nullSafeSort(planItems, PlanItemWS.defaultComparator()), CollectionUtil.nullSafeSort(planWS.planItems, PlanItemWS.defaultComparator())) &&
                nullSafeEquals(CollectionUtil.nullSafeSort(usagePoolIds), CollectionUtil.nullSafeSort(planWS.usagePoolIds)) &&
                nullSafeEquals(CollectionUtil.nullSafeSort(metaFields, MetaFieldValueWS.defaultComparator()), CollectionUtil.nullSafeSort(planWS.metaFields, MetaFieldValueWS.defaultComparator())) &&
                metaFieldsMapEquals(metaFieldsMap, planWS.metaFieldsMap);
    }

    private boolean metaFieldsMapEquals(SortedMap<Integer, MetaFieldValueWS[]> actual, SortedMap<Integer, MetaFieldValueWS[]> expected){

        if (actual == expected){
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        if (actual.size() != expected.size()){
            return false;
        }

        Set<Integer> actualKeySet = actual.keySet();
        Set<Integer> expectedKeySet = expected.keySet();
        if (!nullSafeEquals(actualKeySet, expectedKeySet)){
            return false;
        }
        for (Integer key : actualKeySet){
            if (!nullSafeEquals(actual.get(key), expected.get(key))){
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(itemId);
        result = 31 * result + nullSafeHashCode(periodId);
        result = 31 * result + nullSafeHashCode(description);
        result = 31 * result + editable;
        result = 31 * result + nullSafeHashCode(freeTrial);
        result = 31 * result + nullSafeHashCode(planItems);
        result = 31 * result + nullSafeHashCode(usagePoolIds);
        result = 31 * result + nullSafeHashCode(metaFields);
        result = 31 * result + nullSafeHashCode(metaFieldsMap);
        return result;
    }
}
