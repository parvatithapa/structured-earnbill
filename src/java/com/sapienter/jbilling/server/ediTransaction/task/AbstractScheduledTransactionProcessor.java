package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by aman on 16/10/15.
 */
public abstract class AbstractScheduledTransactionProcessor extends AbstractCronTask
        implements ItemProcessor<EDIFileWS, EDIFileWS> , FileStructure{

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractScheduledTransactionProcessor.class));

    IEDITransactionBean ediTransactionBean=Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
    protected Map<String, Object> companyMetaFieldValueMap = null;

    //Common Parameters, Need to define in
    protected Integer EDI_TYPE_ID;
    protected String SUPPLIER_DUNS;
    protected String UTILITY_DUNS;
    IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    protected String exceptionCode;
    protected String recordLevelExceptionCode;
    protected String status;
    protected  EDIFileWS ediFile;
    protected Map<String, Map<String, String>> recordFields = null;
    protected List<Map<String, String>> recordFieldsList = null;
    protected UserDTO userDTO;
    protected Date startDate;
    protected Date endDate;
    protected String customerAccountNumber;
    public static final String DATE_FORMAT_PATTERN = "yyyyMMdd";
    protected static DateTimeFormatter dateFormat = DateTimeFormat.forPattern(DATE_FORMAT_PATTERN);

    protected String escapeExceptionStatus;
    //Batch Processing

    //Get Job name from implemented class to execute
    protected abstract String getJobName();

    protected void bindPluginParameter(Map<String, String> pluginParameter){}
    public void processFile(EDIFileWS ediFileWS, String escapeStatus) throws Exception{}

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super._init(context);
        LOG.debug("Running Batch Cron scheduler for : " + this.getClass().getCanonicalName());
        JobLauncher asyncJobLauncher = Context.getBean(Context.Name.BATCH_ASYNC_JOB_LAUNCHER);
        Job pluginProcessorJob = Context.getBean(getJobName());

//        Get meta field value in array
        setMetaFieldValues(getEntityId());
        SUPPLIER_DUNS = (String) companyMetaFieldValueMap.get(FileConstants.SUPPLIER_DUNS_META_FIELD_NAME);
        UTILITY_DUNS = (String) companyMetaFieldValueMap.get(FileConstants.UTILITY_DUNS_META_FIELD_NAME);

        Map jobParams = new HashMap();
        jobParams.put("date", new JobParameter(TimezoneHelper.serverCurrentDate()));
        jobParams.put("companyId", new JobParameter((long) getEntityId()));
        jobParams.put(Constants.BATCH_JOB_PARAM_ENTITY_ID, new JobParameter(getEntityId().toString()));
        preBatchConfiguration(jobParams);
        JobExecution execution = null;
        try {
            //execute the job asynchronously
            execution = asyncJobLauncher.run(pluginProcessorJob, new JobParameters(jobParams));
            Long executionId = execution.getId();
        } catch (Exception e) {

            LOG.error("Unable to run batch job for class :" + this.getClass().getCanonicalName());
            e.printStackTrace();
            throw new JobExecutionException("Unable to run batch job for class :" + this.getClass().getCanonicalName(), e);
        }
    }

    @Override
    public abstract String getTaskName();

    public abstract void preBatchConfiguration(Map jobParams);

    public abstract EDIFileWS process(EDIFileWS ediFileWS) throws Exception;


    //Utility Methods
    // Parse record and find Fields

    protected void parseRecords(List<String> recordNames, EDIFileRecordWS[] allRecords) {
        recordFields = new HashMap<String, Map<String, String>>();
        for (EDIFileRecordWS record : allRecords) {
            String recordName = record.getHeader();
            if (recordNames.contains(recordName)) {
                recordFields.put(record.getHeader(), parseRecord(record));
            }
        }
    }


    protected void getHeaderRecord(EDIFileWS ediFileWS, Map<String, String> result, String recordType){
        for(EDIFileRecordWS ediFileRecordWS:ediFileWS.getEDIFileRecordWSes()){
            if(ediFileRecordWS.getHeader().equals(recordType)){
                for(EDIFileFieldWS ediFileFieldWS:ediFileRecordWS.getEdiFileFieldWSes()){

                    result.put(ediFileFieldWS.getKey(), ediFileFieldWS.getValue());
                }
            }
        }
    }


    protected void parseRecordsList(EDIFileRecordWS[] allRecords) {
        recordFieldsList = new ArrayList<Map<String, String>>();
        for (EDIFileRecordWS recordWS : allRecords) {
            Map<String, String> recordMap = new HashMap<String, String>();
            recordMap.put(FileConstants.TAG_NAME_REC_ID, recordWS.getHeader());
            for (EDIFileFieldWS fileFieldWS : recordWS.getEdiFileFieldWSes()) {
                recordMap.put(fileFieldWS.getKey(), fileFieldWS.getValue());
            }
            recordFieldsList.add(recordMap);
        }
    }


    protected Map<String, String> parseRecord(EDIFileRecordWS record) {
        Map fields = new HashMap<String, String>();
        for (EDIFileFieldWS field : record.getEdiFileFieldWSes()) {
            fields.put(field.getKey(), field.getValue());
        }
        return fields;
    }

    protected String findField(FileStructure recordName, String fieldName) {
        if (recordFields.containsKey(recordName.toString())) {
            Map<String, String> fields = recordFields.get(recordName.toString());
            if (fields.containsKey(fieldName)) {
                return fields.get(fieldName);
            }
        }
        return null;
    }

    protected String findField(Map<String, String> fields, String fieldName, boolean mandatory) {
        String value = fields.get(fieldName);
        if ((value == null || value.isEmpty()) && mandatory == true) {
            throw new SessionInternalError("Mandatory field not found : \"" + fieldName + "\"");
        }
        return value;
    }

    protected String findField(FileStructure recordName, String fieldName, boolean mandatory) {
        String value = findField(recordName, fieldName);
        if ((value == null || value.isEmpty()) && mandatory == true) {
            throw new SessionInternalError("Mandatory field not found : \"" + fieldName + "\"");
        }
        return value;
    }

    protected Object findField(Map<String, String> fields, String fieldName, String className, boolean mandatory) {
        String value = findField(fields, fieldName, mandatory);
        return formatValue(fieldName, value, className);
    }

    protected Object findField(FileStructure recordName, String fieldName, String className, boolean mandatory) {
        String value = findField(recordName, fieldName, mandatory);
        return formatValue(fieldName, value, className);
    }

    public static Object formatValue(String fieldName, String value, String className){
        Object formattedValue = null;
        switch (className) {

            case "Integer":
                try {
                    formattedValue = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new SessionInternalError("Unable to Parse the value \"" + value + "\" of Field " + fieldName + " in to Integer");
                }
                break;

            case "Date":
                try {
                    formattedValue = dateFormat.parseDateTime(value).toDate();
                } catch (IllegalArgumentException e) {
                    throw new SessionInternalError("Unable to Parse the value \"" + value + "\" of Field " + fieldName + " in to Date");
                }
                break;

            case "BigDecimal":
                try {
                    //If there  amount is blank then it should be zero.
                    if (value!=null){
                        formattedValue = new BigDecimal(value);
                    }
                } catch (NumberFormatException e) {
                    throw new SessionInternalError("Unable to Parse the value \"" + value + "\" of Field " + fieldName + " in to Money Format");
                }
                break;
        }
        return formattedValue;
    }

    protected boolean isUserAlreadyTerminatedOrDropped(UserWS userWS) {
        if (userWS!=null) {
            for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
                if (metaFieldValueWS.getFieldName().equals(FileConstants.TERMINATION_META_FIELD)) {
                    LOG.debug("Termination meta field exist.");
                    if (metaFieldValueWS.getValue().equals(FileConstants.TERMINATION_PROCESSING) || metaFieldValueWS.getValue().equals(FileConstants.DROPPED)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static UserWS updateTerminationMetaField(UserWS userWS, String terminationValue, Date terminationDate) {

        if(terminationValue.equals(FileConstants.TERMINATION_PROCESSING)){
            new CustomerBL().processEarlyTerminationFee(userWS, terminationDate);
        }

        boolean isMetafieldValueExist = false;
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (metaFieldValueWS.getFieldName().equals(FileConstants.TERMINATION_META_FIELD)) {
                LOG.debug("Termination meta field exist.");
                isMetafieldValueExist = true;
                metaFieldValueWS.setValue(terminationValue);
            }
        }
        if (!isMetafieldValueExist) {
            LOG.debug("meta field value not exist. create one.");
            MetaFieldValueWS metaField = new MetaFieldValueWS();
            metaField.setFieldName(FileConstants.TERMINATION_META_FIELD);
            metaField.setValue(terminationValue);
            List<MetaFieldValueWS> metaFieldValueWSes = new ArrayList<MetaFieldValueWS>(Arrays.asList(userWS.getMetaFields()));
            metaFieldValueWSes.add(metaField);
            userWS.setMetaFields(metaFieldValueWSes.toArray(new MetaFieldValueWS[metaFieldValueWSes.size()]));
        }
        return userWS;
    }

    protected void updateUser(UserWS user) throws SessionInternalError {
        UserBL bl = new UserBL(user.getUserId());
        Integer executorId = user.getUserId();
        UserDTOEx dto = new UserDTOEx(user, user.getEntityId());
        bl.getEntity().touch();
        bl.update(executorId, dto);
    }


    protected void setMetaFieldValues(Integer entityId) {
        LOG.debug("entity ID: %s", entityId);
        CompanyWS companyWS = ediTransactionBean.getCompanyWS(entityId);
        MetaFieldValueWS[] metaFieldValues = companyWS.getMetaFields();
        LOG.debug("MetaFieldValues  are:  %s", Arrays.asList(metaFieldValues));
        companyMetaFieldValueMap = new HashMap<String, Object>();
        for(MetaFieldValueWS metaFieldValueWS : metaFieldValues) {
            companyMetaFieldValueMap.put(metaFieldValueWS.getFieldName(), metaFieldValueWS.getValue());
        }
    }

    public String getCustomerType(UserWS userWS) {
        LOG.debug("Is that customer bill ready: " + userWS.getId());
        Object planValue = findMetaFieldValue(userWS.getMetaFields(), InvoiceBuildTask.InvoiceBuildConstants.PLAN.getValue());
        if (planValue == null) {
            throw new SessionInternalError("Customer should be subscribed to plan");
        }

        ItemDTO item = new ItemDAS().findItemByInternalNumber(planValue.toString(), userWS.getEntityId());

        PlanDTO planDTO = item.getPlans().iterator().next();
        PlanWS pln = webServicesSessionSpringBean.getPlanWS(planDTO.getId());
        Object value = findMetaFieldValue(pln.getMetaFields(), FileConstants.BILLING_MODEL);

        return value.toString();
    }

    private static Object findMetaFieldValue(MetaFieldValueWS[] metaFieldValueWSes, String metaFieldName) {
        for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSes) {
            if (metaFieldValueWS.getFieldName().equals(metaFieldName)) {
                LOG.debug("Meta field: " + metaFieldName + " found for: " + metaFieldValueWS.getValue());
                return metaFieldValueWS.getValue();
            }
        }
        LOG.error("No met field found for " + metaFieldName);
        return null;
    }

    protected void getMeterReadRecord(EDIFileWS ediFileWS, Map<String, String> result){
        for(EDIFileRecordWS ediFileRecordWS:ediFileWS.getEDIFileRecordWSes()){


            if(ediFileRecordWS.getHeader().equals("HDR")){
                for(EDIFileFieldWS ediFileFieldWS:ediFileRecordWS.getEdiFileFieldWSes()){
                    result.put(ediFileFieldWS.getKey(), ediFileFieldWS.getValue());
                }
            }

            if(ediFileRecordWS.getHeader().equals("UMR")){
                for(EDIFileFieldWS ediFileFieldWS:ediFileRecordWS.getEdiFileFieldWSes()){
                    result.put(ediFileFieldWS.getKey(), ediFileFieldWS.getValue());
                }
                return;
            }
        }
    }


    public void throwException(String message, String exceptionCode, String status){
       throwException(message,exceptionCode,status, false);
    }

    public void throwException(String message, String exceptionCode, String status, Boolean mandatory){
        if(exceptionCode!=null){
            this.exceptionCode=exceptionCode;
        }
        this.status=status;
        if(mandatory || escapeExceptionStatus==null){
            throw new SessionInternalError(message);
        }else{
            if(!status.equals(escapeExceptionStatus)){
                throw new SessionInternalError(message);
            }
        }
    }

    public void setRecordLevelExceptionCode(EDIFileRecordWS recordWS){
        EDIFileFieldWS ediFileFieldWS= new EDIFileFieldWS();

        ediFileFieldWS.setKey(FileConstants.EXCEPTION_CODE_KEY);
        ediFileFieldWS.setValue(recordLevelExceptionCode);
        ediFileFieldWS.setOrder(recordWS.getEdiFileFieldWSes().length);

        List<EDIFileFieldWS> ediFileFieldWSList=new ArrayList<>(Arrays.asList(recordWS.getEdiFileFieldWSes()));
        ediFileFieldWSList.add(ediFileFieldWS);
        recordWS.setEdiFileFieldWSes(null);
        recordWS.setEdiFileFieldWSes(ediFileFieldWSList.toArray(new EDIFileFieldWS[ediFileFieldWSList.size()]));
    }

    public void  updateFileStatus(Integer ediFileId, String statusName){
        webServicesSessionSpringBean.updateEDIFileStatus(ediFileId, statusName, null);
    }

    // add a metafield on the customer
    public  <T> void addMetaField(UserWS user,String name, T value){
        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.setFieldName(name);
        metaField.setValue(value);

        List<MetaFieldValueWS> metaFieldValueWSes = new ArrayList<MetaFieldValueWS>(Arrays.asList(user.getMetaFields()));
        metaFieldValueWSes.add(metaField);
        user.setMetaFields(metaFieldValueWSes.toArray(new MetaFieldValueWS[metaFieldValueWSes.size()]));
    }

    /**
     * This method used to bind the some important field(utility account number, start date, end date and user) to EDI file which is used to processing EDI file.
     * @param ediFileWS
     */
    public void bindFields(EDIFileWS ediFileWS){
        if (userDTO!=null){
            ediFileWS.setUserId(userDTO.getId());
        }
        ediFileWS.setStartDate(startDate);
        ediFileWS.setEndDate(endDate);
        ediFileWS.setUtilityAccountNumber(customerAccountNumber);
    }

}
