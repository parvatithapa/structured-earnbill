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
package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.reader;

import com.sapienter.jbilling.server.mediation.MapCallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;

import java.lang.invoke.MethodHandles;

import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.LineMapper;

public class DtOfflineCdrMediationRecordLineConverter extends BasicMediationRecordLineConverter implements LineMapper<MapCallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Format format;
    private String dateTimeFormatter;

    @Override
    public MapCallDataRecord convertLineToRecord(String line) {
        logger.debug("line ->{}", line);
        return super.convertLineToRecord(line, this.format, new MapCallDataRecord());
    }

    @Override
    public MapCallDataRecord convertLineToRecord(String line, Format format) {
        logger.debug("line ->{}", line);
        if (this.format != null) {
            return super.convertLineToRecord(line, this.format, new MapCallDataRecord());
        } else {
            return super.convertLineToRecord(line, format, new MapCallDataRecord());
        }
    }

    public void setFormat(Format format) {
        this.format = format;
    }
    
    public void setDateTimeFormatter(String formatter) {
        setDateFormat(DateTimeFormat.forPattern(formatter));
        this.dateTimeFormatter = formatter;
    }

    @Override
    public MapCallDataRecord mapLine(String line, int lineNumber) throws Exception {
        return convertLineToRecord(line);
    }
}
