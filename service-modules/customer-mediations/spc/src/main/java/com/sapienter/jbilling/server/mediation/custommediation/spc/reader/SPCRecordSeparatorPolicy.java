package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;

/**
 * @author Neelabh
 * @since Dec 18, 2018
 */
public class SPCRecordSeparatorPolicy extends DefaultRecordSeparatorPolicy {

    private String cdrTypeIdentifier;
    private String fieldSeparator;

    @Override
    public boolean isEndOfRecord(final String line) {
        if (isLineEmpty(line) || shouldSkip(line)) {
            return false;
        }
        return super.isEndOfRecord(line);
    }

    /**
     * Returns the line as it should be appended to a record.
     */
    @Override
    public String preProcess(String line) {
        if (isLineEmpty(line) || shouldSkip(line)) {
            return "";
        }
        return super.preProcess(line);
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    public void setFieldSeparator(String fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    public String getCdrTypeIdentifier() {
        return cdrTypeIdentifier;
    }

    public void setCdrTypeIdentifier(String cdrTypeIdentifier) {
        this.cdrTypeIdentifier = cdrTypeIdentifier;
    }

    /**
     * returns true if checks cdr line is blank or empty 
     * else return false.
     * @param line
     */
    private boolean isLineEmpty(String line) {
        return null == line || line.trim().isEmpty();
    }

    private boolean shouldSkip(String line) {
        boolean skip = false;
        String[] cdrTypes = getCdrTypeIdentifier().split(",");
        for(String cdtType : cdrTypes) {
            if (line.startsWith(cdtType)) {
                skip = true;
                break;
            }
        }
        return skip;
    }
}
