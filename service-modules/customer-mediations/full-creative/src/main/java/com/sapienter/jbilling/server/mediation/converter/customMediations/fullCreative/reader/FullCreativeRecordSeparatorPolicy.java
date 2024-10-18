package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader;

import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;

/**
 * Created by neelabh on 05/06/16.
 */
public class FullCreativeRecordSeparatorPolicy extends DefaultRecordSeparatorPolicy {

	private String headerIdentifierName = null;
	private static final String DEFAULT_SEPARATOR = ",";

    private String fieldSeparator; 
	
    @Override
    public boolean isEndOfRecord(final String line) {
    	if (isLineEmpty(line) || line.startsWith(getHeaderIdentifierName())) {
            return false;
        }
        return super.isEndOfRecord(line);
    }

    /**
     * Returns the line as it should be appended to a record.
     */
    @Override
    public String preProcess(String line) {
    	if (isLineEmpty(line) || line.startsWith(getHeaderIdentifierName())) {
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

	public String getHeaderIdentifierName() {
	    return headerIdentifierName;
	}
	
	public void setHeaderIdentifierName(String headerIdentifierName) {
	    this.headerIdentifierName = headerIdentifierName;
	}
	
	/**
	 * returns true if checks cdr line is blank or empty 
	 * else return false.
	 * @param line
	 */
	private boolean isLineEmpty(String line) {
		return null == line || 
				line.trim().split(null != getFieldSeparator() ? getFieldSeparator() : DEFAULT_SEPARATOR).length == 0 ;
	}
}
