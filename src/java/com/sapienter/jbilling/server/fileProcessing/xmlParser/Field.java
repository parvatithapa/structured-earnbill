package com.sapienter.jbilling.server.fileProcessing.xmlParser;

/**
 * Created by aman on 24/8/15.
 */
public class Field {
    public static final String SYSTEM_DATE="SYSDATE";

    String fieldName;
    int maxSize;
    String dateFormat;
    String defaultValue;
    boolean notUsed;
    String comment;
    Visibility inbound;
    Visibility outbound;
    Values possibleValues;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isNotUsed() {
        return notUsed;
    }

    public void setNotUsed(boolean notUsed) {
        this.notUsed = notUsed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Visibility getInbound() {
        return inbound;
    }

    public void setInbound(Visibility inbound) {
        this.inbound = inbound;
    }

    public Visibility getOutbound() {
        return outbound;
    }

    public void setOutbound(Visibility outbound) {
        this.outbound = outbound;
    }

    public Values getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(Values possibleValues) {
        this.possibleValues = possibleValues;
    }

    @Override
    public String toString() {
        return "Field{" +
                "fieldName='" + fieldName + '\'' +
                ", maxSize=" + maxSize +
                ", dateFormat='" + dateFormat + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", notUsed=" + notUsed +
                ", comment='" + comment + '\'' +
                ", inbound=" + inbound +
                ", outbound=" + outbound +
                ", possibleValues=" + possibleValues +
                '}';
    }
}
