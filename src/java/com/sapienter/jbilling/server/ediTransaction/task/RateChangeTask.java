package com.sapienter.jbilling.server.ediTransaction.task;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.helper.CustomerEnrollmentFileGenerationHelper;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
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
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by neeraj on 18/11/15.
 */
public class RateChangeTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RateChangeTask.class));


    enum RecordValues {
        /*Key Record*/
        TRANSACTION_SET("814"),
        TRANSCTION_SUBSET("CS"),

        /*Account Record*/
        LINE_NR("1"),
        SERVICE_REQUESTED("CE"),
        SERVICE_REQUESTED2("HU"),
        PERCENT_PARTICPATE("1"),

        /*Date Record*/
        CUST_ENROLL_AGREE_TIME("000000"),
        CUST_TIMEZONE("ET"),

        /*Meter Record*/
        METER_IDENTIFIER("ALL"),
        SUPPLIER_RATE_CD("SUPPLIER_RATE_CD"),
        METER_ACTION("Q");

        Object val;

        RecordValues(Object value) {
            val = value;
        }

        Object getValue() {
            return val;
        }
    }

    public static enum ChangeRequestGenerationConstants {
        DIVISION("DIVISION"),
        PLAN("PLAN"),
        BLACKLISTED("Blacklisted"),
        METER_TYPE("METER_TYPE"),
        INTERVAL("Interval"),
        NON_INTERVAL("Non Interval"),
        UNKNOWN("Unknown"),
        RATE_CHANGE_DATE("Rate Change Date"),
        BILLING_MODEL("Billing Model"),
        RATE_READY("Rate Ready"),
        CYCLE_NUMBER("CYCLE_NUMBER"),
        SERVICE_REQUESTED2("SERVICE_REQUESTED2");

        String val;

        ChangeRequestGenerationConstants(String value) {
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

    protected Map<String, Object> companyMetaFieldValueMap = null;
    private Integer EDI_TYPE_ID;
    private String SUPPLIER_DUNS;
    private String UTILITY_DUNS;
    private String SUPPLIER_NAME;
    private String UTILITY_NAME;
    private String BILL_CALC;
    private String BILL_DELIVER;
    public String FILE_NAME = "generated_file_" + System.currentTimeMillis();
    private String randomNumber = "" + System.currentTimeMillis();
    private static final String UNDERSCORE_SEPARATOR = "_";
    private static final String DOT_SEPARATOR = ".";
    public static final String NAME_TYPE = "NAME_TYPE";
    public static final String TRANS_REF_NR = "TRANS_REF_NR";
    public static final String CREATE_DATE = "CREATE_DATE";
    public static final String CONTACT_NAME = "CONTACT_NAME";
    public static final String NAME = "NAME";
    public static final String DATE_FORMAT = "MM/dd/yyyy";
    public static final String CDERecordDefaultCount = "3";
    protected String status;
    protected String comment;
    public static final String INVALID_DATA_STATUS = "Invalid Data";
    public static final String DONE_STATUS = "Done";

    IWebServicesSessionBean  webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    public String changeRate;


    private FileFormat fileFormat;
    private EDIFileBL ediFileBL;
    private EDITypeWS ediType;

    IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);

    public RateChangeTask() {
        setUseTransaction(true);
    }
    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        // binding parmeter from company level meta field to instance variable
        setCommonParameter();

        //creating fileFormat and EDIFileBL object only one time. It will be used below to generate multiple EDI files
        fileFormat = FileFormat.getFileFormat(EDI_TYPE_ID);
        ediFileBL = new EDIFileBL();
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);

        List<UserDTO> userDTOList=new UserDAS().findByMetaFieldNameAndValue(FileConstants.CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME, companyCurrentDate(), getEntityId());
        for(UserDTO tmpUserDTO:userDTOList){
            status = null;
            comment = null;
            //process each user in a new transaction
            PlatformTransactionManager transactionManager = null;
            TransactionStatus txStatus = null;
            UserDTO userDTO = null;
            try{
                transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
                DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                txStatus = transactionManager.getTransaction( transactionDefinition );

                //attach user to the new transaction
                userDTO = new UserDAS().find(tmpUserDTO.getId());

                MetaFieldValue rateMetaFieldValue=userDTO.getCustomer().getMetaField(FileConstants.CUSTOMER_RATE_METAFIELD_NAME);
                String plan = getCustomerAitMetaFieldValue(ChangeRequestGenerationConstants.PLAN.getValue(), userDTO);
                if(plan==null){
                    throw new SessionInternalError("No Plan assigned to customer "+userDTO);
                }
                ItemDTO item = new ItemDAS().findItemByInternalNumber(plan,userDTO.getCompany().getId());

                PlanDTO planDTO = new PlanDAS().findPlanByItemId(item.getId());
                if(planDTO==null){
                    throw new SessionInternalError("Configuration issue: item "+item.getInternalNumber()+" should be plan");
                }

                MetaFieldValue metaFieldValue = planDTO.getMetaField(ChangeRequestGenerationConstants.BILLING_MODEL.getValue());
                if (metaFieldValue == null) {
                    throw new SessionInternalError("Configuration issue : In Plan " + ChangeRequestGenerationConstants.BILLING_MODEL.getValue() + " value not found");
                }

                if(metaFieldValue.getValue().equals(ChangeRequestGenerationConstants.RATE_READY.getValue())){
                    ItemDTO planItem=planDTO.getPlanItems().get(0).getItem();
                    if(planItem==null){
                        throw new SessionInternalError("Configuration issue : Plan Should have an item");
                    }
                    BigDecimal planItemPrice=ediTransactionBean.getPlanItemPrice(userDTO, planItem.getId(), getEntityId());
                    if(planItemPrice.compareTo((BigDecimal)rateMetaFieldValue.getValue())!=0){
                        Double price=new Double(planItemPrice.stripTrailingZeros().toString());
                        LOG.debug("Find rate : "+price);
                        changeRate=ediTransactionBean.getRateCode(getEntityId(), price);
                        BigDecimal findRate=ediTransactionBean.getRateByRateCode(getEntityId(), changeRate);
                        LOG.debug("Searched Rate code "+changeRate);
                        status = DONE_STATUS;
                        userDTO.getCustomer().setMetaField(rateMetaFieldValue.getField(), findRate);
                    }
                }

                MetaFieldValue rateChangeDateMetaField=userDTO.getCustomer().getMetaField(ChangeRequestGenerationConstants.RATE_CHANGE_DATE.getValue());
                Date rateChangeDate=(Date)rateChangeDateMetaField.getValue();
                DateTime rateChangeDateTime = new DateTime(rateChangeDate);

                if (ediTransactionBean.hasPlanSendRateChangeDaily(planDTO)) {
                    rateChangeDateTime = rateChangeDateTime.plusDays(1);
                } else {
                    LOG.debug("Calculating customer "+userDTO.getId()+" next Rate Change Date");
                    MetaFieldValue cycleNumberMetaField=userDTO.getCustomer().getMetaField(ChangeRequestGenerationConstants.CYCLE_NUMBER.getValue());
                    if(cycleNumberMetaField==null || cycleNumberMetaField.getValue()==null){
                        throw new SessionInternalError("User "+userDTO.getId()+" should have Meter Cycle");
                    }
                    Integer meterCycle=(Integer)cycleNumberMetaField.getValue();
                    DateTime currentDateTime=new DateTime();
                    rateChangeDateTime = ediTransactionBean.getRateChangeDate(currentDateTime, meterCycle, getEntityId());
                    LOG.debug("Calculated Rate change date : "+rateChangeDateTime);
                }
                userDTO.getCustomer().setMetaField(rateChangeDateMetaField.getField(), rateChangeDateTime.toDate());
                ediTransactionBean.updateUser(userDTO);
                transactionManager.commit(txStatus);

            } catch (Throwable ex) {
                transactionManager.rollback(txStatus);
                LOG.error(ex);
                status = (status == null) ? INVALID_DATA_STATUS : status;
                comment = ex.getMessage();
            }
            List<Map<String, String>> recordMapList = mapMetaFieldWithRecord(userDTO.getEntity().getId(), userDTO);
            generateFile(userDTO, recordMapList);
        }
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
                        map.put(metaFieldValue.getField().getName(), metaFieldValue.getValue().toString());
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

    private List<Map<String, String>> mapMetaFieldWithRecord(Integer entityId, UserDTO user) {
        Map<String, String> metaFieldMap = fetchMapForMetaField(getCustomerMetaFieldValues(user));
        FileFormat fileFormat = FileFormat.getFileFormat(EDI_TYPE_ID);
        List<Map<String, String>> recordMapList = new ArrayList<Map<String, String>>();
        for (RecordStructure recordStructure : fileFormat.getFileStructure().getRecordStructures()) {
            matchRecords(recordStructure, metaFieldMap, recordMapList, user);
        }
        LOG.debug("Record map list is:  " + recordMapList);
        return recordMapList;
    }

    public String getTaskName() {
        return "Rate Change file, generation entity Id: " + getEntityId();
    }


    public String getCustomerAitMetaFieldValue(String key, UserDTO userDTO){
        for(CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField:userDTO.getCustomer().getCustomerAccountInfoTypeMetaFields()){
            if(customerAccountInfoTypeMetaField.getMetaFieldValue().getField().getName().equals(key)){
                MetaFieldValue accountNumberMetaField=customerAccountInfoTypeMetaField.getMetaFieldValue();
                return (String)accountNumberMetaField.getValue();
            }
        }
        return null;
    }

    private List<Map<String, String>> matchRecords(RecordStructure recordStructure, Map<String, String> metaFieldMap, List<Map<String, String>> recordMapList, UserDTO user) {

        Record record = recordStructure.getRecord();
        LOG.debug("record.getRecId().getDefaultValue()  " + record.getRecId().getDefaultValue());
        Integer loop = Integer.parseInt(recordStructure.getLoop().equals("n")? CDERecordDefaultCount:recordStructure.getLoop());
        for (int i = 0; i < loop; i++) {
            String recId = record.getRecId().getDefaultValue();
            switch (recId) {
                case FileConstants.KEY:
                    LOG.debug(recId);
                    recordMapList.add(setKeyRecord(record, metaFieldMap));
                    break;
                case FileConstants.HDR:
                    LOG.debug(recId);
                    recordMapList.add(setHeaderRecord(record, metaFieldMap));
                    break;
                case FileConstants.ACT:
                    LOG.debug(recId);
                    recordMapList.add(setAccountRecord(record, metaFieldMap));
                    break;
                case FileConstants.DTE:
                    LOG.debug(recId);
                    recordMapList.add(setDateRecord(record, metaFieldMap));
                    break;
                case FileConstants.MTR:
                    LOG.debug(recId);
                    recordMapList.add(setMeterRecord(record, metaFieldMap));
                    break;

            }
            if (recordStructure.getChildRecord() != null && recordStructure.getChildRecord().size() > 0) {
                for (RecordStructure childRecordStructure : recordStructure.getChildRecord()) {
                    matchRecords(childRecordStructure, metaFieldMap, recordMapList, user);
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
        keyMap.put(RecordValues.TRANSACTION_SET.name(),  (String) RecordValues.TRANSACTION_SET.getValue());
        keyMap.put(RecordValues.TRANSCTION_SUBSET.name(),(String) RecordValues.TRANSCTION_SUBSET.getValue());

        return keyMap;
    }

    private Map<String, String> setHeaderRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> headerMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        headerMap.put(TRANS_REF_NR, randomNumber);
        headerMap.put(FileConstants.UTILITY_NAME_META_FIELD_NAME, UTILITY_NAME);
        headerMap.put(FileConstants.SUPPLIER_NAME_META_FIELD_NAME, SUPPLIER_NAME);
        headerMap.put(CREATE_DATE, convertStringToDate(convertDateToString(TimezoneHelper.serverCurrentDate()), getDateFormatFromRecord(record, CREATE_DATE)));
        return headerMap;
    }



    private Map<String, String> setAccountRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> accountMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        accountMap.put(FileConstants.BILL_DELIVER_META_FIELD_NAME, BILL_DELIVER);
        accountMap.put(FileConstants.BILL_CALC_META_FIELD_NAME, BILL_CALC);
        accountMap.put(RecordValues.PERCENT_PARTICPATE.name(),(String) RecordValues.PERCENT_PARTICPATE.getValue());
        accountMap.put(RecordValues.SERVICE_REQUESTED.name(),(String) RecordValues.SERVICE_REQUESTED.getValue());
        accountMap.put(RecordValues.SERVICE_REQUESTED2.name(),(String) RecordValues.SERVICE_REQUESTED2.getValue());
        accountMap.put(RecordValues.LINE_NR.name(),(String) RecordValues.LINE_NR.getValue());
        return accountMap;
    }

    private Map<String, String> setDateRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> dateMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        dateMap.put(RecordValues.CUST_ENROLL_AGREE_TIME.name(),(String) RecordValues.CUST_ENROLL_AGREE_TIME.getValue());
        dateMap.put(RecordValues.CUST_TIMEZONE.name(),TimeZone.getEnum(dateMap.get(RecordValues.CUST_TIMEZONE.name())));
        return dateMap;
    }

    private Map<String, String> setMeterRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> dateMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        dateMap.put(RecordValues.METER_IDENTIFIER.name(), (String) RecordValues.METER_IDENTIFIER.getValue());
        dateMap.put(RecordValues.METER_ACTION.name(), (String) RecordValues.METER_ACTION.getValue());
        dateMap.put(RecordValues.SUPPLIER_RATE_CD.name(), changeRate);
        return dateMap;
    }

    private Map<String, String> setCodeRecord(Record record, Map<String, String> metaFieldMap) {
        Map<String, String> codeMap = setRecordFieldInMetaField(record, metaFieldMap, new HashMap<String, String>());
        codeMap.put(RecordValues.METER_IDENTIFIER.name(), (String) RecordValues.METER_IDENTIFIER.getValue());
        return codeMap;
    }
    private String convertStringToDate(String date, String format) {
        String finalDateInString = "";
        if (StringUtils.isNotEmpty(date) && format != null) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                Date date1 = formatter.parse(date);
                SimpleDateFormat convertedFormatter = new SimpleDateFormat(format.replaceAll("C", "Y").replaceAll("D", "d"));
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

    public void setCommonParameter() {
        bindingCompanyMetaFieldValues();

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME);
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

    protected void bindingCompanyMetaFieldValues() {
        CompanyWS companyWS = ediTransactionBean.getCompanyWS(getEntityId());
        MetaFieldValueWS[] metaFieldValues = companyWS.getMetaFields();
        LOG.debug("metaFieldValues  are: 333333333  " + Arrays.asList(metaFieldValues));
        companyMetaFieldValueMap = new HashMap<String, Object>();
        for (MetaFieldValueWS metaFieldValueWS : metaFieldValues) {
            companyMetaFieldValueMap.put(metaFieldValueWS.getFieldName(), metaFieldValueWS.getValue());
        }
    }

    private List<MetaFieldValue> getCustomerMetaFieldValues(UserDTO userDTO){
        List<MetaFieldValue> metaFieldValues=new ArrayList<>();
        for(CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField: userDTO.getCustomer().getCustomerAccountInfoTypeMetaFields()){
            metaFieldValues.add(customerAccountInfoTypeMetaField.getMetaFieldValue());
        }
        return metaFieldValues;
    }

    private void generateFile(UserDTO userDTO, List<Map<String, String>> recordMapList) {
//        Create file naming convention using this link. https://docs.google.com/document/d/14D0T2jQeMajCD3xnWu2_2cYGy6gEHL6q7mRIzrgxACA/edit
        FILE_NAME = UTILITY_DUNS + UNDERSCORE_SEPARATOR + SUPPLIER_DUNS + UNDERSCORE_SEPARATOR + randomNumber + DOT_SEPARATOR + ediType.getEdiSuffix();


        try {
            EDIFileDTO ediFileDTO = ediTransactionBean.generateEDIFile(fileFormat, userDTO.getEntity().getId(), FILE_NAME, recordMapList);
            EDIFileWS fileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());

            //Binding userId and utility account number
            fileWS.setUserId(userDTO.getUserId());
            MetaFieldValue<String> accountNumberMetafield=userDTO.getCustomer().getMetaField(FileConstants.CUSTOMER_ACCOUNT_KEY);

            LOG.debug("Account Number : "+accountNumberMetafield);
            if(accountNumberMetafield!=null){
                fileWS.setUtilityAccountNumber(accountNumberMetafield.getValue());
            }

            EDIFileStatusWS processedFileWS = webServicesSessionSpringBean.findEdiStatusById(FileConstants.EDI_STATUS_PROCESSED);
            LOG.debug("fileWS.getEdiFileStatusWS().getName()  " + fileWS.getEdiFileStatusWS().getName());
            if (fileWS.getEdiFileStatusWS().getName().equalsIgnoreCase(processedFileWS.getName())) {
                LOG.debug("In CustomerEnrollmentFileGenerationTask ediStatus is:  " + status);
                LOG.debug("In StringUtils.trimToNull(ediStatus) is:  " + StringUtils.trimToNull(status));
                EDIFileStatusWS statusWS = null;
                for (EDIFileStatusWS ediFileStatus : ediType.getEdiStatuses()) {
                    if (ediFileStatus.getName().equals(status)) {
                        statusWS = ediFileStatus;
                    }
                }

                if (statusWS == null) {
                    throw new SessionInternalError("EDI file status can not be null");
                }

                fileWS.setEdiFileStatusWS(statusWS);
                if (comment != null) fileWS.setComment(comment);

                Integer editFileId = webServicesSessionSpringBean.saveEDIFileRecord(fileWS);
                LOG.debug("After changing ediFileDTO.getFileStatus() " + fileWS.getEdiFileStatusWS().getName());
            }

            ediFileBL.saveEDIFile(fileWS);
        } catch (Exception ex) {
            LOG.error("Caught Exception While generating file " + ex);
            ex.printStackTrace();
        }

    }
}
