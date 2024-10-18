package com.sapienter.jbilling.server.customerEnrollment.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDAS;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.customerEnrollment.event.EnrollmentCompletionEvent;
import com.sapienter.jbilling.server.customerEnrollment.helper.CustomerEnrollmentFileGenerationHelper;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDAS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.fileGenerator.FlatFileGenerator;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.Field;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.Record;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.RecordStructure;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vivek on 8/9/15.
 */
public class CustomerEnrollmentFileGenerationTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CustomerEnrollmentFileGenerationTask.class));
    /*Constant Values*/

    public static enum RecordValues {
        /*Key Record*/
        TRANSACTION_SET("814"),
        TRANSCTION_SUBSET("EQ"),

        /*Account Record*/
        LINE_NR("1"),
        SERVICE_REQUESTED("CE"),
        SERVICE_REQUESTED2("HU"),
        PERCENT_PARTICPATE("1"),

        /*Date Record*/
        CUST_ENROLL_AGREE_TIME("000000"),
        CUST_TIMEZONE("ET"),


        /*Meter Record*/
        METER_TYPE("METER_TYPE"),
        METER_IDENTIFIER("ALL"),
        SUPPLIER_RATE_CD("SUPPLIER_RATE_CD"), // Need to confirm from Yves that Which rate will be use here.
        METER_ACTION("Q");


        Object val;

        RecordValues(Object value) {
            val = value;
        }

        Object getValue() {
            return val;
        }
    }

    public static enum EnrollmentFileGenerationConstants {
        DIVISION("DIVISION"),
        PLAN("PLAN"),
        BLACKLISTED("Blacklisted"),
        METER_TYPE("METER_TYPE"),
        COMMODITY("COMMODITY"),
        INTERVAL("Interval"),
        NON_INTERVAL("Non Interval"),
        UNKNOWN("Unknown"),
        COMMODITY_PRICE("COMMODITY_PRICE"), // used only for rate ready customer
        FIXED_CHG("FIXED_CHG"), // used only for rate ready customer
        SERVICE_REQUESTED2("SERVICE_REQUESTED2");

        String val;

        EnrollmentFileGenerationConstants(String value) {
            val = value;
        }

        String getValue() {
            return val;
        }

    }


    enum TimeZone {
        CENTRAL_TIME("Central Time", "CT"),
        EASTERN_TIME("Eastern Time", "ET"),
        MOUNTAIN_TIME("Mountain Time", "MT"),
        PACIFIC_TIME("Pacific Time", "PT");

        String val;
        String key;

        TimeZone(String key, String value) {
            this.val = value;
            this.key = key;
        }

        String getValue() {
            return val;
        }

        public String getKey() {
            return key;
        }

        public static String getEnum(String key) {
            for(TimeZone t : values())
                if(t.getKey().equalsIgnoreCase(key)) return t.getValue();
            return "";
        }
    }

    public static final String NAME_TYPE = "NAME_TYPE";
    public static final String[] NAME_TYPE_ARRAY = new String[]{"8R","N1","BT"};
    public static final String NAME_TYPE_1 = "8R";
    public static final String NAME_TYPE_2 = "N1";
    public static final String TRANS_REF_NR = "TRANS_REF_NR";
    public static final String CREATE_DATE = "CREATE_DATE";
    public static final String CONTACT_NAME = "CONTACT_NAME";
    public static final String NAME = "NAME";
    public static final String DATE_FORMAT = "MM/dd/yyyy";
    public static final String HISTORICAL_USAGE= "HU";
    public static final String INTERVAL_HISTORICAL_USAGE= "HI";
    public static final String OFF_CYCLE_READ= "SW";


    public String FILE_NAME = "generated_file_" + System.currentTimeMillis();
    private String randomNumber = "" + System.currentTimeMillis();
    private static final String UNDERSCORE_SEPARATOR = "_";
    private static final String DOT_SEPARATOR = ".";

    public Integer CUSTOMER_ENROLLMENT_ID;
    private static EDIFileStatusDTO PROCESSING =  new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_PROCESSING);
    private static EDIFileStatusDTO PROCESSED = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_PROCESSED);
    private static EDIFileStatusDTO ERROR_DETECTED = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_ERROR_DETECTED);

       public static final ParameterDescription EDI_STATUS =
            new ParameterDescription("edi-status", true, ParameterDescription.Type.STR);

    IWebServicesSessionBean webServicesSessionSpringBean;
    IOrderSessionBean orderSessionBean;
    IEDITransactionBean ediTransactionBean;
    CustomerEnrollmentDTO enrollmentDTO;
    AccountInformationTypeDAS accountInformationTypeDAS;

    protected Map<String, Object> companyMetaFieldValueMap = null;
    private Integer EDI_TYPE_ID;
    private String SUPPLIER_DUNS;
    private String UTILITY_DUNS;
    private String SUPPLIER_NAME;
    private String UTILITY_NAME;
    private String BILL_CALC;
    private String BILL_DELIVER;
    private PlanDTO planDTO;
    public static final List<String> states=Arrays.asList("New York");

    private static final Class<Event> events[] = new Class[]{
            EnrollmentCompletionEvent.class
    };

    //initializer for pluggable params
    {
        descriptions.add(EDI_STATUS);
    }


    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        accountInformationTypeDAS = new AccountInformationTypeDAS();

        if (event instanceof EnrollmentCompletionEvent) {

//            Get metafield values from customer metafield in companyMetaFieldValueMap
            setMetaFieldValues();
//            Set values in local variable.
            setCommonParameter();

            enrollmentDTO = new CustomerEnrollmentDAS().find(((EnrollmentCompletionEvent) event).getEnrollmentId());
            CUSTOMER_ENROLLMENT_ID = ((EnrollmentCompletionEvent) event).getEnrollmentId();

            LOG.debug("finding Plan ");
            MetaFieldValue planMetaFieldValue = enrollmentDTO.getMetaField(EnrollmentFileGenerationConstants.PLAN.getValue(), null);
            ItemDTO item = new ItemDAS().findItemByInternalNumber((String) planMetaFieldValue.getValue(), enrollmentDTO.getCompany().getId());
            if (item == null) {
                throw new SessionInternalError("Plan not found", new String[]{"Plan not found for code " + planMetaFieldValue.getValue()});
            }
            planDTO = new PlanDAS().findPlanByItemId(item.getId());

            //is black listed customer
            isBlackListed(enrollmentDTO);
            //Validating plan exit in the system and if exit then set default value for division
            bindDefaultValueOfDivision(enrollmentDTO);

            Map<String, String> metaFieldMap = fetchMapForMetaField(enrollmentDTO.getMetaFields());
            MetaFieldValue metaFieldValue=enrollmentDTO.getMetaField(EnrollmentFileGenerationConstants.METER_TYPE.getValue());
            String meterType=(String)metaFieldValue.getValue();
            String SERVICE_REQUESTED2="";
            switch (meterType){
                case "Interval":
                    SERVICE_REQUESTED2=INTERVAL_HISTORICAL_USAGE;
                    break;
                case "Non Interval":
                    SERVICE_REQUESTED2=HISTORICAL_USAGE;
                    break;
                case "Unknown":
                    MetaFieldValue metaFieldVal=enrollmentDTO.getCompany().getMetaField(EnrollmentFileGenerationConstants.SERVICE_REQUESTED2.getValue());
                    MetaFieldValue meterTypeVal=enrollmentDTO.getCompany().getMetaField(EnrollmentFileGenerationConstants.METER_TYPE.getValue());
                    enrollmentDTO.setMetaField(metaFieldValue.getField(), meterTypeVal.getValue());
                    SERVICE_REQUESTED2=(String)metaFieldVal.getValue();
            }

            metaFieldMap.put("SERVICE_REQUESTED2", SERVICE_REQUESTED2);
            if(metaFieldMap.get(FileConstants.ACTUAL_START_DATE) != null && metaFieldMap.get(FileConstants.DURATION) != null) {
                try {
                    LOG.debug("Try to put COMPLETION_DT in map");
                    Calendar completionDate = Calendar.getInstance();
                    completionDate.setTime(new SimpleDateFormat(DATE_FORMAT).parse(metaFieldMap.get(FileConstants.ACTUAL_START_DATE)));
                    completionDate.add(Calendar.MONTH, Integer.parseInt(metaFieldMap.get(FileConstants.DURATION)));
                    metaFieldMap.put(FileConstants.COMPLETION_DT,convertDateToString(completionDate.getTime()));
                    LOG.debug("After putting COMPLETION_DT value is:  " + metaFieldMap.get(FileConstants.COMPLETION_DT));
                } catch (ParseException pe) {
                    LOG.error("Unable to parse completion date " + pe);
                    pe.printStackTrace();
                }

            }
            mapMetaFieldWithRecord(metaFieldMap, event.getEntityId());

        }
    }

    private String getTransactionNumber() {
        return randomNumber + CUSTOMER_ENROLLMENT_ID.toString();
    }

    private void mapMetaFieldWithRecord(Map<String, String> metaFieldMap, Integer entityId) {
        Integer ediTypeId = EDI_TYPE_ID;
        FileFormat fileFormat = FileFormat.getFileFormat(ediTypeId);
        List<Map<String, String>> recordMapList = new ArrayList<Map<String, String>>();
        for (RecordStructure recordStructure : fileFormat.getFileStructure().getRecordStructures()) {

            matchRecords(recordStructure, metaFieldMap, recordMapList);
        }
        LOG.debug("Record map list is:  " + recordMapList);

//        Create file naming convention using this link. https://docs.google.com/document/d/14D0T2jQeMajCD3xnWu2_2cYGy6gEHL6q7mRIzrgxACA/edit
        FILE_NAME = UTILITY_DUNS + UNDERSCORE_SEPARATOR + SUPPLIER_DUNS + UNDERSCORE_SEPARATOR + getTransactionNumber() + DOT_SEPARATOR + RecordValues.TRANSACTION_SET.getValue();
        FlatFileGenerator generator = new FlatFileGenerator(fileFormat, entityId, FILE_NAME, recordMapList);
        try {
            EDIFileDTO ediFileDTO = generator.validateAndSaveInput();
            EDIFileWS fileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());

            MetaFieldValue<String> utilityAccountNumber=enrollmentDTO.getMetaField(FileConstants.CUSTOMER_ACCOUNT_KEY);
            LOG.debug("Utility Account Number : "+utilityAccountNumber);
            if(utilityAccountNumber!=null){
                fileWS.setUtilityAccountNumber(utilityAccountNumber.getValue());
            }

            EDIFileStatusWS processedFileWS = webServicesSessionSpringBean.findEdiStatusById(FileConstants.EDI_STATUS_PROCESSED);
            LOG.debug("fileWS.getEdiFileStatusWS().getName()  " + fileWS.getEdiFileStatusWS().getName());
            if(fileWS.getEdiFileStatusWS().getName().equalsIgnoreCase(processedFileWS.getName())) {
                String ediStatus = parameters.get(EDI_STATUS.getName());
                LOG.debug("In CustomerEnrollmentFileGenerationTask ediStatus is:  " + ediStatus);
                LOG.debug("In StringUtils.trimToNull(ediStatus) is:  " + StringUtils.trimToNull(ediStatus));

                EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
                EDIFileStatusWS statusWS = null;
                for (EDIFileStatusWS ediFileStatus : ediType.getEdiStatuses()) {
                    if (ediFileStatus.getName().equals(ediStatus)) {
                        statusWS = ediFileStatus;
                    }
                }

                if(statusWS == null) {
                    throw new SessionInternalError("EDI file status can not be null");
                }

                fileWS.setEdiFileStatusWS(statusWS);

                Integer editFileId = webServicesSessionSpringBean.saveEDIFileRecord(fileWS);
                ediFileDTO = new EDIFileDAS().findNow(editFileId);
                LOG.debug("After changing ediFileDTO.getFileStatus() " + ediFileDTO.getFileStatus());
            }

        } catch (Exception ex) {
            LOG.error("Caught Exception While generating file " + ex);
            ex.printStackTrace();
        }
    }

    private List<Map<String, String>> matchRecords(RecordStructure recordStructure, Map<String, String> metaFieldMap, List<Map<String, String>> recordMapList) {

        Record record = recordStructure.getRecord();
        LOG.debug("record.getRecId().getDefaultValue()  " + record.getRecId().getDefaultValue());
        Integer loop = Integer.parseInt(recordStructure.getLoop());
        for (int i = 0; i < loop; i++) {
            String recId = record.getRecId().getDefaultValue();
            switch (recId) {
                case FileConstants.KEY:
                    System.out.println(recId);
                    recordMapList.add(setKeyRecord(record, metaFieldMap));
                    break;
                case FileConstants.HDR:
                    System.out.println(recId);
                    recordMapList.add(setHeaderRecord(record, metaFieldMap));
                    break;
                case FileConstants.NME:
                    System.out.println(recId);
                    recordMapList.add(setNameRecord(record, metaFieldMap, i));
                    break;
                case FileConstants.ACT:
                    System.out.println(recId);
                    recordMapList.add(setAccountRecord(record, metaFieldMap));
                    break;
                case FileConstants.DTE:
                    System.out.println(recId);
                    recordMapList.add(setDateRecord(record, metaFieldMap));
                    break;
                case FileConstants.MTR:
                    System.out.println(recId);
                    recordMapList.add(setMeterRecord(record, metaFieldMap));
                    break;
            }
            if (recordStructure.getChildRecord() != null && recordStructure.getChildRecord().size() > 0) {
                for (RecordStructure childRecordStructure : recordStructure.getChildRecord()) {
                    matchRecords(childRecordStructure, metaFieldMap, recordMapList);
                }
            }
        }
        return recordMapList;
    }

    private Map<String, String> setRecordFieldInMetaField(Record record, Map<String, String> metaFieldMap, Map<String, String> map) {
        map.put(record.getRecId().getFieldName(), record.getRecId().getDefaultValue());
        for (Field field : record.getFields()) {
            if(StringUtils.isNotEmpty(field.getDateFormat())) {
                map.put(field.getFieldName(), convertStringToDate(metaFieldMap.get(field.getFieldName()),field.getDateFormat()));
            } else {
                map.put(field.getFieldName(), metaFieldMap.get(field.getFieldName()));
            }

        }
        return map;
    }

    private Map<String, String> setKeyRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> keyMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        keyMap.put(FileConstants.SUPPLIER_DUNS_META_FIELD_NAME, SUPPLIER_DUNS);
        keyMap.put(FileConstants.UTILITY_DUNS_META_FIELD_NAME, UTILITY_DUNS);
        //todo : Can not make constant for Transaction_set. Now it is stored in Database
        keyMap.put(RecordValues.TRANSACTION_SET.name(),  (String) RecordValues.TRANSACTION_SET.getValue());
        keyMap.put(RecordValues.TRANSCTION_SUBSET.name(),(String) RecordValues.TRANSCTION_SUBSET.getValue());

        return keyMap;
    }

    private Map<String, String> setHeaderRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> headerMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        headerMap.put(TRANS_REF_NR, getTransactionNumber());
        headerMap.put(FileConstants.UTILITY_NAME_META_FIELD_NAME, UTILITY_NAME);
        headerMap.put(FileConstants.SUPPLIER_NAME_META_FIELD_NAME, SUPPLIER_NAME);
        headerMap.put(CREATE_DATE, convertStringToDate(convertDateToString(TimezoneHelper.serverCurrentDate()), getDateFormatFromRecord(record, CREATE_DATE)));
        return headerMap;
    }

    private Map<String, String> setNameRecord(Record record, Map<String, String> metaFieldMap, Integer loop) {
        LOG.debug("Loop is:  " + loop);
        Map<String, String> nameMap;
        AccountInformationTypeDTO accountInformationTypeDTO = null;
        AccountTypeDTO accountTypeDTO = enrollmentDTO.getAccountType();
        if (accountTypeDTO.getDescription().equals(FileConstants.RESIDENTIAL_ACCOUNT_TYPE)) {
            accountInformationTypeDTO = accountInformationTypeDAS.findByName(FileConstants.CUSTOMER_INFORMATION_AIT, getEntityId(), accountTypeDTO.getId());
        } else if (accountTypeDTO.getDescription().equals(FileConstants.COMMERCIAL_ACCOUNT_TYPE)) {
            if(NAME_TYPE_ARRAY[loop].equals(NAME_TYPE_1) || NAME_TYPE_ARRAY[loop].equals(NAME_TYPE_2)) {
            accountInformationTypeDTO = accountInformationTypeDAS.findByName(FileConstants.BUSINESS_INFORMATION_AIT, getEntityId(), accountTypeDTO.getId());
            } else {
                accountInformationTypeDTO = accountInformationTypeDAS.findByName(FileConstants.CONTACT_INFORMATION_AIT, getEntityId(), accountTypeDTO.getId());
            }
        }
        Map<String, String> aitMetaFields = getMetaFieldValuesFromAIT(enrollmentDTO.getMetaFields(), accountInformationTypeDTO);
        nameMap = setRecordFieldInMetaField(record, aitMetaFields, new HashMap<String, String>());
        nameMap.put(NAME_TYPE, NAME_TYPE_ARRAY[loop]);
        nameMap.put(CONTACT_NAME,nameMap.get(NAME));
        LOG.debug("For loop number: " + loop + " name map is:  " + nameMap);

        return nameMap;
    }

    private Map<String, String> setAccountRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> accountMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        accountMap.put(FileConstants.BILL_DELIVER_META_FIELD_NAME, BILL_DELIVER);
        accountMap.put(FileConstants.BILL_CALC_META_FIELD_NAME, BILL_CALC);
        accountMap.put(RecordValues.PERCENT_PARTICPATE.name(),(String) RecordValues.PERCENT_PARTICPATE.getValue());
        accountMap.put(RecordValues.SERVICE_REQUESTED.name(),(String) RecordValues.SERVICE_REQUESTED.getValue());
        accountMap.put(RecordValues.LINE_NR.name(),(String) RecordValues.LINE_NR.getValue());

        //adding supplier account number on the enrollment outbound file if enrollment is for the new york state.
        if(enrollmentDTO.getMetaField(FileConstants.STATE) !=null && enrollmentDTO.getMetaField(FileConstants.STATE).getValue()!=null && states.contains(enrollmentDTO.getMetaField(FileConstants.STATE).getValue()+"")){
            MetaFieldValue supplierAccountNumber=enrollmentDTO.getCompany().getMetaField(FileConstants.COMPANY_SUPPLIER_ACCOUNT_NUMBER_META_FIELD_NAME);
            if(supplierAccountNumber!=null && supplierAccountNumber.getValue()!=null){
                accountMap.put(FileConstants.COMPANY_SUPPLIER_ACCOUNT_NUMBER_META_FIELD_NAME, (String)supplierAccountNumber.getValue());
            }

            String billingModal=ediTransactionBean.getBillingModelType(enrollmentDTO);
            if(billingModal.equals(FileConstants.BILLING_MODEL_RATE_READY)){
                String rate=ediTransactionBean.calculateRate(enrollmentDTO, false);
                accountMap.put(EnrollmentFileGenerationConstants.COMMODITY_PRICE.getValue(), rate);
                //TODO need to confirm FIXED_CHG value
                accountMap.put(EnrollmentFileGenerationConstants.FIXED_CHG.getValue(), calculateFixedCharges());
            }
        }

        return accountMap;
    }

    private Map<String, String> setDateRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> dateMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        //add CUST_ENROLL_AGREE_DT date manually because CUST_ENROLL_AGREE_DT metafield name replace by ACTUAL_START_DATE
        Field fields = record.getFields().stream().filter(field -> field != null && FileConstants.CUST_ENROLL_AGREE_DT.equals(field.getFieldName())).findFirst().orElse(null);
        if (fields != null) {
            dateMap.put(FileConstants.CUST_ENROLL_AGREE_DT, convertStringToDate(metaFieldMap.get(FileConstants.ACTUAL_START_DATE), fields.getDateFormat()));
        } else {
            LOG.error("CUST_ENROLL_AGREE_DT not found in fields record");
            throw new SessionInternalError("CUST_ENROLL_AGREE_DT not found in fields record");
        }
        dateMap.put(RecordValues.CUST_ENROLL_AGREE_TIME.name(),(String) RecordValues.CUST_ENROLL_AGREE_TIME.getValue());
        dateMap.put(RecordValues.CUST_TIMEZONE.name(),TimeZone.getEnum(dateMap.get(RecordValues.CUST_TIMEZONE.name())));
        return dateMap;
    }

    private Map<String, String> setMeterRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> dateMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        dateMap.put(RecordValues.METER_IDENTIFIER.name(), (String) RecordValues.METER_IDENTIFIER.getValue());
        MetaFieldValue metaFieldValue=enrollmentDTO.getMetaField(EnrollmentFileGenerationConstants.METER_TYPE.getValue());
        String meterType=(String)metaFieldValue.getValue();
        dateMap.put(RecordValues.METER_TYPE.name(), meterType);
        dateMap.put(RecordValues.METER_ACTION.name(), (String) RecordValues.METER_ACTION.getValue());

        MetaFieldValue commodityMetaFieldValue=enrollmentDTO.getMetaField(EnrollmentFileGenerationConstants.COMMODITY.getValue());
        String productCode=commodityMetaFieldValue.getValue().toString();
        ItemDTO itemDTO=new ItemDAS().findItemByInternalNumber(productCode, getEntityId());
        if(itemDTO==null){
            throw new SessionInternalError("Configuration issue", new String[]{"No item find for product code : "+productCode});
        }

        throwExceptionIsPlanOrMetaFieldNull(planDTO);
        //Send rate for only rate ready customer
        if(FileConstants.BILLING_MODEL_RATE_READY.equals(planDTO.getMetaField(FileConstants.BILLING_MODEL).getValue())){
            String rate=ediTransactionBean.calculateRate(enrollmentDTO, true);
            dateMap.put(RecordValues.SUPPLIER_RATE_CD.name(), rate);
        }
        return dateMap;
    }

    private Map<String, String> setCodeRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> codeMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        return codeMap;
    }

    Map<String, String> fetchMapForMetaField(List<MetaFieldValue> metaFieldValues) {
        Map<String, String> map = new HashMap<String, String>();
        for (MetaFieldValue metaFieldValue : metaFieldValues) {
            if (metaFieldValue.getValue() != null) {
                if (metaFieldValue.getField().getName().equals(FileConstants.COMMODITY)) {
                    String productCode=metaFieldValue.getValue().toString();
                    String commodityCode=getCommodityCode(productCode);
                    if(commodityCode==null){
                        throw new SessionInternalError("No Item found for  product code "+productCode);
                    }
                    map.put(metaFieldValue.getField().getName(), commodityCode);
                } else if(metaFieldValue.getField().getName().equals(FileConstants.STATE)) {
                    map.put(metaFieldValue.getField().getName(), CustomerEnrollmentFileGenerationHelper.USState.getAbbreviationForState(metaFieldValue.getValue().toString()));
                } else {
                    if(metaFieldValue.getField().getDataType().equals(DataType.DATE)) {
                        map.put(metaFieldValue.getField().getName(), convertDateToString((Date) metaFieldValue.getValue()));
                    } else {
                        map.put(metaFieldValue.getField().getName(), metaFieldValue.getValue().toString().toUpperCase());
                    }
                }
            } else if (metaFieldValue.getField().getDataType().equals(DataType.BOOLEAN)) {
                map.put(metaFieldValue.getField().getName(), convertBooleanFValue((Boolean) metaFieldValue.getValue()));
            }
        }
        return map;
    }

    private String getCommodityCode(String internalNumber) {
        return ediTransactionBean.getCommodityCode(internalNumber, getEntityId());
    }

    private Map<String, String> getMetaFieldValuesFromAIT(List<MetaFieldValue> metaFieldValues, AccountInformationTypeDTO ait) {
        Map<String, String> map = new HashMap<String, String>();
        if (ait != null) {
            for (MetaFieldValue metaFieldValue : metaFieldValues) {
                MetaFieldGroup group = metaFieldValue.getField().getMetaFieldGroups().size() > 0 ? (MetaFieldGroup) metaFieldValue.getField().getMetaFieldGroups().toArray()[0] : null;
                MetaField metaField = metaFieldValue.getField();
                if (group != null && metaField.getFieldUsage() != null && group.getId() == ait.getId()) {
                    if (metaFieldValue.getValue() != null) {
                        map.put(metaField.getName(), metaFieldValue.getValue().toString().toUpperCase());
                    }
                }
            }
        }
        return map;
    }

    private String convertStringToDate(String date, String format) {
        String finalDateInString = "";
        if (StringUtils.isNotEmpty(date) && format != null) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                Date date1 = formatter.parse(date);
                SimpleDateFormat convertedFormatter = new SimpleDateFormat(format.replaceAll("C", "Y").replaceAll("D", "d").replaceAll("Y", "y"));
//            convert year in YYYY instead of CCYY from given format as java does not support CCYY format. D should be d for date format in java.
                finalDateInString = convertedFormatter.format(date1);
            } catch (ParseException ex) {
                LOG.error("caught exception in throwing in converting date");
                throw new SessionInternalError(ex);
            }
        }

        return finalDateInString;

    }
    private String convertDateToString(Date date) {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        return df.format(date);
    }

    private String convertBooleanFValue(Boolean val) {
        return (val!=null && val) ? "Y" : "N";
    }

    private String getDateFormatFromRecord(Record record, String fieldName) {
        String format = null;
        LOG.debug("record key: " + record.getRecId().getDefaultValue() + " field name:  " + fieldName);
        for (Field field: record.getFields()) {
            LOG.debug("field.getFieldName()   " + field.getFieldName() + " field.getDateFormat()  " + field.getDateFormat());
            LOG.debug("StringUtils.trimToNull(field.getDateFormat())  " + StringUtils.trimToNull(field.getDateFormat()));
            if(!field.isNotUsed() && field.getFieldName().equals(fieldName) && StringUtils.trimToNull(field.getDateFormat()) != null) {
                format = StringUtils.trimToNull(field.getDateFormat());
            }
        }
        LOG.debug("returned format is:  " + format);
        return format;
    }

    private void isBlackListed(CustomerEnrollmentDTO customerEnrollmentDTO){
        UserDTO user=customerEnrollmentDTO.getParentCustomer();

        if(user!=null){
            MetaFieldValue blackListedMetaFieldValue=user.getCustomer().getMetaField(EnrollmentFileGenerationConstants.BLACKLISTED.getValue(), null);
            Boolean isBlackListed=false;
            if(blackListedMetaFieldValue!=null){
                isBlackListed=blackListedMetaFieldValue.getValue()!=null?(Boolean)blackListedMetaFieldValue.getValue():false;
            }

            if(isBlackListed){
                throw new SessionInternalError("Blacklisted customer", new String[]{"Customer "+user.getUserName()+" is blacklisted. You can not create enrollment for him"});
            }
        }

    }
    private void bindDefaultValueOfDivision(CustomerEnrollmentDTO customerEnrollmentDTO) {
        MetaFieldValue divisionMetaFieldValue = customerEnrollmentDTO.getMetaField(EnrollmentFileGenerationConstants.DIVISION.getValue(), null);
        if (divisionMetaFieldValue != null && divisionMetaFieldValue.getValue() == null) {
            if (planDTO != null) {
                MetaFieldValue metaFieldValue = planDTO.getMetaField(EnrollmentFileGenerationConstants.DIVISION.getValue());
                if (metaFieldValue == null) {
                    throw new SessionInternalError("In Plan " + EnrollmentFileGenerationConstants.DIVISION.getValue() + "value not found");
                }
                customerEnrollmentDTO.setMetaField(divisionMetaFieldValue.getField(), metaFieldValue.getValue());
            }
        }
    }

    protected void setMetaFieldValues() {

        LOG.debug("getEntityId() is: 1111" + getEntityId());
        CompanyWS companyWS = ediTransactionBean.getCompanyWS(getEntityId());
        MetaFieldValueWS[] metaFieldValues = companyWS.getMetaFields();
        LOG.debug("metaFieldValues  are: 333333333  " + Arrays.asList(metaFieldValues));
        companyMetaFieldValueMap = new HashMap<String, Object>();
        for (MetaFieldValueWS metaFieldValueWS : metaFieldValues) {
            companyMetaFieldValueMap.put(metaFieldValueWS.getFieldName(), metaFieldValueWS.getValue());
        }
    }

    public void setCommonParameter() {
        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME);
        SUPPLIER_DUNS = (String) companyMetaFieldValueMap.get(FileConstants.SUPPLIER_DUNS_META_FIELD_NAME);
        UTILITY_DUNS = (String) companyMetaFieldValueMap.get(FileConstants.UTILITY_DUNS_META_FIELD_NAME);

        isValueNull("EDI_TYPE_ID", EDI_TYPE_ID);
        isValueNull("SUPPLIER_DUNS", SUPPLIER_DUNS);
        isValueNull("UTILITY_DUNS", UTILITY_DUNS);

        SUPPLIER_NAME = (String) companyMetaFieldValueMap.get(FileConstants.SUPPLIER_NAME_META_FIELD_NAME);
        UTILITY_NAME = (String) companyMetaFieldValueMap.get(FileConstants.UTILITY_NAME_META_FIELD_NAME);
        BILL_CALC = (String) companyMetaFieldValueMap.get(FileConstants.BILL_CALC_META_FIELD_NAME);
        BILL_DELIVER = (String) companyMetaFieldValueMap.get(FileConstants.BILL_DELIVER_META_FIELD_NAME);


        LOG.debug("EDI_TYPE_ID  is: " + EDI_TYPE_ID);
        LOG.debug("SUPPLIER_DUNS  : " + SUPPLIER_DUNS);
        LOG.debug("UTILITY_DUNS  id is: " + UTILITY_DUNS);
    }

    private void isValueNull(String key, Object value) {
        if (value == null) {
            throw new SessionInternalError("" + key + " can not be null");
        }
    }

    private void throwExceptionIsPlanOrMetaFieldNull(PlanDTO planDTO) {
        if (planDTO == null) {
            LOG.error("Plan not found");
            throw new SessionInternalError("Configuration issue", new String[]{"Plan not found"});
        } else if (planDTO.getMetaFields() == null) {
            LOG.error("Meta field not found at this plan level");
            throw new SessionInternalError("Configuration issue", new String[]{"Meta field not found at plan level"});
        }
    }

    private String calculateFixedCharges(){
        return "0.0";
    }

}
