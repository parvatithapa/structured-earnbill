package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.batch;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.LineMapper;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CDRFormatNotFoundException;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class OptusMurCdrLineMapper implements LineMapper<ICallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<String, Format> cdrTypeAndFormatMap;
    private final String fieldSeparator;
    private final BasicMediationRecordLineConverter lineConverter;

    public OptusMurCdrLineMapper(Map<String, Format> cdrTypeAndFormatMap, String fieldSeparator) {
        this.cdrTypeAndFormatMap = cdrTypeAndFormatMap;
        this.fieldSeparator = fieldSeparator;
        this.lineConverter = new BasicMediationRecordLineConverter();
    }

    @Override
    public ICallDataRecord mapLine(String line, int lineNumber) {
        String key = line.split(fieldSeparator, -1)[0];
        if(StringUtils.isEmpty(key)) {
            throw new CDRFormatNotFoundException("cdr format not found for empty record type");
        }
        logger.debug("looking format for key {}", key);
        Format format = cdrTypeAndFormatMap.get(key);
        if(null == format) {
            throw new CDRFormatNotFoundException("cdr format not found for key " + key);
        }
        return lineConverter.convertLineToRecord(line, format);
    }

}
