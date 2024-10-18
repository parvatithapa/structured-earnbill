package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.earlyTermination.task.CustomerTerminationTask;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.batch.core.*;

import java.io.File;
import java.util.*;


/**
 * Task to parse response for ESCO initiated termination.
 * The file format is defined by 814_Esco_Customer_Termination
 */
public class ParseEscoTerminationResponseTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ParseEscoTerminationResponseTask.class));

    //the records we care about
    public static enum EscoTermination implements FileStructure {
        KEY, HDR, NME, ACT;
    }

    //the field we care about in the header record
    public static enum HeaderRecord {
        LIFECYCLE_NR
    }

    //the fields we care about in the account record
    public static enum AccountRecord {
        LINE_NR,
        ACCEPT_REJ_FLAG,
        UTILITY_CUST_ACCT_NR,
        BILL_DELIVER,
        BILL_CALC,
        READ_CONSUMPTION,
        CUST_LIFE_SUPPORT,
        PERCENT_PARTICPATE
    }

    //Value of ACT->ACCEPT_REJ_FLAG
    private static final String ACCEP_REJ_IS_ACCEPT = "A";
    private static final String ACCEP_REJ_IS_REJ = "R";

    //possible values of the edi file
    protected static final ParameterDescription STATUS_NAME_ACCEPTED =
            new ParameterDescription("accepted_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription STATUS_NAME_REJECTED =
            new ParameterDescription("rejected_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription STATUS_NAME_INVALID_DATA =
            new ParameterDescription("invalid_status", true, ParameterDescription.Type.STR);

    {
        descriptions.add(STATUS_NAME_ACCEPTED);
        descriptions.add(STATUS_NAME_INVALID_DATA);
        descriptions.add(STATUS_NAME_REJECTED);
    }


    //Batch processor instance variable needs to initialize

    IWebServicesSessionBean webServicesSessionSpringBean;
    IEDITransactionBean ediTransactionBean;

    private String STATUS_ACCEPTED;
    private String STATUS_REJECTED;
    private String STATUS_INVALID_DATA;

    private Integer ediTypeId;

    private int companyId;
    private String exceptionCode;
    private String status;
    private String comment;
    private Date terminationDate;

    protected DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd");

    @Override
    public String getTaskName() {
        return "Esco Termination parser: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_ESCO_TERMINATION_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

        LOG.debug("Execute MeadReadParserTask  plugin.");

        EDI_TYPE_ID = Integer.valueOf(companyMetaFieldValueMap.get(FileConstants.ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME).toString());
        if(EDI_TYPE_ID == null) {
            throw new SessionInternalError("EDI type id not valid : " + EDI_TYPE_ID);
        }

        EDITypeWS ediType = null;

        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null) {
            throw new SessionInternalError("EDI type id not found : " + EDI_TYPE_ID);
        }

        jobParams.put("STATUS_ACCEPTED", new JobParameter(parameters.get(STATUS_NAME_ACCEPTED.getName())));
        jobParams.put("STATUS_REJECTED", new JobParameter(parameters.get(STATUS_NAME_REJECTED.getName())));
        jobParams.put("STATUS_INVALID_DATA", new JobParameter(parameters.get(STATUS_NAME_INVALID_DATA.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("EDI File Invoice Item Processor : Before Step");
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();

        STATUS_ACCEPTED = jobParameters.getString("STATUS_ACCEPTED");
        STATUS_REJECTED = jobParameters.getString("STATUS_REJECTED");
        STATUS_INVALID_DATA = jobParameters.getString("STATUS_INVALID_DATA");
        companyId = jobParameters.getLong("companyId").intValue();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    private void clearData(){

        comment=null;
        userDTO=null;
        customerAccountNumber=null;
        status=null;
        terminationDate=null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        LOG.debug("Esco Termination Task Process Method");
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;
                processEscoTerminationFile();
            }
        } catch (Exception ex) {
            LOG.error(ex);
            if(status == null) {
                status = STATUS_REJECTED;
            }
            comment= EDITransactionHelper.getExceptionMessage(ex);
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for(EDIFileStatusWS ediStatus : ediType.getEdiStatuses()){
            if(ediStatus.getName().equals(status)){
                statusWS = ediStatus;
                break;
            }
        }

        ediFileWS.setEdiFileStatusWS(statusWS);
        ediFileWS.setComment(comment);
        if(exceptionCode!=null)ediFileWS.setExceptionCode(exceptionCode);

        bindFields(ediFileWS);
        clearData();
        return ediFileWS;
    }

    /**
     * Delete the edi file from the temp folder
     * @param ediFileWS
     */
    private void deleteEdiFile(EDIFileWS ediFileWS) {
        FileFormat fileFormat = FileFormat.getFileFormat(ediTypeId);
        File file = new File(FileConstants.getEDITypePath(fileFormat.getEdiTypeDTO().getEntity().getId(), fileFormat.getEdiTypeDTO().getPath(), FileConstants.INBOUND_PATH) + File.separator + ediFileWS.getName());
        if(file.exists()) {
            file.delete();
        }
    }

    /**
     * Extract the values from incoming records and process the user termination.
     */
    public void processEscoTerminationFile(){
        //We only care about the HDR and ACT records
        List<String> maidenRecords = new LinkedList<>();
        maidenRecords.add(EscoTermination.HDR.toString());
        maidenRecords.add(EscoTermination.ACT.toString());
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());


        //Check that the customer nr matches the one in the sent file
        LOG.info("Validating is customer exist");
        customerAccountNumber = findField(EscoTermination.ACT, AccountRecord.UTILITY_CUST_ACCT_NR.toString(), true);
        userDTO = ediTransactionBean.findUserByAccountNumber(companyId, FileConstants.UTILITY_CUST_ACCT_NR, customerAccountNumber);
        if(userDTO==null){
            throw new SessionInternalError("No customer found for account number "+customerAccountNumber);
        }

        //Validating meter read record
        validateEscoTerminationFile();

        UserBL userBL = new UserBL(userDTO);
        UserWS userWS = userBL.getUserWS();

        String acceptRejFlag = findField( EscoTermination.ACT, AccountRecord.ACCEPT_REJ_FLAG.name(), true);
        //if the termination was ACCEPTED by the LDC
        if(ACCEP_REJ_IS_ACCEPT.equals(acceptRejFlag)) {
            if(isUserAlreadyTerminatedOrDropped(userWS)) {
                exceptionCode = FileConstants.EXCEPTION_ESCO_TERMINATION_REQUEST_DATA;
                comment = "User already terminated.";
            } else {
                //Traer desde el outbound el field END_SERVICE_DT
                addMetaField(userWS, FileConstants.CUSTOMER_TERMINATION_DATE_METAFIELD , terminationDate);
                AbstractScheduledTransactionProcessor.updateTerminationMetaField(userWS, FileConstants.TERMINATION_PROCESSING, terminationDate);
            }
            status = STATUS_ACCEPTED;
        //if the termination was REJECTED by the LDC
        } else if(ACCEP_REJ_IS_REJ.equals(acceptRejFlag)) {
            if(!isUserAlreadyTerminatedOrDropped(userWS)) {
                AbstractScheduledTransactionProcessor.updateTerminationMetaField(userWS, FileConstants.TERMINATION_ESCO_REJECTED, null);
            } else {
                comment = "User already terminated.";
            }
            exceptionCode = FileConstants.EXCEPTION_ESCO_TERMINATION_REJECTED;
            status = STATUS_ACCEPTED;
        //unknown termination flag
        } else {
            exceptionCode=FileConstants.EXCEPTION_ESCO_TERMINATION_REQUEST_DATA;
            status= STATUS_INVALID_DATA;
            throw new SessionInternalError("Unknown Accept/Reject Flag '"+acceptRejFlag+"' for file "+ediFile.getId());
        }

        webServicesSessionSpringBean.updateUserWithCompanyId(userWS, companyId);

        try {
            userWS.close();
        } catch (Exception e) {
            LOG.debug("Exception while clean up: "+e);
        }

    }

    /**
     * Validate the termination file
     */
    private void validateEscoTerminationFile() {

        LOG.info("Validating Meter Read record");
        EDIFileWS fileWS = null;

        //Check Transaction number match the one of the sent file
        String transRefNr = findField(EscoTermination.HDR, HeaderRecord.LIFECYCLE_NR.toString());
        LOG.debug("%s value found : %s", HeaderRecord.LIFECYCLE_NR, transRefNr);
        try {
            fileWS = ediTransactionBean.getOutboundFileForCustomerEnrollment(transRefNr, companyId, ediTypeId);
        } catch (Exception e) {
            status=STATUS_INVALID_DATA;
            exceptionCode = FileConstants.EXCEPTION_ESCO_TERMINATION_REQUEST_DATA;
            throw new SessionInternalError("Unable to match file to outbound file.");
        }

        String terminationStr = fileWS.findField(CustomerTerminationTask.CustomerTermination.DTE.name(), CustomerTerminationTask.CustomerTerminationField.END_SERVICE_DT.toString());

        try {

            if(!StringUtils.isBlank(terminationStr)){
                terminationDate = dateFormat.parseDateTime(terminationStr).toDate();
            }
        } catch (IllegalArgumentException e) {
            throw new SessionInternalError("Unable to Parse the value \"" + terminationStr + "\" of Field " + CustomerTerminationTask.CustomerTerminationField.END_SERVICE_DT.toString() + "in to Date");
        }

        //Check that the customer nr matches the one in the sent file
        LOG.info("Validating is customer exist");
        customerAccountNumber = findField(EscoTermination.ACT, AccountRecord.UTILITY_CUST_ACCT_NR.toString());
        if(customerAccountNumber==null || !customerAccountNumber.equals(fileWS.findField(EscoTermination.ACT.name(), AccountRecord.UTILITY_CUST_ACCT_NR.name()))) {
            status=STATUS_INVALID_DATA;
            LOG.error("Account Id not found in Esco Termination file. EDI File Id :" + ediFile.getId());
            exceptionCode = FileConstants.EXCEPTION_ESCO_TERMINATION_REQUEST_DATA;
            throw new SessionInternalError("Customer with Account Id :" + customerAccountNumber + " does not exist");
        }
    }

    public void  updateFileStatus(EDIFileDTO ediFileDTO, String statusName){
        webServicesSessionSpringBean.updateEDIFileStatus(ediFileDTO.getId(), statusName, null);
    }

}
