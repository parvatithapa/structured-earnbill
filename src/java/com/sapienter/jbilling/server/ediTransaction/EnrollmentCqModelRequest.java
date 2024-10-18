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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class is used to send 814 Enrollment CQ Model Change to the LDC.
 *
 * @author Gerhard Maree
 * @since 05-11-2015
 */
public class EnrollmentCqModelRequest {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EnrollmentCqModelRequest.class));

    public static enum BillModel {
        RATE_READY("LDC"), BILL_READY("ESP");

        BillModel(String billCalc) {
            this.billCalc = billCalc;
        }

        private String billCalc;

        public String billCalc() {
            return billCalc;
        }
    }

    private CompanyDTO companyDTO;
    private CustomerDTO customerDTO;
    private Date effectiveDate;
    private Date completionDate;
    private BillModel rateModel;
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

    private static final String TRANSACTION_SET = "t814Esco";

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
        SERVICE_REQUESTED2(""),
        CUST_LIFE_SUPPORT("N"),
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
        METER_CYCLE();

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
    private enum CodeRecord1 {
        REC_ID("CDE"),
        REC_TYPE("C"),
        CODE("REFPC");

        private String value;

        CodeRecord1() {
        }

        CodeRecord1(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Code record fields
     */
    private enum CodeRecord2 {
        REC_ID("CDE"),
        REC_TYPE("C"),
        CODE("REFBLT");

        private String value;

        CodeRecord2() {
        }

        CodeRecord2(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public EnrollmentCqModelRequest(CompanyDTO companyDTO, CustomerDTO customerDTO, BillModel rateModel, Date effectiveDate, Date completionDate) {
        assert companyDTO != null;
        assert customerDTO != null;
        assert rateModel != null;
        assert effectiveDate != null;
        assert completionDate != null;

        this.companyDTO = companyDTO;
        this.customerDTO = customerDTO;
        this.effectiveDate = effectiveDate;
        this.completionDate = completionDate;
        this.rateModel = rateModel;
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
        records.put(AccountRecord.BILL_DELIVER.name(), rateModel.billCalc);
        records.put(AccountRecord.BILL_CALC.name(), AccountRecord.BILL_CALC.value());
        records.put(AccountRecord.PERCENT_PARTICPATE.name(), AccountRecord.PERCENT_PARTICPATE.value());
        records.put(AccountRecord.UTILITY_CUST_ACCT_NR.name(), editFieldUtil.customerAccountNr());
        records.put(AccountRecord.SERVICE_REQUESTED2.name(), "");
        records.put(AccountRecord.CUST_LIFE_SUPPORT.name(), "N");
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
        records.put(MeterRecord.METER_CYCLE.name(), editFieldUtil.meterCycle());
        recordFieldsList.add(records);
    }

    public void populateCodeRecord1() {
        Map<String, String> records = new HashMap<>(6);
        addRecordIdFieldName(CodeRecord1.REC_ID.value(), records);
        records.put(CodeRecord1.REC_ID.name(), CodeRecord1.REC_ID.value());
        records.put(CodeRecord1.REC_TYPE.name(), CodeRecord1.REC_TYPE.value());
        records.put(CodeRecord1.CODE.name(), CodeRecord1.CODE.value());
        recordFieldsList.add(records);
    }

    public void populateCodeRecord2() {
        Map<String, String> records = new HashMap<>(6);
        addRecordIdFieldName(CodeRecord1.REC_ID.value(), records);
        records.put(CodeRecord2.REC_ID.name(), CodeRecord2.REC_ID.value());
        records.put(CodeRecord2.REC_TYPE.name(), CodeRecord2.REC_TYPE.value());
        records.put(CodeRecord2.CODE.name(), CodeRecord2.CODE.value());
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
            populateCodeRecord1();
            populateCodeRecord2();

            int generatedFileId = webServicesSessionBean.generateEDIFile(ediTypeId, companyDTO.getId(), fileName, recordFieldsList);
            EDIFileWS generatedFileWS = ediTransactionBean.getEDIFileWS(generatedFileId);
            return generatedFileWS;
        } catch (SessionInternalError sie) {
            LOG.debug("Could not create outbound file for Auto Renewal Model Change Request. " + sie.getMessage(), sie);
            throw sie;
        }
    }
}
