/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.reader;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.FormatField;

import org.apache.commons.lang.NotImplementedException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Panche Isajeski
 * @since 12/18/12
 */
public class BasicMediationRecordLineConverter implements MediationRecordLineConverter {

	// Commented as Date Format for AC is configured in resources.xml
	// private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd-HHmmss");
	private boolean removeQuote = true;

    @Override
    public ICallDataRecord convertLineToRecord(String line) {
        throw new NotImplementedException();
    }

    @Override
    public ICallDataRecord convertLineToRecord(String line, Format format) {
        return convertLineToRecord(line, format, new CallDataRecord());
    }

    public <T extends ICallDataRecord> T convertLineToRecord(String line, Format format, T record) {

        MediationTokenizer tokenizer = format.getTokenizer();
        if (tokenizer != null) {
            String [] tokens = tokenizer.tokenize(line, format);

            // remove quotes if needed
            if (removeQuote) {
                for (int f = 0; f < tokens.length; f++) {
                    if (tokens[f].length() < 2) {
                        continue;
                    }
                    // remove first and last char, if they are quotes
                    if ((tokens[f].charAt(0) == '\"' || tokens[f].charAt(0) == '\'') &&
                            (tokens[f].charAt(tokens[f].length() - 1) == '\"' || tokens[f].charAt(tokens[f].length() - 1) == '\'')) {
                        tokens[f] = tokens[f].substring(1, tokens[f].length() - 1);
                    }
                }
            }

            // create the record
            int tkIdx = 0;

            try {
                for (FormatField field : format.getFields()) {

                    switch (PricingField.mapType(field.getType())) {
                        case STRING:
                            record.addField(new PricingField(field.getName(),
                                    tokens[tkIdx++]), field.getIsKey());
                            break;
                        case INTEGER:
                            String intStr = tokens[tkIdx++].trim();
                            try {
                                record.addField(new PricingField(field.getName(), intStr.length() > 0 ?
                                        Integer.valueOf(intStr.trim()) : null), field.getIsKey());
                            } catch (NumberFormatException e) {

                            }
                            break;
                        case DATE:
                            try {
                                String dateStr = tokens[tkIdx++];
                                record.addField(new PricingField(field.getName(), dateStr.length() > 0 ?
                                        dateFormat.parseDateTime(dateStr).toDate() : null), field.getIsKey());
                            } catch (IllegalArgumentException e) {

                            }
                            break;
                        case DECIMAL:
                            String floatStr = tokens[tkIdx++].trim();
                            record.addField(new PricingField(field.getName(), floatStr.length() > 0 ?
                                    new BigDecimal(floatStr) : null), field.getIsKey());
                            break;
                        case BOOLEAN:
                            boolean value = "true".equalsIgnoreCase(tokens[tkIdx++].trim());
                            record.addField(new PricingField(field.getName(), value), field.getIsKey());
                            break;
                        case LONG:
                            String longStr = tokens[tkIdx++].trim();
                            try {
                                record.addField(new PricingField(field.getName(), longStr.length() > 0 ?
                                        Long.valueOf(longStr.trim()) : null), field.getIsKey());
                            } catch (NumberFormatException e) {

                            }
                    }
                }
            } catch (RuntimeException e) {
                record.getErrors().add(e.getMessage());
            }

            return record;
        }
        return null;
    }

    public void setDateFormat(DateTimeFormatter dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setRemoveQuote(boolean removeQuote) {
        this.removeQuote = removeQuote;
    }
    
}
