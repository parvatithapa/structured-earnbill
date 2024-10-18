package com.sapienter.jbilling.server.customerEnrollment.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentCommentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.event.AcceptedByLDCEnrollmentEvent;
import com.sapienter.jbilling.server.customerEnrollment.event.RejectedByLDCEnrollmentEvent;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.task.AbstractScheduledTransactionProcessor;
import com.sapienter.jbilling.server.ediTransaction.task.FileStructure;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.batch.core.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by vivek on 10/9/15.
 */
public class EnrollmentResponseParserTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EnrollmentResponseParserTask.class));

//    public static final Long TRANSACTION_SET = new Long(814);

    private String SUCCESS_STATUS;
    private String ACCEPT_STATUS;
    private String REJECT_STATUS;
    private String INVALID_FILE_STATUS;
    private String INTERNAL_ERROR;
    private int companyId;

    private Integer ediTypeId;
    private String supplierDUNS;
    private String utilityDUNS;

    public static enum EnrollmentResponseParser implements FileStructure {
        KEY, HDR, NME, ACT, DTE, MTR, CDE
    }

    private EDIFileWS outboundFileWS = null;
    private CustomerEnrollmentWS customerEnrollmentWS = null;
    /*Status, comment, exception code for inbound edi file*/
    private String status = null;
    private String comment;
    private String exceptionCode;
    /*status comment for outbound edi file.*/
    private String outboundStatus = null;
    private String outboundComment = null;

    /*Status, comment for customer enrollment*/
    private CustomerEnrollmentStatus enrollmentStatus = null;
    private String enrollmentComment;
    private Integer customerEnrollmentId = null;


    public static enum EnrollmentResponseParserField {
        TRANS_REF_NR,
        LIFECYCLE_NR,
        CODE,
        REC_TYPE,
        METER_CYCLE,
        SUPPLIER_RATE_CD,
        UTILITY_CUST_ACCT_NR,
        PEAK_LOAD_CONTRIBUTION,
        TRANSMISSION_CONTRIBUTION,
        STANDARD_POINT_LOC
    }


    IWebServicesSessionBean webServicesSessionSpringBean;
    IEDITransactionBean ediTransactionBean;

    protected static final ParameterDescription ACCEPT_STATUS_NAME =
            new ParameterDescription("accept_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECT_STATUS_NAME =
            new ParameterDescription("reject_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription SUCCESS_STATUS_NAME =
            new ParameterDescription("success_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_FILE_STATUS_NAME =
            new ParameterDescription("invalid_file_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INTERNAL_ERROR_NAME =
            new ParameterDescription("internal_error", true, ParameterDescription.Type.STR);

    {
        descriptions.add(ACCEPT_STATUS_NAME);
        descriptions.add(REJECT_STATUS_NAME);
        descriptions.add(SUCCESS_STATUS_NAME);
        descriptions.add(INVALID_FILE_STATUS_NAME);
        descriptions.add(INTERNAL_ERROR_NAME);
    }


    public String getTaskName() {
        return "Enrollment response parser task, entity Id: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_ENROLLMENT_RESPONSE_PARSER_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        LOG.debug("Execute InvoiceReadTask  plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME);
        if(EDI_TYPE_ID == null) {
            throw new SessionInternalError("EDI type id not valid : " + EDI_TYPE_ID);
        }

        EDITypeWS ediType = null;
        //todo : refactor
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throw new SessionInternalError("EDI type id not found : " + EDI_TYPE_ID);


        jobParams.put("ACCEPT_STATUS", new JobParameter(parameters.get(ACCEPT_STATUS_NAME.getName())));

        jobParams.put("REJECT_STATUS", new JobParameter(parameters.get(REJECT_STATUS_NAME.getName())));
        jobParams.put("SUCCESS_STATUS", new JobParameter(parameters.get(SUCCESS_STATUS_NAME.getName())));
        jobParams.put("INVALID_FILE_STATUS", new JobParameter(parameters.get(INVALID_FILE_STATUS_NAME.getName())));
        jobParams.put("INTERNAL_ERROR", new JobParameter(parameters.get(INTERNAL_ERROR_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {

        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);

        LOG.debug("EDI File Invoice Item Processor : Before Step");
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");

        SUCCESS_STATUS = jobParameters.getString("SUCCESS_STATUS");
        ACCEPT_STATUS = jobParameters.getString("ACCEPT_STATUS");
        REJECT_STATUS = jobParameters.getString("REJECT_STATUS");
        INTERNAL_ERROR = jobParameters.getString("INTERNAL_ERROR");
        INVALID_FILE_STATUS = jobParameters.getString("INVALID_FILE_STATUS");
        companyId = jobParameters.getLong("companyId").intValue();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    private void clearData(){

        comment=null;
        status=null;
        outboundFileWS = null;
        customerEnrollmentWS = null;
        outboundStatus = null;
        outboundComment = null;

    /*Status, comment for customer enrollment*/
        enrollmentStatus = null;
        enrollmentComment=null;
        customerEnrollmentId = null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
        customerAccountNumber=null;
        userDTO=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        setMetaFieldValues(companyId);
        LOG.debug("Enrollment Response parser Task Process Method  " + ediFileWS);
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;

                /*Parse record in Map*/
                processEnrollmentResponseFile();
                /*Try to enroll customer from here*/
                enrollCustomer();

                /*Update status and comment in customer enrollment and outbound file.*/
                updateEnrollmentAndOutboundFile();
            } else {
                // Change the status to Error Detected
                LOG.error("Error");
                status = ediFileWS.getEdiFileStatusWS().getName();
            }
        } catch (Exception ex) {
            LOG.error("---------------Final exception in enrollment response parser task------------------------- ", ex);
            status = (status == null) ? INVALID_FILE_STATUS : status;
            comment= EDITransactionHelper.getExceptionMessage(ex);
            if (enrollmentStatus == CustomerEnrollmentStatus.REJECTED) {
                EventManager.process(new RejectedByLDCEnrollmentEvent(this.getEntityId(), customerEnrollmentId, ediFileWS.getExceptionCode()));
            }
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for (EDIFileStatusWS ediStatus : ediType.getEdiStatuses()) {
            if (ediStatus.getName().equals(status)) {
                statusWS = ediStatus;
            }
        }

        ediFileWS.setEdiFileStatusWS(statusWS);
        ediFileWS.setComment(comment);
        if (exceptionCode != null) ediFileWS.setExceptionCode(exceptionCode);
        bindFields(ediFileWS);
        clearData();
        return ediFileWS;
    }

    private void updateEnrollmentAndOutboundFile() {
        //        Save enrollment from here.
        if (customerEnrollmentWS != null) {
            changeCustomerEnrollmentStatus(enrollmentStatus, enrollmentComment);
        }

        //        Save outbound file here.
        if (outboundFileWS != null && outboundStatus != null) {
            outboundFileWS.setComment(outboundComment);
            //binding user id to the enrollment outbound file
            outboundFileWS.setUserId(userDTO.getUserId());
            changeEdiFileStatus(outboundStatus, outboundFileWS);
        }
    }


    private void processEnrollmentResponseFile() throws Exception {
        List<String> maidenRecords = new LinkedList<String>();
        maidenRecords.add(EnrollmentResponseParser.KEY.toString());
        maidenRecords.add(EnrollmentResponseParser.HDR.toString());
        maidenRecords.add(EnrollmentResponseParser.ACT.toString());
        maidenRecords.add(EnrollmentResponseParser.MTR.toString());
        maidenRecords.add(EnrollmentResponseParser.CDE.toString());
        maidenRecords.add(EnrollmentResponseParser.DTE.toString());
        //Parse maiden records
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());

        customerAccountNumber = findField(EnrollmentResponseParser.ACT, EnrollmentResponseParserField.UTILITY_CUST_ACCT_NR.toString());
        LOG.debug("Response Account Number: "+customerAccountNumber);

    }

    private void enrollCustomer() {
//        Create customer from enrollment from here.
        String transferNRValue = findField(EnrollmentResponseParser.HDR, EnrollmentResponseParserField.TRANS_REF_NR.toString());
        LOG.debug(EnrollmentResponseParserField.TRANS_REF_NR.toString() + "value found : " + transferNRValue);

        try {
            ediTransactionBean.isUniqueKeyExistForFile(companyId, ediTypeId, ediFile.getId(), FileConstants.TRANS_REF_NR, transferNRValue, TransactionType.INBOUND);
        } catch (SessionInternalError sie) {
            LOG.error("Edi file has unique key.");
            status = INVALID_FILE_STATUS;
            comment = sie.getMessage();
            exceptionCode = FileConstants.DUPLICATE_TRANSACTION_EXP_CODE;
            throw sie;
        }

//        Get customer enrollment id here.
        customerEnrollmentId = getCustomerEnrollmentId(ediFile);

        customerEnrollmentWS = webServicesSessionSpringBean.getCustomerEnrollment(customerEnrollmentId);

        if (customerEnrollmentWS == null) {
            LOG.error("Customer enrollment not found for this Enrollment ID : " + customerEnrollmentId);
            throw new SessionInternalError("Customer enrollment not found for this Enrollment ID : " + customerEnrollmentId+". Please provide the valid LIFECYCLE_NR");
        }

        if(customerEnrollmentWS.getMetaFieldValue(EnrollmentResponseParserField.UTILITY_CUST_ACCT_NR.toString())==null){
            throw new SessionInternalError("Enrollment did not have account number");
        }

        String enrollmentAccountNumber=(String)customerEnrollmentWS.getMetaFieldValue(EnrollmentResponseParserField.UTILITY_CUST_ACCT_NR.toString());
        LOG.debug("Enrolled Account Number: "+enrollmentAccountNumber);

        if(!enrollmentAccountNumber.toUpperCase().equals(customerAccountNumber)){
            throw new SessionInternalError("Account Number did match with the enrolled customer account number");
        }

        //binding enrollment account number to Inbound file
        customerAccountNumber=enrollmentAccountNumber;

        if (customerEnrollmentWS == null) {
            throw new SessionInternalError("Could not found enrollment corresponding to enrollment Id " + customerEnrollmentId);
        }

//        get outbound edi file for corresponding inbound file.
        outboundFileWS = getOutboundFileWS(customerEnrollmentId.toString());

         /*If edi file has CDE record and REC_TYPE is R means file will be rejected.*/
        checkIfFileRejected();

        /*Create customer and order here.*/
        try {
            createCustomerAndOrder();
        } catch (SessionInternalError sie) {
            status = INTERNAL_ERROR;
            comment = sie.getMessage();
            throw sie;
        }
    }

    private void createCustomerAndOrder() {
        if (outboundFileWS != null && outboundFileWS.getEdiFileStatusWS().getName().equals(SUCCESS_STATUS)) {
            try {

                Map<String, String> ediFileData=new HashMap<>();
                Integer meterCycle=(Integer)findField(EnrollmentResponseParser.MTR, EnrollmentResponseParserField.METER_CYCLE.toString(), "Integer", true);

                ediFileData.put(FileConstants.CUSTOMER_METER_CYCLE_METAFIELD_NAME, meterCycle + "");

                String peakLoadContribution = findField(EnrollmentResponseParser.ACT, EnrollmentResponseParserField.PEAK_LOAD_CONTRIBUTION.toString());
                if(peakLoadContribution!=null){
                    ediFileData.put(FileConstants.CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME, peakLoadContribution);
                }
                String transmissionContribution = findField(EnrollmentResponseParser.ACT, EnrollmentResponseParserField.TRANSMISSION_CONTRIBUTION.toString());

                if(transmissionContribution!=null){
                    ediFileData.put(FileConstants.CUSTOMER_TRANSMISSION_CONTRIBUTION_META_FIELD_NAME, transmissionContribution);
                }

                String zone = findField(EnrollmentResponseParser.ACT, EnrollmentResponseParserField.STANDARD_POINT_LOC.toString());

                if(zone!=null){
                    ediFileData.put(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME, zone);
                }

                //code for getting plan price and set into the customer metafield
                String plan = (String) customerEnrollmentWS.getMetaFieldValue(FileConstants.PLAN);
                ItemDTO item = new ItemDAS().findItemByInternalNumber(plan,customerEnrollmentWS.getEntityId());
                BigDecimal planPrice=item.getPrice(TimezoneHelper.companyCurrentDate(customerEnrollmentWS.getEntityId()), customerEnrollmentWS.getEntityId()).getRate();
                PlanDTO planDTO = new PlanDAS().findPlanByItemId(item.getId());

                if (planDTO.getMetaField(FileConstants.BILLING_MODEL) != null) {
                    String billingModel = ((StringMetaFieldValue)planDTO.getMetaField(FileConstants.BILLING_MODEL)).getValue();
                    //send rate for Rate Ready
                    if (FileConstants.BILLING_MODEL_RATE_READY.equals(billingModel)) {
                        String rate = findRate(ediFile);
                        ediFileData.put(FileConstants.CUSTOMER_RATE_METAFIELD_NAME, rate);
                    }

                    // Set email preference for Rate and Bill ready customers
                    MetaField receiveEmailsMetaField = new MetaFieldDAS().getFieldByName(companyId, new EntityType[]{EntityType.CUSTOMER}, FileConstants.META_FIELD_RECEIVE_EMAIL);
                    if (receiveEmailsMetaField != null) {
                        if (FileConstants.BILLING_MODEL_RATE_READY.equals(billingModel) || FileConstants.BILLING_MODEL_BILL_READY.equals(billingModel)) {
                            ediFileData.put(FileConstants.META_FIELD_RECEIVE_EMAIL, "false");
                        } else {
                            ediFileData.put(FileConstants.META_FIELD_RECEIVE_EMAIL, "true");
                        }
                    }
                } else {
                    LOG.error("Billing Model not found in that plan ID:" + planDTO.getId());
                    throw new SessionInternalError("Billing Model not found in that plan.");
                }


                ediFileData.put(FileConstants.CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME, getRateChangeDate(item.getId(), meterCycle));

                //Binding customer load curve meta field
                Object loadCurve=companyMetaFieldValueMap.get(FileConstants.INTERVAL_LOAD_CURVE_COMPANY_METAFIELD);
                if(loadCurve!=null){
                    ediFileData.put(FileConstants.INTERVAL_LOAD_CURVE_CUSTOMER_METAFIELD, (String)loadCurve);
                }

                String custEnrollAgreeDTStr = findField(EnrollmentResponseParser.DTE, FileConstants.CUST_ENROLL_AGREE_DT);
                //update ACTUAL_START_DATE from enrollment file
                if (custEnrollAgreeDTStr != null) {
                    Date custEnrollAgreeDT = dateFormat.parseDateTime(custEnrollAgreeDTStr).toDate();
                    Arrays.asList(customerEnrollmentWS.getMetaFields()).stream().filter(metaFieldValueWS -> metaFieldValueWS != null && metaFieldValueWS.getFieldName().equals(FileConstants.ACTUAL_START_DATE)).findFirst().get().setValue(custEnrollAgreeDT);
                }

                /*Create customer and order from order session bean. This method should always call within new transaction.*/
                Integer userId=ediTransactionBean.createCustomerAndOrder(customerEnrollmentWS, ediFileData);
                ediFile.setUserId(userId);
                userDTO = new UserDAS().find(userId);
//                Set edi file status after creating customer and it's order successfully.
                status = ACCEPT_STATUS;

//                Set outbound file status after creating customer and it's order successfully.
                outboundStatus = ACCEPT_STATUS;

//                Set Enrollment status after creating customer and it's order successfully.
                enrollmentStatus = CustomerEnrollmentStatus.ENROLLED;
                enrollmentComment = "Customer creation has been done.";

                EventManager.process(new AcceptedByLDCEnrollmentEvent(this.companyId, customerEnrollmentWS, meterCycle.toString()));

            } catch (Exception ex) {
                LOG.error("Caught exception here while trying to create customer and order.", ex);
                throw new SessionInternalError(ex.getMessage());
            }

        } else {
            if (outboundFileWS != null && outboundFileWS.getEdiFileStatusWS().getName().equals(ACCEPT_STATUS)) {
                comment = "Outbound file" + outboundFileWS.getName() + " is already in accepted status.";
                throw new SessionInternalError("Outbound file " + outboundFileWS.getName() + " is already in accepted status.");
            } else {
                comment = "File can be proceed. current status of Outbound file is " + outboundFileWS.getEdiFileStatusWS().getName();
                throw new SessionInternalError("Request can not be proceed. current status of corresponding Outbound file is " + outboundFileWS.getEdiFileStatusWS().getName());
            }
        }
    }

    /**
     * if customer is subscribed to plan in which Send rate change daily is defined as true.
     * Then Rate change date of customer should be equal to “ACTUAL_START_DATE"
     */
    private String getRateChangeDate(int itemId, Integer meterCycle) {
        PlanDTO planDTO = new PlanDAS().findPlanByItemId(itemId);
        DateTimeFormatter format = DateTimeFormat.forPattern("MM/dd/yyyy");
        if (ediTransactionBean.hasPlanSendRateChangeDaily(planDTO)) {
            LOG.debug("Rate Change Date Set as CUST_ENROLL_AGREE_DT");
            Date CustEnrollAgreeDT = (Date) customerEnrollmentWS.getMetaFieldValue(FileConstants.ACTUAL_START_DATE);
            return format.print(new DateTime(CustEnrollAgreeDT).minus(1));
        } else {
            DateTime currentDate=new DateTime();
            return format.print(ediTransactionBean.getRateChangeDate(currentDate, meterCycle, customerEnrollmentWS.getEntityId()));
        }
    }

    private Integer getCustomerEnrollmentId(EDIFileWS fileWS) {
        LOG.debug("getCustomerEnrollmentId   In Do Execute ediFileWS   " + fileWS);
        Integer customerEnrollmentId = null;
        String lifeCycleNR = findField(EnrollmentResponseParser.HDR, EnrollmentResponseParserField.LIFECYCLE_NR.toString());
        LOG.debug("Before split LifecycleNR is:  " + lifeCycleNR);
        if (!blank(lifeCycleNR) && lifeCycleNR.length() > 13) {
            lifeCycleNR = lifeCycleNR.substring(13);
        } else {
            LOG.error(EnrollmentResponseParserField.LIFECYCLE_NR.toString() + " can not be null");
            /*TODO: put exception code here regarding null enrollment id.*/
            throw new SessionInternalError("Could not found valid enrollment Id.");
        }
        LOG.debug("lifeCycleNR  after split:  " + lifeCycleNR);
        try {
            customerEnrollmentId = Integer.parseInt(lifeCycleNR);
        } catch (NumberFormatException nfe) {
            LOG.error("Could not found valid enrollment Id.");
            /*TODO: put exception code here regarding null enrollment id.*/
            throw new SessionInternalError("Could not parse enrollment Id. Value is: " + lifeCycleNR);
        }
        return customerEnrollmentId;
    }

    private EDIFileWS getOutboundFileWS(String enrollmentId) {
        EDIFileWS outboundFileWS = ediTransactionBean.getOutboundFileForCustomerEnrollment(enrollmentId, companyId, ediTypeId);
        if (outboundFileWS == null) {
            throw new SessionInternalError("No OUTBOUND File match with this file.");
        }
        return outboundFileWS;
    }

    private void checkIfFileRejected() {
//        This method reject coming file and attached OUTBOUND file if file has a CDE record with REC_TYPE "R".
        String rejectionValue = findField(EnrollmentResponseParser.CDE, EnrollmentResponseParserField.REC_TYPE.toString());
        String rejectionCode = findField(EnrollmentResponseParser.CDE, EnrollmentResponseParserField.CODE.toString());

        if(rejectionCode!=null){
            status = REJECT_STATUS;
            exceptionCode = rejectionCode;
            if (rejectionValue!=null && rejectionValue.equals("R")) {
                if (customerEnrollmentWS.getStatus().equals(CustomerEnrollmentStatus.REJECTED)) {
                    LOG.error("File has already been rejected.");
                    comment = "File has already been Rejected.";
                    throw new SessionInternalError("File has already been Rejected.");
                }
                enrollmentStatus = CustomerEnrollmentStatus.REJECTED;
                enrollmentComment = "File has been rejected by LDC. Reason: " + rejectionCode;
                outboundStatus = REJECT_STATUS;
                outboundComment = "File has been rejected. reason is: " + rejectionCode;
                LOG.debug("In EnrollmentResponseParserTask response has been rejected.  ");
                updateEnrollmentAndOutboundFile();
            }
            comment = "File has been rejected. reason is: " + rejectionCode;
            throw new SessionInternalError("File has been rejected. reason is: " + rejectionCode);
        }
    }

    private void changeCustomerEnrollmentStatus(CustomerEnrollmentStatus enrollmentStatus, String comment) {
        try {
            LOG.debug("Started change enrollment status here " + enrollmentStatus);
            customerEnrollmentWS.setStatus(enrollmentStatus);
            CustomerEnrollmentCommentWS commentWS = new CustomerEnrollmentCommentWS();
            commentWS.setComment(comment);
            commentWS.setCustomerEnrollmentWS(customerEnrollmentWS);
            commentWS.setDateCreated(TimezoneHelper.serverCurrentDate());
            List<CustomerEnrollmentCommentWS> customerEnrollmentCommentWSes = new ArrayList<CustomerEnrollmentCommentWS>(Arrays.asList(customerEnrollmentWS.getCustomerEnrollmentComments()));
            customerEnrollmentCommentWSes.add(commentWS);
            customerEnrollmentWS.setCustomerEnrollmentComments(customerEnrollmentCommentWSes.toArray(new CustomerEnrollmentCommentWS[customerEnrollmentCommentWSes.size()]));
            webServicesSessionSpringBean.createUpdateEnrollment(customerEnrollmentWS);
        } catch (Exception ex) {
            LOG.error("Facing issue while saving enrollment. ", ex);
            throw new SessionInternalError("Could not save Enrollment.");
        }
    }

    private void changeEdiFileStatus(String statusName, EDIFileWS ediFileWS) {
        LOG.debug("In EnrollmentResponseParserTask before fetching edit status  " + statusName);
        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for (EDIFileStatusWS ediStatus : ediType.getEdiStatuses()) {
            if (ediStatus.getName().equals(status)) {
                statusWS = ediStatus;
            }
        }
        ediFileWS.setEdiFileStatusWS(statusWS);
        LOG.debug("In EnrollmentResponseParserTask completing loop here:  " + statusWS);
        try {
            webServicesSessionSpringBean.saveEDIFileRecord(ediFileWS);

        } catch (SessionInternalError sie) {
            LOG.error("Unable to save edi file status " + sie);
            throw new SessionInternalError("Problem in saving outbound edi file");
        }
        LOG.debug("statusWS after save edi file is:  " + statusWS);
    }

    private boolean blank(Object o) {
        return o == null;
    }

    /*This method find rate which send on the enrollment creation time*/
    private String findRate(EDIFileWS ediFileWS){
        String rateCode=findValue(ediFileWS, EnrollmentResponseParser.MTR.toString(), EnrollmentResponseParserField.SUPPLIER_RATE_CD.toString());
        if(rateCode==null){
            throw new SessionInternalError("No rate found for customer");
        }
        return ediTransactionBean.getRateByRateCode(companyId, rateCode).stripTrailingZeros().toPlainString();
    }

    private String findValue(EDIFileWS ediFileWS, String header, String key){
        for(EDIFileRecordWS ediFileRecordWS:ediFileWS.getEDIFileRecordWSes()){
            if(ediFileRecordWS.getHeader().equals(header)){
                for(EDIFileFieldWS ediFileFieldWS:ediFileRecordWS.getEdiFileFieldWSes()){
                    if(ediFileFieldWS.getKey().equals(key)){
                        return ediFileFieldWS.getValue();
                    }
                }
            }
        }
        return null;
    }
}
