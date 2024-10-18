package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CDRFormatNotFoundException;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.AAPTVoipCtopRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.OptusFixedLineRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.OptusMobileRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.TelstraRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;

/**
 * @author Neelabh
 * @since Jan 17, 2019
 */
public class SPCPatternBasedCompositeRecordLineConverter implements LineMapper<ICallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PERIOD = ".";
    private static final String BBS = "BBS";
    private static final String COMMA_SEPARATOR = ",";

    private SPCMediationRecordLineMapper lineMapper = new SPCMediationRecordLineMapper();
    private CustomFieldSetMapper<ICallDataRecord> fieldSetMapper;
    private Map<String, LineTokenizer> lineTokenizer;
    private Map<String, SPCRecordFormatContainer> crdRecordFormat;

    @Value("#{jobParameters['filePath']}")
    private String resource;
    private MediationServiceType mediationServiceType;

    public SPCPatternBasedCompositeRecordLineConverter(Map<String, LineTokenizer> lineTokenizer,
            Map<String, SPCRecordFormatContainer> crdRecordFormat) {
        this.lineTokenizer = lineTokenizer;
        this.crdRecordFormat = crdRecordFormat;
    }

    @Override
    public ICallDataRecord mapLine(String line, int lineNumber) throws Exception {
        ICallDataRecord record = null;
        if(resource.contains(BBS)) {
            this.mediationServiceType =
                    (StringUtils.isNumeric(line.split(COMMA_SEPARATOR)[0])) ?
                            MediationServiceType.SERVICE_ELEMENTS_VOICE : MediationServiceType.SERVICE_ELEMENTS_DATA;
        } else {
            for(MediationServiceType mediationType : MediationServiceType.values()) {
                if(resource.contains(mediationType.getFileNamePrefix())) {
                    this.mediationServiceType = mediationType;
                    break;
                }
            }
        }
        switch (mediationServiceType) {
        case OPTUS_FIXED_LINE:
            record = convertFixedLengthRecord(line, OptusFixedLineRecord.getTypeCodes());
            break;
        case OPTUS_MOBILE:
            record = convertFixedLengthRecord(line, OptusMobileRecord.getTypeCodes());
            break;
        case AAPT_VOIP_CTOP:
            record = convertFixedLengthRecord(line, AAPTVoipCtopRecord.getTypeCodes());
            break;
        case TELSTRA_FIXED_LINE:
            record = convertFixedLengthRecord(line, TelstraRecord.getTypeCodes());
            break;
        case ENGIN:
        case SCONNECT:
        case SCONNECT_DATA:
        case AAPT_INTERNET_USAGE:
        case TELSTRA_MOBILE_4G:
        case SERVICE_ELEMENTS_VOICE:
        case SERVICE_ELEMENTS_DATA:
            record = convertCommaDelimitedRecord(line);
            break;
        default:
            logger.debug("Invalid mediation file name. Did not find CDR resolver {} ", mediationServiceType);
            break;
        }
        if (record != null) {
            record.addField(new PricingField(SPCConstants.SERVICE_TYPE, mediationServiceType.getServiceName()), false);
        }
        return record;
    }

    public CustomFieldSetMapper<ICallDataRecord> getFieldSetMapper() {
        return fieldSetMapper;
    }

    public void setFieldSetMapper(CustomFieldSetMapper<ICallDataRecord> fieldSetMapper) {
        this.fieldSetMapper = fieldSetMapper;
    }

    public Map<String, LineTokenizer> getLineTokenizer() {
        return lineTokenizer;
    }

    public void setLineTokenizer(Map<String, LineTokenizer> lineTokenizer) {
        this.lineTokenizer = lineTokenizer;
    }

    //Generic method to convert/parse fixed length records
    private ICallDataRecord convertFixedLengthRecord(String line, String[] recordTypes) {
        ICallDataRecord record = null;
        LineTokenizer lt = lineTokenizer.get(mediationServiceType.getServiceName());
        if (lt == null) {
            for(String recordType : recordTypes) {
                if (line.startsWith(recordType)) {
                    lt = lineTokenizer.get(mediationServiceType.getServiceName() + PERIOD + recordType);
                    break;
                }
            }
        }
        if(lt == null) {
            logger.debug("Skipping line {} since no format found for it's cdr type", line);
            throw new CDRFormatNotFoundException("CDR format not found for " + line);
        }
        FieldSet fields = lt.tokenize(line);
        record = fieldSetMapper.mapLineToRecord(fields, mediationServiceType);
        return record;
    }

    //Generic method to convert/parse comma delimited records
    private ICallDataRecord convertCommaDelimitedRecord(String line) {
        SPCRecordFormatContainer recordFormatContainer = crdRecordFormat.get(mediationServiceType.getServiceName());
        if(null == recordFormatContainer) {
            logger.debug("Skipping line {} since no format found for it's cdr type", line);
            throw new CDRFormatNotFoundException("CDR format not found for " + line);
        }
        lineMapper.setDateFormat(DateTimeFormat.forPattern(recordFormatContainer.getDatePattern()));
        return lineMapper.mapLineToRecord(line, recordFormatContainer.getFormat(), mediationServiceType);
    }
}
