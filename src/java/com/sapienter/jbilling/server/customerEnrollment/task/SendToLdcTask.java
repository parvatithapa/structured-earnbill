package com.sapienter.jbilling.server.customerEnrollment.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by vivek on 9/9/15.
 */
public class SendToLdcTask extends AbstractCronTask {

    private static final String ERROR_FILE_EXTENSION = "error";
    private static final String PARAM_LOCAL_PATH = "local_path";
    private static final String PROCESSED = "Processed";


    private static File TARGET_FOLDER;
    //    private static Integer ediTypeId;
    private static String sourceStatus;
    private static String successStatus;
    private static String errorStatus;
    private EDIFileBL fileBL;
    EDIFileDAS ediFileDAS;
    IWebServicesSessionBean webServicesSessionSpringBean;
    IOrderSessionBean orderSessionBean;
    IEDITransactionBean ediTransactionBean;

    protected Map<String, Object> companyMetaFieldValueMap = null;
    private List<Integer> EDI_TYPE_IDS = new ArrayList<Integer>();

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SendToLdcTask.class));
    protected static final ParameterDescription SOURCE_STATUS_ID =
            new ParameterDescription("source_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription SUCCESS_STATUS_ID =
            new ParameterDescription("success_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription ERROR_STATUS_ID =
            new ParameterDescription("error_status", true, ParameterDescription.Type.STR);

    {
//        descriptions.add(EDI_TYPE_ID);
        descriptions.add(SOURCE_STATUS_ID);
        descriptions.add(SUCCESS_STATUS_ID);
        descriptions.add(ERROR_STATUS_ID);
    }

    public SendToLdcTask() {
        setUseTransaction(true);
    }

    public String getTaskName() {
        return "Sending enrollment file, entity Id: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Try to execute cron job for send enrollment task");
        super._init(context);
        fileBL = new EDIFileBL();
        ediFileDAS = new EDIFileDAS();
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);

//            Get metafield values from customer metafield in companyMetaFieldValueMap
        setMetaFieldValues();
//            Set values in local variable.
        setCommonParameter();

        sourceStatus = StringUtils.trimToNull(parameters.get(SOURCE_STATUS_ID.getName()));
        errorStatus = StringUtils.trimToNull(parameters.get(ERROR_STATUS_ID.getName()));
        successStatus = StringUtils.trimToNull(parameters.get(SUCCESS_STATUS_ID.getName()));

        for (Integer EDI_TYPE_ID : EDI_TYPE_IDS) {
            sendToLDC(EDI_TYPE_ID);
        }
    }

    private void sendToLDC(Integer EDI_TYPE_ID) throws JobExecutionException {
        EDITypeWS ediTypeWS = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);

        if (ediTypeWS == null) {
            throw new SessionInternalError("EdI type must not be null");
        }

        EDIFileStatusWS successStatusWS = null;
        for (EDIFileStatusWS ediFileStatus : ediTypeWS.getEdiStatuses()) {
            if (ediFileStatus.getName().equals(successStatus)) {
                successStatusWS = ediFileStatus;
            }
        }
        if (successStatusWS == null) {
            throw new SessionInternalError("Success status must not be null");
        }
        LOG.debug("sourceStatus  is " + sourceStatus);

        List<EDIFileDTO> ediFileDTOs = ediFileDAS.getEDIFilesUsingStatus(getEntityId(), EDI_TYPE_ID, sourceStatus);
        List<EDIFileWS> ediFileWSes = new ArrayList<EDIFileWS>();
        LOG.debug("ediFileDTOs  is:  " + ediFileDTOs);

        try {
            String filePath = FileConstants.getEDITypePath(ediTypeWS.getEntityId(), ediTypeWS.getPath(), FileConstants.OUTBOUND_PATH);
            LOG.debug("File path is:  " + filePath);
            File sourceFolder = new File(filePath);
            String targetPath = ediTransactionBean.getEDICommunicationPath(getEntityId(), TransactionType.OUTBOUND);
            LOG.debug("Target path is:  " + targetPath);
            if (StringUtils.trimToNull(targetPath) != null) {
                TARGET_FOLDER = new File(targetPath);
                if (!TARGET_FOLDER.exists()) {
                    TARGET_FOLDER.mkdir();
                }
            } else {
                throw new JobExecutionException("Remote path does not exist. Please provide correct remote path");
            }

            for (EDIFileDTO ediFileDTO : ediFileDTOs) {
                EDIFileWS ediFileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());
                ediFileWSes.add(ediFileWS);
                storeFilesInTargetFolder(ediFileWS.getName(), sourceFolder, TARGET_FOLDER);
            }
        } catch (NumberFormatException nfe) {
            LOG.debug("Problem in convert String to integer here " + nfe);
            throw new JobExecutionException(nfe);
        } catch (Exception ex) {
            LOG.debug("File does not defined correctly.  " + ex);
            throw new JobExecutionException(ex);
        }

        LOG.debug("successStatus  is " + successStatus);
        for (EDIFileWS successFile : ediFileWSes) {
            LOG.debug("This is success files  " + successFile + " with success status  " + successStatusWS);
            LOG.debug("File ws with new Status:  " + successFile.getEdiFileStatusWS().getName());
            webServicesSessionSpringBean.updateEDIStatus(successFile, successStatusWS, true);
        }
    }

    private void storeFilesInTargetFolder(String ediFileName, File sourceFolder, File targetFolder) throws JobExecutionException {
//        Copy edit file in a separate folder which have success status id.
        try {
            File sourceFile = new File(sourceFolder + "/" + ediFileName);
            File targetFile = new File(targetFolder + "/" + ediFileName);
            FileUtils.copyFile(sourceFile, targetFile);
        } catch (IOException ioex) {
            LOG.error("Can not copy file. " + ioex);
            throw new JobExecutionException(ioex);
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
        setAvailableParameter();
        LOG.debug("EDI_TYPE_IDS  is: " + EDI_TYPE_IDS);
    }

    private void setAvailableParameter() {
        String[] parameters = new String[]{FileConstants.ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME,
                FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME,
                FileConstants.TERMINATION_EDI_TYPE_ID_META_FIELD_NAME,
                FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME,
                FileConstants.ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME};
        for (String parameter : parameters) {
            Object val = companyMetaFieldValueMap.get(parameter);
            if (val != null) {
                LOG.debug("key: " + parameter + ", value: " + val);
                EDI_TYPE_IDS.add((Integer) val);
            }
        }
    }
}
