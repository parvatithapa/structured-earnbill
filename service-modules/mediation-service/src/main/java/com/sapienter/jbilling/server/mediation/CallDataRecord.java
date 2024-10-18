package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.item.PricingField;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by marcolin on 06/10/15.
 */
public class CallDataRecord implements ICallDataRecord {

    private StringBuffer key = new StringBuffer();
    private Date processingDate ;
    private int position = 1;
    private Integer entityId;
    private Integer mediationCfg;
    private List<PricingField> fields = new ArrayList<PricingField>();

    // Record format errors go here
    private List<String> errors = new ArrayList<String>(1);

    private String recordId;

    @Override
    public Date getProcessingDate() {
        return processingDate;
    }

    @Override
    public void setProcessingDate(String processingTime) {
        try{
            long processTime=Long.parseLong(processingTime);
            this.processingDate = new Date(processTime);
        }catch (NumberFormatException e){
            //wrong date format inside the hbase

        }
    }

    public CallDataRecord() {}

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
        for (PricingField field : fields) {
            field.setPosition(position);
        }
    }

    @Override
    public List<PricingField> getFields() {
        return fields;
    }

    @Override
    public void setFields(List<PricingField> fields) {
        this.fields = fields;
    }

    @Override
    public void addField(PricingField field, boolean isKey) {
        if (isKey && PricingField.find(fields, field.getName()) == null) {
            key.append(field.getValue().toString());
        }
        PricingField.add(fields, field);
    }

    @Override
    public String getKey() {
        return key.toString();
    }

    @Override
    public void setKey(String key) {
        this.key = new StringBuffer(key);
    }

    @Override
    public void appendKey(String key) {
        this.key.append(key);
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public void addError(String error) {
        errors.add(error);
    }

    @Override
    public String getRecordId() {
        return recordId;
    }

    @Override
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{key=").append(key);
        sb.append(", position=").append(position);
        sb.append(", fields=").append(fields);
        sb.append(", recordId=").append(recordId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallDataRecord record = (CallDataRecord) o;

        if (position != record.position) return false;
        if (fields != null ? !fields.equals(record.fields) : record.fields != null) return false;
        if (!key.equals(record.key)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + position;
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public Integer getMediationCfgId() {
        return mediationCfg;
    }

    @Override
    public void setMediationCfgId(Integer mediationCfgId) {
        this.mediationCfg = mediationCfgId;
    }

    @Override
    public PricingField getField(String name) {
        return PricingField.find(fields, name);
    }
}
