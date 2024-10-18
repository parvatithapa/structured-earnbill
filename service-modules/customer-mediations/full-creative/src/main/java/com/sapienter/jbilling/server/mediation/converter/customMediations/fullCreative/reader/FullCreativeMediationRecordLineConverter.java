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
package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.reader;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import org.joda.time.format.DateTimeFormat;
import org.springframework.batch.item.file.LineMapper;

/**
 * Created by neelabh on 15/02/16.
 */
public class FullCreativeMediationRecordLineConverter extends BasicMediationRecordLineConverter implements LineMapper<ICallDataRecord> {

    private Format format;
    private String dateTimeFormatter;

    @Override
    public ICallDataRecord convertLineToRecord(String line) {
        setDateFormat(DateTimeFormat.forPattern(dateTimeFormatter));
        return super.convertLineToRecord(line, this.format);
    }

    @Override
    public ICallDataRecord convertLineToRecord(String line, Format format) {
        setDateFormat(DateTimeFormat.forPattern(dateTimeFormatter));
        if (this.format != null) {
            return super.convertLineToRecord(line, this.format);
        } else {
            return super.convertLineToRecord(line, format);
        }
    }

    public void setFormat(Format format) {
        this.format = format;
    }
    
    public void setDateTimeFormatter(String formatter) {
        this.dateTimeFormatter = formatter;
    }

    @Override
    public ICallDataRecord mapLine(String line, int lineNumber) throws Exception {
        return convertLineToRecord(line);
    }
}
