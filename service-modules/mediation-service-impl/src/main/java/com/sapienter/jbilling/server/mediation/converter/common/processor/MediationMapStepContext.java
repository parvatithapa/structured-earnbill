package com.sapienter.jbilling.server.mediation.converter.common.processor;/*
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

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.MapCallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import java.util.List;
import java.util.Map;

public class MediationMapStepContext {

    private MediationStepResult result;
    private MapCallDataRecord record;
    private Integer entityId;

    private MediationMapStepContext() {}

    public MediationMapStepContext(MediationStepResult result, MapCallDataRecord record, Integer entityId) {
        this.result = result;
        this.record = record;
        this.entityId = entityId;
    }

    public List<PricingField> getPricingFields() {
        return record.getFields();
    }

    public Map<String, PricingField> getPricingFieldsMap() {
        return record.getFieldMap();
    }

    public PricingField getPricingField(String name) {
        return record.getField(name);
    }

    public ICallDataRecord getRecord() {
        return record;
    }

    public void setRecord(MapCallDataRecord record) {
        this.record = record;
    }

    public MediationStepResult getResult() {
        return result;
    }

    public void setResult(MediationStepResult result) {
        this.result = result;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }
}
