package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.batch.WrappedObjectLine;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Wajeeha Ahmed on 12/11/17.
 */
public class ProductProcessor implements ItemProcessor<WrappedObjectLine<ProductFileItem>,WrappedObjectLine<ProductFileItem>>, InitializingBean, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;

    /** error file writer */
    private ResourceAwareItemWriterItemStream errorWriter;
    private Integer entityId;
    private Date latestValidEffectiveDate;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        executionContext    = stepExecution.getJobExecution().getExecutionContext();
        entityId = jobParameters.getLong(ProductImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        latestValidEffectiveDate = null;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public WrappedObjectLine<ProductFileItem> process(WrappedObjectLine<ProductFileItem> wrappedObject) {

        ProductFileItem product = wrappedObject.getObject();
        StringBuilder sb = new StringBuilder();
        ItemDTO productPresent = null;

        if (null == product.getProductCode() || product.getProductCode().equals("")) {
            sb.append("Product Code is required; ");
        }else {
            productPresent = new ItemDAS().findItemByInternalNumber(product.getProductCode(), entityId);
        }
        String[] categories = product.getCategories();
        ArrayList<String> categoryList = new ArrayList<>();

        for(String category : categories){
            if(StringUtils.isNotBlank(category)){
                categoryList.add(category);
            }
        }

        product.setCategories(categoryList.toArray(new String[categoryList.size()]));

        if (0==categoryList.size() && productPresent == null ) {
            sb.append("Product Category/ies are required; ");
        }

        if(!isEffectiveDateValid(product, latestValidEffectiveDate)){
            sb.append("Unable to set price prior to last billing run date of: " +
                    DateConvertUtils.asLocalDateTime(latestValidEffectiveDate).format(DATE_FORMATTER) + "; ");
        }

        if (null != product.getActiveUntil() && null != product.getActiveSince()) {
            // Available END Date should not be lower than the Available START DATE
            if (product.getActiveUntil().compareTo(product.getActiveSince()) < 0) {
                sb.append("Available End Date is less than Available Start Date; ");
            }
        }

        if (0 < sb.length()) {
            String msg = sb.toString();
            try {
                logger.debug("[{}] {}", wrappedObject.getLineNr(), msg);
                writeLineToErrorFile(wrappedObject, msg);
            } catch (Exception e) {
            }
            return null;
        } else {
            return wrappedObject;
        }
    }

    private boolean isEffectiveDateValid(ProductFileItem product, Date billingDate) {
        if (null == billingDate) return true;
        if (product.getPriceEffectiveDate().compareTo(billingDate) >= 0) return true;

        return false;
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
        executionContext.putInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt+1);
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
    private void writeLineToErrorFile(WrappedObjectLine<ProductFileItem> item, String message) throws Exception {
        incrementErrorCount();
        errorWriter.write(Arrays.asList(item.getLine() + ',' + message));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(errorWriter, "ErrorWriter must be set");
    }

}
