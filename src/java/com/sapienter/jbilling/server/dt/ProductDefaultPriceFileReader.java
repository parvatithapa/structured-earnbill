package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.common.CommonConstants;
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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Wajeeha Ahmed on 12/8/17.
 */
public class ProductDefaultPriceFileReader  implements ItemStreamReader, ItemReader, StepExecutionListener {

    private SingleItemPeekableItemReader<FieldSet> delegate;
    private List<ProductImportConstants.ColumnIdentifier> attributesRead = null;
    private int userId;
    private Integer entityId;
    private boolean recordFinished;
    private boolean processNextItem;
    private boolean errorLogged;
    private String productRow;
    private String currentRow;
    private WrappedObjectLine<ProductFileItem> wrappedObjectLinews = null;
    private ProductFileItem fileItem = null;
    private PriceModelWS priceModelWS = null;
    private PriceModel priceModel = null;
    private boolean priceModelAdded =false;
    private boolean resetPriceModel =false;
    private String fileName;

    private ProductPriceFileValidator validator;

    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ResourceAwareItemWriterItemStream errorWriter;

    public void setValidator(ProductPriceFileValidator validator) {
        this.validator = validator;
    }

    public void setErrorWriter(ResourceAwareItemWriterItemStream errorWriter) {
        this.errorWriter = errorWriter;
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
    public Object read() throws Exception {
        recordFinished = false;

        try {
            while (!recordFinished) {

                if (peakNextItem() == null) {
                    logger.debug("Bulk Upload - Default Price: FILE FINISHED - {}", fileName);
                    recordFinished = true;
                    continue;
                }

                if (!processNextItem) {
                    if (!peakNextItem().getLine().isEmpty() &&
                                    !peakNextItem().getObject().readString(0).equals(ProductImportConstants.ColumnIdentifier.PRODUCT.toString())) {
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
                    logger.error("Bulk Upload - Default Price: PRODUCT FINISHED with ERROR - {}", fileName);
                    processNextItem = false;
                    wrappedObjectLinews = null;
                    throw validationException;
                }
                //If the next line identifier is PROD, then that means the next product will be starting and current product is completed
                if (peakNextItem() == null || (!peakNextItem().getLine().isEmpty() &&
                        peakNextItem().getObject().readString(0).equals(ProductImportConstants.ColumnIdentifier.PRODUCT.toString()))) {
                    logger.debug("Bulk Upload - Default Price: RECORD FINISHED");

                    if (null != wrappedObjectLinews) {
                        processNextItem = false;
                        recordFinished = true;
                    }
                }
            }

            if (null == wrappedObjectLinews) {
                return null;
            }

            try {
                // Validate whether required rows has been read or not
                validator.validateProductRowsForDefaultPrices(attributesRead);
            } catch (ValidationException validationException) {
                logger.error("Bulk Upload - Default Price: PRODUCT FINISHED with ERROR - {}", fileName);

                writeLineToErrorFile(productRow, validationException.getMessage());

                wrappedObjectLinews = null;
                attributesRead.clear();
                attributesRead = null;
                throw validationException;
            }

            WrappedObjectLine<ProductFileItem> result = wrappedObjectLinews;

            attributesRead.clear();
            attributesRead = null;
            wrappedObjectLinews = null;
            fileItem = null;
            return result;
        }catch (Exception exception){

            String exceptionMessage = exception.getMessage();

            logger.error("Bulk Upload - Default Price: PRODUCT FINISHED with ERROR - {}", fileName);

            if(exceptionMessage.contains("Cannot access columns by name without meta data"))
            {
                logger.error(String.format("Bulk Upload - Default Price - Row contains unsupported tags/info: %s", currentRow),
                        exceptionMessage);
                exceptionMessage = "Invalid Row format";
            }
            writeLineToErrorFile(currentRow, exceptionMessage);

            logger.error(String.format("Bulk Upload - Default Price: %s", currentRow), exceptionMessage);

            processNextItem = false;
            wrappedObjectLinews = null;
            attributesRead = null;
            throw new ValidationException(exception.getMessage());
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
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt + 1);
    }

    /**
     * Process individual rows
     * @param wrappedObjectLine
     * @throws Exception
     */
    private void process(WrappedObjectLine<FieldSet> wrappedObjectLine) throws Exception {

        // finish processing if we hit the end of file
        if (wrappedObjectLine == null) {
            logger.debug("Bulk Upload - Default Price: FILE FINISHED - {}", fileName);
            recordFinished = true;
            return;
        }
        try {
            FieldSet fieldSet = wrappedObjectLine.getObject();
            currentRow = wrappedObjectLine.getLineNr() + "," + wrappedObjectLine.getLine();

            ProductUploadFileItemProcessor.Validator = validator;

            String lineId = fieldSet.readString(ProductImportConstants.TYPE_COL);
            //If the line identifier is PROD, then that means the product is starting so create a new instance and read in values and assign to appropriate fields, e.g. product code
            if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRODUCT.toString())) {

                productRow = wrappedObjectLine.getLineNr() + "," + wrappedObjectLine.getLine();
                logger.debug("Bulk Upload - Default Price: STARTING NEW PRODUCT from file - {}", fileName);
                logger.debug("Bulk Upload - Default Price: Product Basic Details - {}", productRow);

                incrementTotalCount();
                errorLogged = false;

                processNextItem = true;
                attributesRead = new ArrayList<>();
                fileItem = new ProductFileItem();
                wrappedObjectLinews = new WrappedObjectLine<ProductFileItem>(wrappedObjectLine.getLineNr(), wrappedObjectLine.getLine(), fileItem);

                attributesRead.add(ProductImportConstants.ColumnIdentifier.PRODUCT);
                ProductUploadFileItemProcessor.processProductFields(fieldSet, fileItem, entityId);

            }else if(lineId.equals(ProductImportConstants.ColumnIdentifier.RATING_UNIT.toString())){

                //Add Rating Unit in the same fileItem object
                ProductUploadFileItemProcessor.processRatingUnit(fieldSet, fileItem, this.entityId);

                // Indicate that the Rating Unit has been read as incoming parameter
                if(!attributesRead.contains(ProductImportConstants.ColumnIdentifier.RATING_UNIT)) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.RATING_UNIT);
                }

            }else if(lineId.equals(ProductImportConstants.ColumnIdentifier.META_FIELDS.toString())){

                //Add MetaFields in the same fileItem object
                ProductUploadFileItemProcessor.processMetaFields(fieldSet, fileItem, this.entityId);

                // Indicate that the MetaFields has been read as incoming parameters
                if(!attributesRead.contains(ProductImportConstants.ColumnIdentifier.META_FIELDS)) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.META_FIELDS);
                }
            }else if(null != attributesRead && attributesRead.contains(ProductImportConstants.ColumnIdentifier.PRODUCT)) {

                if((!lineId.equals("PRICE") && BulkLoaderUtility.PriceModelInitializationRequired(priceModelWS, lineId)) || resetPriceModel ) {
                    priceModelWS = new PriceModelWS();
                    resetPriceModel = false;
                }

                if(priceModelAdded == false && !lineId.equals("PRICE")){
                    Price price = fileItem.getPrices().get(fileItem.getPrices().size() - 1);
                    fileItem.getPriceModelTimeLine().put(price.getDate(), priceModel);
                    priceModelAdded = true;

                }

                //map Price field in the same fileItem object
                if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRICE.toString())) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.PRICE);
                    ProductUploadFileItemProcessor.processPrice(fieldSet, fileItem, entityId, userId);
                    priceModel = new PriceModel();
                    ProductUploadFileItemProcessor.setChainedValueInPriceModel(priceModel, fieldSet);
                    priceModelAdded = false;
                    resetPriceModel = true;
                }

                //map FLAT price model field in the same fileItem object
                else if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_FLAT.toString())) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_FLAT);

                    if (priceModelWS.getType() == null) {
                        Price price = fileItem.getPrices().get(fileItem.getPrices().size() - 1);
                        priceModelWS.setCurrencyId(price.getCurrencyId());
                        fileItem.getPriceModelTimeLine().put(price.getDate(), priceModel);

                        if(fileItem.IsPriceModelForEpochDateEnable()){
                            fileItem.getPriceModelTimeLine().put(CommonConstants.EPOCH_DATE, priceModel);
                            fileItem.EnablePriceModelForEpochDate(false);
                        }

                        ProductUploadFileItemProcessor.createUpdatePriceTimeLine(fileItem, price.getDate(), priceModelWS);
                    }

                    ProductUploadFileItemProcessor.processPriceModelFlat(fieldSet, priceModelWS);

                }

                //map TIERED price model field in the same fileItem object
                else if (lineId.equals(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_TIERED.toString())) {
                    attributesRead.add(ProductImportConstants.ColumnIdentifier.PRICE_MODEL_TIERED);

                     if(priceModelWS.getType() == null) {

                         Price price = fileItem.getPrices().get(fileItem.getPrices().size() - 1);
                         priceModelWS.setCurrencyId(price.getCurrencyId());

                         if(fileItem.IsPriceModelForEpochDateEnable()){
                             fileItem.getPriceModelTimeLine().put(CommonConstants.EPOCH_DATE, priceModel);
                             fileItem.EnablePriceModelForEpochDate(false);
                         }

                         ProductUploadFileItemProcessor.createUpdatePriceTimeLine(fileItem, price.getDate(), priceModelWS);
                     }

                    ProductUploadFileItemProcessor.processPriceModelTiered(fieldSet, priceModelWS);

                }

            }else{
                writeLineToErrorFile(wrappedObjectLine, "PROD identifier row not available");
                errorLogged = false;
            }
        } catch (ValidationException validationException) {
            writeLineToErrorFile(wrappedObjectLine, validationException.getMessage());
            throw validationException;
        }
    }

    private void incrementTotalCount() {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, cnt+1);
    }
}
