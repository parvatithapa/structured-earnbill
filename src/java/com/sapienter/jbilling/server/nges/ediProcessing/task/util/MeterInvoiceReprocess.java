package com.sapienter.jbilling.server.nges.ediProcessing.task.util;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.invoiceRead.InvoiceReadTask;
import com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aman on 29/7/16.
 */
/*
* It should process 810 file which are suspended in the lack of 867 and now when we are processing the one,
* it should find the 810 which has same period and error status.
* */
public class MeterInvoiceReprocess {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MeterInvoiceReprocess.class));
    private static IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
    private static IWebServicesSessionBean iWebServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    Integer meterReadTypeId;
    protected static DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd");

    /**
     * This method will search for invoice read and change the status of file which triggers an internal event plugin
     * named UpdateEDIStatusProcessTask. It will reprocess the file based on its status.
     * It will not exception as it should not affect the meter read file processing if it got issue in invoice read processing.
     */
    public void processForInvoiceRead(Integer entityId, Date activeSince, Date activeUntil, String customerAccountNumber) {

        LOG.debug("In Meter Invoice Reprocess!");
        List<String> statues= Arrays.asList("Invalid File");
        // Find invoice read edi type from company level meta field.
        Integer invoiceReadEDITypeId = ediTransactionBean.getEDITypeId(entityId, FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME);
        if (invoiceReadEDITypeId == null) {
            LOG.error("Invoice read edi type id is not found at company level meta field for company id : %d", entityId);
            return;
        }



        EDIFileStatusWS statusWS = ediTransactionBean.getEDIFileStatus(invoiceReadEDITypeId, "Accepted");
        if (statusWS == null) {
            LOG.error("No status found by name Accepted in edi type : %d", invoiceReadEDITypeId);
            return;
        }

        // Update the status and UpdateEDIStatusProcessTask will pick it up and try to reprocess it.
        EDIFileWS invoiceReadWS=findExistingMeterReads(entityId,"INVOICE_PURPOSE_CD", "00", customerAccountNumber, activeSince, activeUntil, invoiceReadEDITypeId);
        if(invoiceReadWS==null){
            return;
        }

        try {
            LOG.debug("Processing invoice read file : %d", invoiceReadWS.getId());
            if(!statues.contains(invoiceReadWS.getEdiFileStatusWS().getName())){
                return;
            }
            iWebServicesSessionBean.updateEDIStatus(invoiceReadWS, statusWS, false);
            LOG.debug("Processed invoice read file : %d", invoiceReadWS.getId());
        } catch (SessionInternalError e) {
            LOG.error("Exception occurred while processing Invoice read " + invoiceReadWS.getId() + " for meter read.", e);
            return;
        }
    }

    /**
     * This method  reprocess the existing cancellation and replacement meter reads which are in the hold state.
     * For reporcessing of cancellation meter read original meter read is mandaory and for reprocessing of replacement meter read,
     * cancellation and original meter reads are mandatory
     * @param entityId companyId
     * @param customerAccountNumber customer utility account number
     * @param activeSince meter read should have this start date
     * @param activeUntil meter read should have this end date
     */
    public void reprocessCancellationAndReplacementMeterRead(Integer entityId, String customerAccountNumber,Date activeSince, Date activeUntil, EDIFileStatusWS doneStatus, Integer meterReadTypeId){

        LOG.debug("Reprocessing Cancellation and Rebill meter read");
        meterReadTypeId = meterReadTypeId;
        String recordTypeKey=FileConstants.METER_READ_RECORD_TYPE;

        EDIFileWS cancellationMeterRead=findExistingMeterReads(entityId,recordTypeKey, "01", customerAccountNumber, activeSince, activeUntil, meterReadTypeId);
        LOG.debug("cancellation Meter Read : "+cancellationMeterRead);
        if(cancellationMeterRead==null){
            return;
        }

        LOG.debug("Reprocessing the Cancellation meter read");
        iWebServicesSessionBean.updateEDIStatus(cancellationMeterRead, doneStatus, false);
        cancellationMeterRead=iWebServicesSessionBean.getEDIFileById(cancellationMeterRead.getId());
        LOG.debug("After reprocess cancellation meter read status "+ cancellationMeterRead.getEdiFileStatusWS().getName());
        if(!cancellationMeterRead.getEdiFileStatusWS().getName().equals("EXP001")){
            return;
        }

        /*Reprocessing replacement read*/
        reprocessReplacementMeterRead(entityId, customerAccountNumber, activeSince, activeUntil, doneStatus, meterReadTypeId);
    }

    /**
     * This method  reprocess the existing replacement meter reads which are in the hold state.
     * For reporcessing the replacement meter read cancellation meter is mandatory
     * @param entityId companyId
     * @param customerAccountNumber customer utility account number
     * @param activeSince meter read should have this start date
     * @param activeUntil meter read should have this end date
     */
    public void reprocessReplacementMeterRead(Integer entityId, String customerAccountNumber,Date activeSince, Date activeUntil, EDIFileStatusWS doneStatus, Integer meterReadTypeId){

        EDIFileWS replacementMeterRead=findExistingMeterReads(entityId,FileConstants.METER_READ_RECORD_TYPE, "05", customerAccountNumber, activeSince, activeUntil, meterReadTypeId);
        LOG.debug("replacement Meter Read : "+replacementMeterRead);
        if(replacementMeterRead==null){
            return;
        }

        LOG.debug("Reprocessing the Rebill meter read");
        iWebServicesSessionBean.updateEDIStatus(replacementMeterRead, doneStatus, false);
    }

    /**
     * This method return the existing original, cancellation and replacement which are in the hold state
     * @param entityId company id
     * @param recordType its value will be 00(Original), 01(Cancellation) and 05 (Rebill)
     * @param customerAccountNumber
     * @param startDate read start date
     * @param endDate  read end date
     * @return
     */
    private EDIFileWS findExistingMeterReads(Integer entityId, String recordTypeKey,  String recordType, String customerAccountNumber, Date startDate, Date endDate, Integer ediTypeId){
        List<String> reprocessStatus= Arrays.asList("EXP002", "Invalid Data", "Invalid File");
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", ediTypeId));
        conjunction.add(Restrictions.eq("entity.id", entityId));
        conjunction.add(Restrictions.eq("type", TransactionType.INBOUND));
        conjunction.add(Restrictions.in("status.name", reprocessStatus));
        conjunction.add(Restrictions.eq("utilityAccountNumber", customerAccountNumber));
        conjunction.add(Restrictions.eq("startDate", startDate));
        conjunction.add(Restrictions.eq("endDate", endDate));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", recordTypeKey));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", recordType));

        List<EDIFileDTO> ediFileDTOList= new EDIFileDAS().findEDIFiles(conjunction);
        if(ediFileDTOList.size()>0){
            return new EDIFileBL(ediFileDTOList.get(0)).getWS();
        }
        return null;
    }
}
