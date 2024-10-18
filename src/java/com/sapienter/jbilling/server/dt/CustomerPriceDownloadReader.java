package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
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
import java.util.List;

/**
 * Created by wajeeha on 6/12/18.
 */
public class CustomerPriceDownloadReader implements ItemReader<UserDTO>, ItemStreamReader<UserDTO>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int entityId;
    private String fileName;
    private String customerIdentificationCode;
    private boolean itemProcessed;
    private static Integer arrayIndex;
    private List<UserDTO> allUsersByEntityId;

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
        customerIdentificationCode = jobParameters.getString(ProductImportConstants.JOB_PARAM_IDENTIFICATION_CODE);
        fileName = FilenameUtils.getBaseName(jobParameters.getString(ProductImportConstants.JOB_PARAM_OUTPUT_FILE));
        arrayIndex = 0;
        itemProcessed = false;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public UserDTO read() throws Exception{
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {

            try {
                if(StringUtils.isNotBlank(customerIdentificationCode) && !itemProcessed) {

                    Integer customerBaseUserId = new UserDAS().findUserByMetaFieldNameAndValue(Constants.DeutscheTelekom.EXTERNAL_ACCOUNT_IDENTIFIER,
                            customerIdentificationCode, this.entityId);

                    if (customerBaseUserId != null) {

                        UserDTO userDTO = new UserBL().getUserEntity(customerBaseUserId);
                        itemProcessed = true;

                        return userDTO;
                    }

                }else if(allUsersByEntityId != null && arrayIndex < allUsersByEntityId.size()) {

                    UserDTO user = allUsersByEntityId.get(arrayIndex++);
                    return user;
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

        // Read Users for the current entity
        if(StringUtils.isBlank(customerIdentificationCode)) {
            allUsersByEntityId = new UserDAS().findAllCustomers(entityId);
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
     * increment the error line count
     */
    private void incrementErrorCount() {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt + 1);
    }
}

