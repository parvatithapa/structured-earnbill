package com.sapienter.jbilling.server.nges.ediProcessing.task.listeners;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean;
import com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.nges.ediProcessing.task.util.MeterInvoiceReprocess;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


/**
 * Created by aman on 2/8/16.
 */
/**
 * This class will execute the reprocess of Meter and Invoice read.
 * If an original meter successfully processed then existing cancellation and replacement meter read will be reprocessed
 * If and cancellation meter read successfully process then only replacement meter read will be reprocessed
 */
public class MeterReadChunkListener implements ChunkListener {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MeterReadChunkListener.class));

    private MeterInvoiceReprocess meterInvoiceReprocess;
    private static IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
    public MeterInvoiceReprocess getMeterInvoiceReprocess() {
        return meterInvoiceReprocess;
    }

    public void setMeterInvoiceReprocess(MeterInvoiceReprocess meterInvoiceReprocess) {
        this.meterInvoiceReprocess = meterInvoiceReprocess;
    }

    @Override
    public void beforeChunk(ChunkContext chunkContext) {

    }

    @Override
    @Transactional
    public void afterChunk(ChunkContext chunkContext) {
        LOG.debug("After chunk.....");
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();

        String status = (String) stepExecution.getExecutionContext().get("FILE_STATUS");
        LOG.debug("Meter read File status : %s", status);

        LOG.debug("Status removed : " + stepExecution.getExecutionContext().get("FILE_STATUS"));
        Date startDate = (Date) stepExecution.getExecutionContext().get(MeterReadParserTask.MeterReadField.START_SERVICE_DT.toString());
        Date endDate = (Date) stepExecution.getExecutionContext().get(MeterReadParserTask.MeterReadField.END_SERVICE_DT.toString());
        String customerAccountNumber = (String) stepExecution.getExecutionContext().get(MeterReadParserTask.MeterReadField.UTILITY_CUST_ACCT_NR.toString());

        EDIFileWS ediFileWS = (EDIFileWS) stepExecution.getExecutionContext().get("ediFile");
        String recordType = (String) stepExecution.getExecutionContext().get("RECORD_TYPE");

        stepExecution.getExecutionContext().remove("FILE_STATUS");
        stepExecution.getExecutionContext().remove(MeterReadParserTask.MeterReadField.START_SERVICE_DT.toString());
        stepExecution.getExecutionContext().remove(MeterReadParserTask.MeterReadField.END_SERVICE_DT.toString());
        stepExecution.getExecutionContext().remove(MeterReadParserTask.MeterReadField.UTILITY_CUST_ACCT_NR.toString());
        stepExecution.getExecutionContext().remove("FILE_ID");
        stepExecution.getExecutionContext().remove("RECORD_TYPE");

        Integer entityId = stepExecution.getJobParameters().getLong("companyId").intValue();
        try {

            LOG.debug("Reprocessing Cancellation and Rebill meter read");

            Integer meterReadTypeId = ediTransactionBean.getEDITypeId(entityId, FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
            LOG.debug("Meter read type id : "+meterReadTypeId);

            EDIFileStatusWS doneStatus = ediTransactionBean.getEDIFileStatus(meterReadTypeId, "Done");
            if(doneStatus==null){
                LOG.error("Done status not found in the meter read");
                return;
            }

            LOG.debug("Meter Read Type : " + recordType);

            /*If meter read is original and its status is Done then reprocesses the cancellation and replacement meter read */
            if(recordType!=null && recordType.equals("00")){
                LOG.debug("Original Meter read status : " + ediFileWS.getEdiFileStatusWS().getName());
                if(ediFileWS.getEdiFileStatusWS().getName().equals("Done")){
                    meterInvoiceReprocess.reprocessCancellationAndReplacementMeterRead(entityId, customerAccountNumber, startDate, endDate, doneStatus, meterReadTypeId);

                }
            }

            /*If meter read is cancellation and its status is EXP001 then reprocess the existing replacement meter read */
            if(recordType!=null && recordType.equals("01")){
                LOG.debug("Cancellation Meter read status : " + ediFileWS.getEdiFileStatusWS().getName());
                if(ediFileWS.getEdiFileStatusWS().getName().equals("EXP001")){
                    meterInvoiceReprocess.reprocessReplacementMeterRead(entityId, customerAccountNumber, startDate, endDate, doneStatus, meterReadTypeId);
                }
            }

            LOG.debug("Reprocess invoice read file if any exist which is waiting for 867.");
            LOG.debug("Meter read data : %d, %s, %s, %s", entityId, startDate, endDate, customerAccountNumber);
            meterInvoiceReprocess.processForInvoiceRead(entityId, startDate, endDate, customerAccountNumber);
        } catch (Exception e) {
            LOG.error("Exception occurred while processing Meter/Invoice read.", e);
        }
    }

    @Override
    public void afterChunkError(ChunkContext chunkContext) {
        LOG.debug("After afterChunkError.....");
        return;
    }
}