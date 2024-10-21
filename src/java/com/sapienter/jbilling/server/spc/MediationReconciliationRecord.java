package com.sapienter.jbilling.server.spc;

import java.lang.reflect.Field;
import java.util.StringJoiner;

public class MediationReconciliationRecord {
    String fileName;
    Integer totalRecords;
    Integer totalProcessedRecords;
    Integer difference;
    String comment;

    public MediationReconciliationRecord(String fileName, Integer totalRecords, Integer totalProcessedRecords, Integer difference, String comment) {
        this.fileName = fileName;
        this.totalRecords = totalRecords;
        this.totalProcessedRecords = totalProcessedRecords;
        this.difference = difference;
        this.comment = comment;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getTotalProcessedRecords() {
        return totalProcessedRecords;
    }

    public void setTotalProcessedRecords(Integer totalProcessedRecords) {
        this.totalProcessedRecords = totalProcessedRecords;
    }

    public Integer getDifference() {
        return difference;
    }

    public void setDifference(Integer difference) {
        this.difference = difference;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public static String getHeaders() {
        Field[] fields = MediationReconciliationRecord.class.getDeclaredFields();
        StringJoiner joiner = new StringJoiner(",");
        for (Field field : fields) {
            joiner.add(field.getName());
        }
        return joiner.toString();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");
        joiner.add(fileName).add(totalRecords.toString()).add(totalProcessedRecords.toString()).add(difference.toString()).add(comment);
        return joiner.toString();
    }
}
