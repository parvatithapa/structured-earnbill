package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.batch.WrappedObjectLine;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.validator.ValidationException;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Wajeeha Ahmed on 11/27/17.
 */
public class ProductDefaultPriceFileWriter implements ItemWriter<WrappedObjectLine<ProductFileItem>> {

    /** error file writer */
    private ResourceAwareItemWriterItemStream errorWriter;
    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;
    private BulkLoaderProductFactory bulkLoaderProductFactory;

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;
    private int entityId;
    private int userId;
    private String previousLine;

    @BeforeStep
    void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        userId = jobParameters.getLong(ProductImportConstants.JOB_PARAM_USER_ID).intValue();
        entityId = jobParameters.getLong(ProductImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        bulkLoaderProductFactory = new BulkLoaderProductFactory(entityId, webServicesSessionBean);
        executionContext = stepExecution.getJobExecution().getExecutionContext();
        previousLine = StringUtils.EMPTY;
    }
    @Override
    public void write(List<? extends WrappedObjectLine<ProductFileItem>> wrappedObjectLines) throws ValidationException {

        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            for (WrappedObjectLine<ProductFileItem> wrappedObjectLine : wrappedObjectLines) {

                ProductFileItem fileItem = wrappedObjectLine.getObject();

                if(StringUtils.isNotBlank(previousLine) && previousLine.equals(fileItem.toString())){
                    return;
                }

                try {

                    bulkLoaderProductFactory.createOrUpdateProductPrice(fileItem);
                }catch (Exception exception){
                    previousLine = fileItem.toString();
                    writeLineToErrorFile(wrappedObjectLine, exception.getMessage());
                    throw new ValidationException(exception.getMessage());
                }
            }

            previousLine = StringUtils.EMPTY;
        }
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
    private void writeLineToErrorFile(WrappedObjectLine<ProductFileItem> item, String message) {
        incrementErrorCount();
        try {
            errorWriter.write(Arrays.asList(item.getLine() + ',' + message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
