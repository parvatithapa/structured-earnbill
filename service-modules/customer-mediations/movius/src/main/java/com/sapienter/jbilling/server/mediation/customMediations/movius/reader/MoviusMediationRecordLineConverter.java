package com.sapienter.jbilling.server.mediation.customMediations.movius.reader;

import java.util.Map;

import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.LineMapper;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.customMediations.movius.CDRFormatNotFoundException;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class MoviusMediationRecordLineConverter implements LineMapper<ICallDataRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(MoviusMediationRecordLineConverter.class);

    private BasicMediationRecordLineConverter converter = new BasicMediationRecordLineConverter();
    private final CustomPatternMatcher crdRecordFormat;
    private final String fieldSeparator;
    
    public MoviusMediationRecordLineConverter(Map<String, MoviusRecordFormatContainer> crdRecordFormat, String fieldSeparator) {
        this.crdRecordFormat = new CustomPatternMatcher(crdRecordFormat);
        this.fieldSeparator = fieldSeparator;
    }
    
    @Override
    public ICallDataRecord mapLine(String line, int lineNumber) throws Exception {
        MoviusRecordFormatContainer formatContainer = crdRecordFormat.match(line, fieldSeparator);
        
        if(null == formatContainer) {
            LOG.debug("Skipping line {} Since no format found for it's cdr type", line);
            throw new CDRFormatNotFoundException("CDR Format not found for " + line);
        }
        
        converter.setDateFormat(DateTimeFormat.forPattern(formatContainer.getDatePattern()));
        return converter.convertLineToRecord(line,formatContainer.getFormat());
    }

    public CustomPatternMatcher getCrdRecordFormat() {
        return crdRecordFormat;
    }

}
