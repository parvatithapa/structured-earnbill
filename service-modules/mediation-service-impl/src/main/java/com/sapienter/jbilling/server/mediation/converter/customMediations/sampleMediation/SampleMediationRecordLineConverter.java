package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation;/*
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

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import org.joda.time.format.DateTimeFormat;
import org.springframework.batch.item.file.LineMapper;

/**
 * Created by marcomanzi on 5/30/14.
 */
public class SampleMediationRecordLineConverter extends BasicMediationRecordLineConverter implements LineMapper<ICallDataRecord> {

    private Format format;

    @Override
    public ICallDataRecord convertLineToRecord(String line) {
        setDateFormat(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        return super.convertLineToRecord(line, this.format);
    }

    @Override
    public ICallDataRecord convertLineToRecord(String line, Format format) {
        setDateFormat(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        if (this.format != null) {
            return super.convertLineToRecord(line, this.format);
        } else {
            return super.convertLineToRecord(line, format);
        }
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    @Override
    public ICallDataRecord mapLine(String line, int lineNumber) throws Exception {
        return convertLineToRecord(line);
    }
}
