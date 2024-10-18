package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.Field;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileStructure;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class is used to send 814 Enrollment CQ Rate Change to the LDC
 *
 * @author Gerhard Maree
 * @since 05-11-2015
 */
public class EnrollmentCqRateRequest {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EnrollmentCqRateRequest.class));

    private CompanyDTO companyDTO;
    private CustomerDTO customerDTO;
    private Date effectiveDate;
    private Date completionDate;
    private BigDecimal rate;

    private EditFieldUtil editFieldUtil;
    private FileFormat fileFormat;

    //tx beans
    private IWebServicesSessionBean webServicesSessionBean;
    private IEDITransactionBean ediTransactionBean;

    //records in file
    protected List<Map<String, String>> recordFieldsList = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    //these fields are used to generate unique transaction refs
    private static String txPref;
    private static AtomicInteger counter;
    {
        counter = new AtomicInteger(1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmssSSS");
        txPref = dateFormat.format(TimezoneHelper.serverCurrentDate());
    }

    private static final String TRANSACTION_SET = "t814AutoRenew";

    /**
     * All the fields in the KEY record
     */
    private enum KeyRecord {
        REC_ID("KEY"),
        SUPPLIER_DUNS(),
        UTILITY_DUNS(),
        TRANSACTION_SET("814"),
        TRANSCTION_SUBSET("CQ"),
        STATE(),
        COMMODITY();

        private String value;

        KeyRecord() {
        }

        KeyRecord(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Fields of the Header record
     */
    private enum HeaderRecord {
        REC_ID("HDR"),
        TRANS_REF_NR(),
        UTILITY_NAME(),
        CREATE_DATE(),
        SUPPLIER_NAME();

        private String value;

        HeaderRecord() {
        }

        HeaderRecord(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Account record fields
     */
    private enum AccountRecord {
        REC_ID("ACT"),
        SERVICE_REQUESTED("CE"),
        LINE_NR("1"),
        UTILITY_CUST_ACCT_NR(),
        BILL_DELIVER("LDC"),
        BILL_CALC("LDC"),
        PERCENT_PARTICPATE("1");

        private String value;

        AccountRecord() {
        }

        AccountRecord(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Dates record fields
     */
    private enum DatesRecord {
        REC_ID("DTE"),
        EFFECTIVE_DT(),
        COMPLETION_DT();

        private String value;

        DatesRecord() {
        }

        DatesRecord(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Code record fields line 1
     */
    private enum MeterRecord {
        REC_ID("MTR"),
        METER_IDENTIFIER("ALL"),
        METER_ACTION("Q"),
        SUPPLIER_RATE_CD();

        private String value;

        MeterRecord() {
        }

        MeterRecord(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Code record fields line 1
     */
    private enum CodeRecord {
        REC_ID("CDE"),
        REC_TYPE("C"),
        CODE("REFRB");

        private String value;

        CodeRecord() {
        }

        CodeRecord(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public EnrollmentCqRateRequest(CompanyDTO companyDTO, CustomerDTO customerDTO, BigDecimal rate, Date effectiveDate, Date completionDate) {
        assert companyDTO != null;
        assert customerDTO != null;
        assert rate != null;
        assert effectiveDate != null;
        assert completionDate != null;

        this.companyDTO = companyDTO;
        this.customerDTO = customerDTO;
        this.effectiveDate = effectiveDate;
        this.completionDate = completionDate;
        this.rate = rate;
        recordFieldsList = new ArrayList<>() ;

        webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        editFieldUtil = new EditFieldUtil(companyDTO, customerDTO, effectiveDate, ediTransactionBean);
    }

    private void addRecordIdFieldName(String recordName, Map<String, String> record) {
        FileStructure structure = fileFormat.getFileStructure();
        Field field = structure.findRecordByName(recordName).getRecId();
        record.put(field.getFieldName(), field.getDefaultValue());
    }

    public void populateKeyRecord() {
        Map<String, String> records = new HashMap<>(4);
        addRecordIdFieldName(KeyRecord.REC_ID.value(), records);
        records.put(KeyRecord.REC_ID.name(), KeyRecord.REC_ID.value());
        records.put(KeyRecord.TRANSACTION_SET.name(), KeyRecord.TRANSACTION_SET.value());
        records.put(KeyRecord.TRANSCTION_SUBSET.name(), KeyRecord.TRANSCTION_SUBSET.value());
        records.put(KeyRecord.STATE.name(), editFieldUtil.stateProvince());
        records.put(KeyRecord.COMMODITY.name(), editFieldUtil.commodity());
        records.put(KeyRecord.SUPPLIER_DUNS.name(), editFieldUtil.supplierDuns());
        records.put(KeyRecord.UTILITY_DUNS.name(), editFieldUtil.utilityDuns());
        recordFieldsList.add(records);
    }

    public void populateHeaderRecord() {
        Map<String, String> records = new HashMap<>(2);
        addRecordIdFieldName(HeaderRecord.REC_ID.value(), records);
        records.put(HeaderRecord.REC_ID.name(), HeaderRecord.REC_ID.value());
        records.put(HeaderRecord.SUPPLIER_NAME.name(), editFieldUtil.utilityName());
        records.put(HeaderRecord.UTILITY_NAME.name(), editFieldUtil.utilityName());
        records.put(HeaderRecord.TRANS_REF_NR.name(), generateSequenceNr());
        records.put(HeaderRecord.CREATE_DATE.name(), dateFormat.format(TimezoneHelper.serverCurrentDate()));
        recordFieldsList.add(records);
    }

    public void populateAccountRecord() {
        Map<String, String> records = new HashMap<>(16);
        addRecordIdFieldName(AccountRecord.REC_ID.value(), records);
        records.put(AccountRecord.REC_ID.name(), AccountRecord.REC_ID.value());
        records.put(AccountRecord.SERVICE_REQUESTED.name(), AccountRecord.SERVICE_REQUESTED.value());
        records.put(AccountRecord.LINE_NR.name(), AccountRecord.LINE_NR.value());
        records.put(AccountRecord.BILL_DELIVER.name(), AccountRecord.BILL_DELIVER.value());
        records.put(AccountRecord.BILL_CALC.name(), AccountRecord.BILL_CALC.value());
        records.put(AccountRecord.PERCENT_PARTICPATE.name(), AccountRecord.PERCENT_PARTICPATE.value());
        records.put(AccountRecord.UTILITY_CUST_ACCT_NR.name(), editFieldUtil.customerAccountNr());
        recordFieldsList.add(records);
    }

    public void populateDatesRecord() {
        Map<String, String> records = new HashMap<>(6);
        addRecordIdFieldName(DatesRecord.REC_ID.value(), records);
        records.put(DatesRecord.REC_ID.name(), DatesRecord.REC_ID.value());
        records.put(DatesRecord.EFFECTIVE_DT.name(), effectiveDate == null ? "" : dateFormat.format(effectiveDate));
        records.put(DatesRecord.COMPLETION_DT.name(), completionDate == null ? "" : dateFormat.format(completionDate));
        recordFieldsList.add(records);
    }

    public void populateMeterRecord() {
        Map<String, String> records = new HashMap<>(6);
        addRecordIdFieldName(MeterRecord.REC_ID.value(), records);
        records.put(MeterRecord.REC_ID.name(), MeterRecord.REC_ID.value());
        records.put(MeterRecord.METER_IDENTIFIER.name(), MeterRecord.METER_IDENTIFIER.value());
        records.put(MeterRecord.METER_ACTION.name(), MeterRecord.METER_ACTION.value());
        records.put(MeterRecord.SUPPLIER_RATE_CD.name(), rate.toString());
        recordFieldsList.add(records);
    }

    public void populateCodeRecord() {
        Map<String, String> records = new HashMap<>(6);
        addRecordIdFieldName(CodeRecord.REC_ID.value(), records);
        records.put(CodeRecord.REC_ID.name(), CodeRecord.REC_ID.value());
        records.put(CodeRecord.REC_TYPE.name(), CodeRecord.REC_TYPE.value());
        records.put(CodeRecord.CODE.name(), CodeRecord.CODE.value());
        recordFieldsList.add(records);
    }

    private String generateSequenceNr() {
        int ctr = counter.incrementAndGet();
        return txPref + ctr;
    }

    public EDIFileWS generateFile() {
        String fileName = editFieldUtil.utilityDuns() + FileConstants.UNDERSCORE_SEPARATOR + editFieldUtil.supplierDuns()+ FileConstants.UNDERSCORE_SEPARATOR + generateSequenceNr() + TRANSACTION_SET;
        try {
            MetaFieldValue<Object> editTypeMf = companyDTO.getMetaField(FileConstants.AUTO_RENEWAL_EDI_TYPE_ID_META_FIELD_NAME);
            if(editTypeMf == null) {
                throw new SessionInternalError("Meta field '"+FileConstants.AUTO_RENEWAL_EDI_TYPE_ID_META_FIELD_NAME+"' not defined for company " +companyDTO.getId() );
            }
            int ediTypeId = Integer.valueOf(editTypeMf.getValue().toString());
            fileFormat = FileFormat.getFileFormat(ediTypeId);

            populateKeyRecord();
            populateHeaderRecord();
            populateAccountRecord();
            populateDatesRecord();
            populateMeterRecord();
            populateCodeRecord();

            int generatedFileId = webServicesSessionBean.generateEDIFile(ediTypeId, companyDTO.getId(), fileName, recordFieldsList);
            EDIFileWS generatedFileWS = ediTransactionBean.getEDIFileWS(generatedFileId);
            //binding the utility account number and custoemr id in the Auto Renewal EDI file.
            generatedFileWS.setUtilityAccountNumber(editFieldUtil.customerAccountNr());
            generatedFileWS.setUserId(customerDTO.getBaseUser().getUserId());
            new EDIFileBL().saveEDIFile(generatedFileWS);

            return generatedFileWS;
        } catch (SessionInternalError sie) {
            LOG.debug("Could not create outbound file for Auto Renewal Model Change Request. " + sie.getMessage(), sie);
            throw sie;
        }
    }
}
