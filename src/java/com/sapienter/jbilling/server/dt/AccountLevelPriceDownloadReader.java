package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * Created by wajeeha on 6/12/18.
 */
public class AccountLevelPriceDownloadReader implements ItemReader<AccountTypeWS>, ItemStreamReader<AccountTypeWS>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int entityId;
    private String fileName;
    private String accountId;
    private boolean itemProcessed;
    private static Integer arrayIndex;
    private AccountTypeWS[] allAccountTypesByEntityId;

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;

    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;

    private ResourceAwareItemWriterItemStream errorWriter;

    public void setErrorWriter(ResourceAwareItemWriterItemStream errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        executionContext = stepExecution.getJobExecution().getExecutionContext();
        entityId = jobParameters.getLong(ProductImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        accountId = jobParameters.getString(ProductImportConstants.JOB_PARAM_IDENTIFICATION_CODE);
        fileName = FilenameUtils.getBaseName(jobParameters.getString(ProductImportConstants.JOB_PARAM_OUTPUT_FILE));
        arrayIndex = 0;
        itemProcessed = false;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public AccountTypeWS read() throws Exception{
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {

            try {
                if(StringUtils.isNotBlank(accountId) && !itemProcessed) {

                    AccountTypeWS accountType = webServicesSessionBean.getAccountType(Integer.valueOf(accountId));

                    if(accountType != null){

                        itemProcessed = true;
                        return accountType;
                    }

                }else if(allAccountTypesByEntityId != null && arrayIndex < allAccountTypesByEntityId.length) {

                    AccountTypeWS accountTypeWS = allAccountTypesByEntityId[arrayIndex++];
                    return accountTypeWS;
                }
            }catch (Exception exception){
                logger.error("Exception: " + exception);
                incrementErrorCount();
                writeLineToErrorFile(exception.toString());
            }
            return null;
        }
    }

    private void writeLineToErrorFile(String message) {
        try {
            errorWriter.write(Arrays.asList(message));
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // Read Products for the current entity
        if(StringUtils.isBlank(accountId)) {
            allAccountTypesByEntityId = webServicesSessionBean.getAllAccountTypesByCompanyId(entityId);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, cnt+1);
    }

    @Override
    public void close() throws ItemStreamException {

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
}
