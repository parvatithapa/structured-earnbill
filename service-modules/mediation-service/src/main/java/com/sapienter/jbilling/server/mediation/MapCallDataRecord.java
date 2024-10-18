package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.item.PricingField;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapCallDataRecord implements ICallDataRecord {

    private Map<String, PricingField> fieldMap = new LinkedHashMap<>();

    private StringBuffer key = new StringBuffer();
    private Date processingDate ;
    private int position = 1;
    private Integer entityId;
    private Integer mediationCfg;

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

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
        for (PricingField field : fieldMap.values()) {
            field.setPosition(position);
        }
    }

    @Override
    public List<PricingField> getFields() {
        return new ArrayList<>(fieldMap.values());
    }

    @Override
    public void setFields(List<PricingField> fields) {
        for(PricingField field : fields) {
            fieldMap.put(field.getName(), field);
        }
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
        sb.append(", fields=").append(fieldMap);
        sb.append(", recordId=").append(recordId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MapCallDataRecord record = (MapCallDataRecord) o;

        if (position != record.position) return false;
        if (!key.equals(record.key)) return false;
        if (fieldMap != null ? !fieldMap.equals(record.fieldMap) : record.fieldMap != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + position;
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
    public void addField(PricingField field, boolean isKey) {
        if (isKey && !fieldMap.containsKey(field.getName())) {
            key.append(field.getValue().toString());
        }
        fieldMap.put(field.getName(), field);
    }

    @Override
    public PricingField getField(String name) {
        return fieldMap.get(name);
    }

    public Map<String, PricingField> getFieldMap() {
        return fieldMap;
    }
}
