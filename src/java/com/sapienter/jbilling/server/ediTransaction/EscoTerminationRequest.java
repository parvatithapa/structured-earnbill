package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.helper.CustomerEnrollmentFileGenerationHelper;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.Field;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileStructure;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.UserWS;
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
 * @author Gerhard Maree
 * @since 05-11-2015
 */
public class EscoTerminationRequest {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EscoTerminationRequest.class));

    private CompanyDTO companyDTO;
    private CustomerDTO customerDTO;
    private Date effectiveDate;
    private String reasonCode;

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
        TRANSCTION_SUBSET("EQ"),
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

    private enum NameRecord {
        REC_ID("NME"),
        NAME_TYPE("8R"),
        NAME(),
        ADDRESS1(),
        ADDRESS2(),
        CITY(),
        STATE(),
        ZIP_CODE(),
        TELEPHONE(),
        CONTACT_NAME();

        private String value;

        NameRecord() {
        }

        NameRecord(String value) {
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
        SERVICE_REQUESTED("HU"),
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
        END_SERVICE_DT(),
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
     * Code record fields
     */
    private enum CodeRecord {
        REC_ID("CDE"),
        REC_TYPE("S"),
        CODE();

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

    public EscoTerminationRequest(CompanyDTO companyDTO, CustomerDTO customerDTO, String reasonCode, Date date) {
        this.companyDTO = companyDTO;
        this.customerDTO = customerDTO;
        this.effectiveDate = date;
        this.reasonCode = reasonCode;
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

    public void populateNameRecord() {
        StringBuilder name = new StringBuilder();

        if(editFieldUtil.lastName() != null) {
            name.append(editFieldUtil.lastName());
            name.append(", ");
        }
        if(editFieldUtil.firstName() != null) {
            name.append(editFieldUtil.firstName());
        }
        Map<String, String> records = new HashMap<>(16);
        addRecordIdFieldName(NameRecord.REC_ID.value(), records);

        records.put(NameRecord.REC_ID.name(), NameRecord.REC_ID.value());
        records.put(NameRecord.NAME_TYPE.name(), NameRecord.NAME_TYPE.value());
        records.put(NameRecord.NAME.name(), name.toString());
        records.put(NameRecord.ADDRESS1.name(), editFieldUtil.address1());
        records.put(NameRecord.ADDRESS2.name(), editFieldUtil.address2());
        records.put(NameRecord.CITY.name(), editFieldUtil.city());
        records.put(NameRecord.STATE.name(), CustomerEnrollmentFileGenerationHelper.USState.getAbbreviationForState(editFieldUtil.stateProvince()));
        records.put(NameRecord.ZIP_CODE.name(), editFieldUtil.postalCode());
        records.put(NameRecord.TELEPHONE.name(), editFieldUtil.phoneNr());
        records.put(NameRecord.CONTACT_NAME.name(), name.toString());
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
        records.put(DatesRecord.END_SERVICE_DT.name(), dateFormat.format(effectiveDate));
        records.put(DatesRecord.COMPLETION_DT.name(), dateFormat.format(effectiveDate));
        recordFieldsList.add(records);
    }

    public void populateCodeRecord() {
        Map<String, String> records = new HashMap<>(6);
        addRecordIdFieldName(CodeRecord.REC_ID.value(), records);
        records.put(CodeRecord.REC_ID.name(), CodeRecord.REC_ID.value());
        records.put(CodeRecord.REC_TYPE.name(), CodeRecord.REC_TYPE.value());
        records.put(CodeRecord.CODE.name(), reasonCode);
        recordFieldsList.add(records);
    }

    private String generateSequenceNr() {
        int ctr = counter.incrementAndGet();
        return txPref + ctr;
    }

    public EDIFileWS generateFile() {
        String fileName = editFieldUtil.utilityDuns() + FileConstants.UNDERSCORE_SEPARATOR + editFieldUtil.supplierDuns()+ FileConstants.UNDERSCORE_SEPARATOR + generateSequenceNr() + TRANSACTION_SET;
        try {
            MetaFieldValue<Object> editTypeMf = companyDTO.getMetaField(FileConstants.ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME);
            if(editTypeMf == null) {
                throw new SessionInternalError("Meta field '"+FileConstants.ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME+"' not defined for company " +companyDTO.getId() );
            }
            int ediTypeId = Integer.valueOf(editTypeMf.getValue().toString());
            fileFormat = FileFormat.getFileFormat(ediTypeId);

            populateKeyRecord();
            populateHeaderRecord();
            populateNameRecord();
            populateAccountRecord();
            populateDatesRecord();
            populateCodeRecord();

            int generatedFileId = webServicesSessionBean.generateEDIFile(ediTypeId, companyDTO.getId(), fileName, recordFieldsList);
            EDIFileWS generatedFileWS = ediTransactionBean.getEDIFileWS(generatedFileId);

            generatedFileWS.setUserId(customerDTO.getBaseUser().getId());
            generatedFileWS.setUtilityAccountNumber(getCustomerUtilityAccountNumber());
            ediTransactionBean.changeEdiFileStatus(FileConstants.READY_TO_SEND_STATUS, generatedFileWS, ediTypeId);
            return generatedFileWS;
        } catch (SessionInternalError sie) {
            LOG.debug("Could not create outbound file for Esco Termination Request. " + sie.getMessage());
            throw sie;
        }
    }


    public String getCustomerUtilityAccountNumber(){
        UserWS userWS=webServicesSessionBean.getUserWS(customerDTO.getBaseUser().getId());
        Optional<MetaFieldValueWS> metaFieldValueWSOptional=Arrays.asList(userWS.getMetaFields()).stream().filter((MetaFieldValueWS metaFieldValueWS)-> metaFieldValueWS.getFieldName().equals(FileConstants.CUSTOMER_ACCOUNT_KEY)).findFirst();
        if(!metaFieldValueWSOptional.isPresent()){
            throw new SessionInternalError("No UTILITY_CUST_ACCT_NR found for the customer "+customerDTO.getBaseUser().getUserName());
        }
       return  (String)metaFieldValueWSOptional.get().getValue();
    }
}
