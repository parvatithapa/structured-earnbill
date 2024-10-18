package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.batch.WrappedObjectLine;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
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
import org.springframework.util.Assert;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Taimoor Choudhary on 12/11/17.
 */
public class ProductAccountPriceFileReader implements ItemStreamReader, ItemReader, StepExecutionListener {
    private SingleItemPeekableItemReader<FieldSet> delegate;
    private Integer entityId;
    private Integer userId;
    private List<ProductImportConstants.ColumnIdentifier> attributesRead = null;

    private boolean recordFinished;
    private boolean processNextItem;
    private boolean errorLogged;
    private String productRow;
    private String currentRow;
    Integer currencyId = null;
    PriceModelWS priceModel= null;
    WrappedObjectLine<ProductFileItem> wrappedObjectLinews = null;
    ProductFileItem fileItem = null;
    private String fileName;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;

    /**
     * error file writer
     */
    private ResourceAwareItemWriterItemStream errorWriter;

    private ProductPriceFileValidator validator;

    public void setValidator(ProductPriceFileValidator validator) {
        this.validator = validator;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        executionContext    = stepExecution.getJobExecution().getExecutionContext();
        entityId            = jobParameters.getLong(ProductImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        userId              = jobParameters.getLong(ProductImportConstants.JOB_PARAM_USER_ID).intValue();
        attributesRead      = null;
        processNextItem     = true;
        fileName = FilenameUtils.getBaseName(jobParameters.getString(ProductImportConstants.JOB_PARAM_INPUT_FILE));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public WrappedObjectLine<ProductFileItem> read() throws Exception {
        recordFinished = false;

        try {
            while (!recordFinished) {

                if (peakNextItem() == null) {
                    logger.debug("Bulk Upload - Account Type: FILE FINISHED - {}", fileName);
                    recordFinished = true;
                    continue;
                }

                if (!processNextItem) {
                    if (!peakNextItem().getLine().isEmpty() &&
                                    !peakNextItem().getObject().readString(0).equals(ProductImportConstants.ColumnIdentifier.PRICE.toString())) {
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
                    logger.error("Bulk Upload - Account Type: Price FINISHED with ERROR - {}", fileName);
                    processNextItem = false;
                    wrappedObjectLinews = null;
                    throw validationException;
                }
                //If the next line identifier is PRICE, then that means the next product will be starting and current product is completed
                if (peakNextItem() == null || (!peakNextItem().getLine().isEmpty() &&
                                peakNextItem().getObject().readString(0).equals(ProductImportConstants.ColumnIdentifier.PRICE.toString()))) {

                    logger.debug("Bulk Upload - Account Type: RECORD FINISHED - {}", fileName);

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
                validator.validateProductRows(attributesRead);
            } catch (ValidationException validationException) {
                logger.error("Bulk Upload - Account Type: Price FINISHED with ERROR - {}", fileName);

                writeLineToErrorFile(productRow, validationException.getMessage());

                wrappedObjectLinews = null;
                attributesRead.clear();
                attributesRead = null;
                throw validationException;
            }

            WrappedObjectLine<ProductFileItem> result = wrappedObjectLinews;
            wrappedObjectLinews = null;
            fileItem = null;
            priceModel = null;
            return result;
        } catch (Exception exception) {

            String exceptionMessage = exception.getMessage();

            logger.error("Bulk Upload - Account Type: Price FINISHED with ERROR - {}", fileName);

            if(exceptionMessage.contains("Cannot access columns by name without meta data"))
            {
                logger.error(String.format("Bulk Upload - Account Type - Row contains unsupported tags/info: %s", currentRow),
                        exceptionMessage);
                exceptionMessage = "Invalid Row format";
            }
            writeLineToErrorFile(currentRow, exceptionMessage);

            logger.error(String.format("Bulk Upload - Account Type: %s", currentRow), exceptionMessage);

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
    private void process(WrappedObjectLine<FieldSet> wrappedObjectLine) throws Exception {

        // finish processing if we hit the end of file
        if (wrappedObjectLine == null) {
            logger.debug("Bulk Upload - Account Type: FILE FINISHED - {}", fileName);
            recordFinished = true;
            return;
        }
        try {
            FieldSet fieldSet = wrappedObjectLine.getObject();
            currentRow = wrappedObjectLine.getLineNr() + "," + wrappedObjectLine.getLine();

            ProductUploadFileItemProcessor.Validator = validator;

            String lineId = fieldSet.readString(ProductImportConstants.TYPE_COL);
            //If the line identifier is PRICE, then that means the Account type price is starting so create a new instance
            //Read in values and assign to appropriate fields.
            //Map Price field in the same fileItem object
            if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRICE.toString())) {
                processNextItem = true;
                attributesRead = new ArrayList<>();
                productRow = wrappedObjectLine.getLineNr() + "," + wrappedObjectLine.getLine();
                logger.debug("Bulk Upload - Account Type: STARTING NEW ACCOUNT TYPE PRICE from file - {}", fileName);
                logger.debug("Bulk Upload - Account Type: Product Basic Details - {}", productRow);

                incrementTotalCount();
                errorLogged = false;

                fileItem = new ProductFileItem();
                wrappedObjectLinews = new WrappedObjectLine<>(wrappedObjectLine.getLineNr(), wrappedObjectLine.getLine(), fileItem);

                attributesRead.add(ProductImportConstants.ColumnIdentifier.PRICE);
                currencyId = ProductUploadFileItemProcessor.processAccountTypePrice(fieldSet, fileItem, entityId, userId);
            }
            else if(null != attributesRead && attributesRead.contains(ProductImportConstants.ColumnIdentifier.PRICE)) {

                boolean newPricingModelInitialized = false;

                if(BulkLoaderUtility.PriceModelInitializationRequired(priceModel, lineId) ) {
                    priceModel = new PriceModelWS();
                    newPricingModelInitialized = true;
                }

                if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_FLAT.toString())) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_FLAT);
                    ProductUploadFileItemProcessor.processPriceModelFlat(fieldSet, priceModel);

                    priceModel.setCurrencyId(currencyId);
                }
                else if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_TIERED.toString())) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_TIERED);
                    ProductUploadFileItemProcessor.processPriceModelTiered(fieldSet, priceModel);
                    priceModel.setCurrencyId(currencyId);
                }

                if(newPricingModelInitialized) {
                    BulkLoaderUtility.AddPriceModelToChain(fileItem,priceModel);
                }
            }else{
                writeLineToErrorFile(wrappedObjectLine, "PRICE identifier row not available");
                errorLogged = false;
            }
        } catch (ValidationException validationException) {
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

    /**
     * Gets count for total products processed
     */
    private int getTotalCount() {
        return executionContext.getInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0);
    }

    /**
     * Increments the total number of line processed
     */
    private void incrementTotalCount() {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, cnt+1);
    }

    /**
     * increment the error line count
     */
    private void incrementErrorCount() {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt + 1);
    }

    public void setErrorWriter(ResourceAwareItemWriterItemStream errorWriter) {
        this.errorWriter = errorWriter;
    }

    /**
     * Write the original line the WrappedObjectLine and the message to the error file
     *
     * @param item      Object containing the error
     * @param message   Error message to append to line
     * @throws Exception Thrown when an error occurred while trying to write to the file
     */
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

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(errorWriter, "ErrorWriter must be set");
    }


    private WrappedObjectLine<FieldSet> readItem() throws  Exception {
        return (WrappedObjectLine<FieldSet>) this.delegate.read();
    }

    private WrappedObjectLine<FieldSet> peakNextItem() throws  Exception {
        return (WrappedObjectLine<FieldSet>) this.delegate.peek();
    }

}
