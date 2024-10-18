package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.batch.WrappedObjectLine;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.batch.item.validator.ValidationException;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Taimoor Choudhary on 3/28/18.
 */
public class PlanPriceFileReader implements ItemStreamReader, ItemReader, StepExecutionListener {

    private SingleItemPeekableItemReader<FieldSet> delegate;
    private int entityId;
    private boolean recordFinished;
    private boolean processNextItem;
    private boolean errorLogged;
    private String planRow;
    private String currentRow;
    private String fileName;
    private PlanFileItem planFileItem;
    private WrappedObjectLine<PlanFileItem> wrappedObjectLinews = null;
    private List<PlanImportConstants.ColumnIdentifier> attributesRead = null;

    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ResourceAwareItemWriterItemStream errorWriter;

    public void setErrorWriter(ResourceAwareItemWriterItemStream errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        executionContext    = stepExecution.getJobExecution().getExecutionContext();
        entityId            = jobParameters.getLong(PlanImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        attributesRead      = null;
        processNextItem     = true;
        fileName = FilenameUtils.getBaseName(jobParameters.getString(PlanImportConstants.JOB_PARAM_INPUT_FILE));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public Object read() throws Exception{

        recordFinished = false;

        try {
            while (!recordFinished) {

                if (peakNextItem() == null) {
                    logger.debug("Bulk Upload - Plan Prices: FILE FINISHED - {}", fileName);
                    recordFinished = true;
                    continue;
                }

                if (!processNextItem) {
                    if (!peakNextItem().getLine().isEmpty() &&
                            !peakNextItem().getObject().readString(0).equals(PlanImportConstants.ColumnIdentifier.PLAN.toString())) {
                        readItem();
                        continue;
                    }
                }

                try {

                    WrappedObjectLine<FieldSet> fileLine = readItem();

                    if(!fileLine.getLine().isEmpty()) {
                        process(fileLine);
                    }

                } catch (ValidationException validationException) {
                    logger.error("Bulk Upload - Plan Prices: Plan FINISHED with ERROR - {}", fileName);
                    processNextItem = false;
                    wrappedObjectLinews = null;
                    throw validationException;
                }
                //If the next line identifier is PLAN, then that means the next Plan will be starting
                if (peakNextItem() == null || (!peakNextItem().getLine().isEmpty() &&
                        peakNextItem().getObject().readString(0).equals(PlanImportConstants.ColumnIdentifier.PLAN.toString()))) {

                    logger.debug("Bulk Upload - Plan Prices: RECORD FINISHED - {}", fileName);

                    if (null != wrappedObjectLinews) {
                        processNextItem = false;
                        recordFinished = true;
                    }
                }
            }
            if (wrappedObjectLinews == null) {
                return null;
            }

            try {
                // Validate whether required rows has been read or not
                PlanPriceFileValidator.validatePlanRows(attributesRead);
            } catch (ValidationException validationException) {
                logger.error("Bulk Upload - Plan Prices: Plan FINISHED with ERROR - {}", fileName);

                writeLineToErrorFile(planRow, validationException.getMessage());

                wrappedObjectLinews = null;
                attributesRead.clear();
                attributesRead = null;
                throw validationException;
            }

            WrappedObjectLine<PlanFileItem> result = wrappedObjectLinews;
            wrappedObjectLinews = null;
            planFileItem = null;
            return result;
        } catch (Exception exception) {

            String exceptionMessage = exception.getMessage();

            logger.error("Bulk Upload - Plan Prices: Price FINISHED with ERROR - {}", fileName);

            if(exceptionMessage.contains("Cannot access columns by name without meta data"))
            {
                logger.error(String.format("Bulk Upload - Plan Prices - Row contains unsupported tags/info: %s", currentRow),
                        exceptionMessage);
                exceptionMessage = "Invalid Row format";
            }
            writeLineToErrorFile(currentRow, exceptionMessage);

            logger.error(String.format("Bulk Upload - Plan Prices: %s", currentRow), exceptionMessage);

            processNextItem = false;
            wrappedObjectLinews = null;
            attributesRead = null;
            throw new ValidationException(exception.getMessage());
        }
    }

    /**
     * Process individual rows
     * @param wrappedObjectLine
     * @throws Exception
     */
    private void process(WrappedObjectLine<FieldSet> wrappedObjectLine) throws Exception{
        // finish processing if we hit the end of file
        if (wrappedObjectLine == null) {
            logger.debug("Bulk Upload - Plan Price: FILE FINISHED - {}", fileName);
            recordFinished = true;
            return;
        }

        try{
            FieldSet fieldSet = wrappedObjectLine.getObject();
            currentRow = wrappedObjectLine.getLineNr() + "," + wrappedObjectLine.getLine();

            String lineId = fieldSet.readString(PlanImportConstants.TYPE_COL);

            //If the line identifier is PLAN, then that means new plan is starting
            if (lineId.equals(PlanImportConstants.ColumnIdentifier.PLAN.toString())) {

                planRow = wrappedObjectLine.getLineNr() + "," + wrappedObjectLine.getLine();
                logger.debug("Bulk Upload - Plan Price: STARTING NEW PLAN from file - {}", fileName);
                logger.debug("Bulk Upload - Plan Price: Plan Basic Details - {}", planRow);

                incrementTotalCount();

                errorLogged = false;
                processNextItem = true;

                attributesRead = new ArrayList<>();
                planFileItem = new PlanFileItem();
                wrappedObjectLinews = new WrappedObjectLine<>(wrappedObjectLine.getLineNr(), wrappedObjectLine.getLine(), planFileItem);

                attributesRead.add(PlanImportConstants.ColumnIdentifier.PLAN);

                // Process PLAN details row
                PlanUploadProcessor.processPlanFields(fieldSet, this.planFileItem, this.entityId);

            }else if(lineId.equals(PlanImportConstants.ColumnIdentifier.FUP.toString())){

                // Process Free Usage Pool rows in PLAN
                PlanUploadProcessor.processPlanUsagePoolFields(fieldSet, this.planFileItem, this.entityId);

                // Indicate that the Plan FUP row has been read once as incoming parameter
                if(!attributesRead.contains(PlanImportConstants.ColumnIdentifier.FUP)) {
                    attributesRead.add(PlanImportConstants.ColumnIdentifier.FUP);
                }
            }else if(lineId.equals(PlanImportConstants.ColumnIdentifier.ITEM.toString())){

                // Process Product rows in PLAN
                PlanUploadProcessor.processPlanItemFields(fieldSet, this.planFileItem, this.entityId);

                // Indicate that the Plan Item has been read once as incoming parameter
                if(!attributesRead.contains(PlanImportConstants.ColumnIdentifier.ITEM)) {
                    attributesRead.add(PlanImportConstants.ColumnIdentifier.ITEM);
                }
            }else if(lineId.equals(PlanImportConstants.ColumnIdentifier.FLAT.toString())){

                PlanUploadProcessor.processFlatPriceModel(fieldSet, this.planFileItem);
            }else if(lineId.equals(PlanImportConstants.ColumnIdentifier.TIER.toString())){

                PlanUploadProcessor.processTierPriceModel(fieldSet, this.planFileItem);
            }

        }catch (ValidationException validationException){
            writeLineToErrorFile(wrappedObjectLine, validationException.getMessage());
            throw validationException;
        }
    }

    public void setDelegate(SingleItemPeekableItemReader<FieldSet> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    @Override
    public void open(ExecutionContext arg0) throws ItemStreamException {
        delegate.open(arg0);
    }

    @Override
    public void update(ExecutionContext arg0) throws ItemStreamException {
        delegate.update(arg0);
    }

    private void writeLineToErrorFile(WrappedObjectLine<FieldSet> item, String message) throws Exception {
        if(errorLogged) return;

        errorLogged = true;
        incrementErrorCount();

        errorWriter.write(Arrays.asList("Line Number," + (Arrays.toString(item.getObject().getNames())).replaceAll("\\[", "").replaceAll("\\]","") + ",Error Message"));
        errorWriter.write(Arrays.asList(item.getLineNr() + "," +item.getLine() + ',' + message));
    }

    private void writeLineToErrorFile(String row, String message) throws Exception {
        if(errorLogged) return;

        errorLogged = true;
        incrementErrorCount();

        errorWriter.write(Arrays.asList("Line Number,Error in following row"));
        errorWriter.write(Arrays.asList(row + "," + message));
    }

    private WrappedObjectLine<FieldSet> readItem() throws  Exception {
        return (WrappedObjectLine<FieldSet>) this.delegate.read();
    }

    private WrappedObjectLine<FieldSet> peakNextItem() throws  Exception {
        return (WrappedObjectLine<FieldSet>) this.delegate.peek();
    }

    /**
     * increment the error line count
     */
    private void incrementErrorCount() {
        int cnt = executionContext.getInt(PlanImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(PlanImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt + 1);
    }

    private void incrementTotalCount() {
        int cnt = executionContext.getInt(PlanImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0);
        executionContext.putInt(PlanImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, cnt+1);
    }
}
