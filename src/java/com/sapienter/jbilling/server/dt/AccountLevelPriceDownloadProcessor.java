package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.db.AccountTypePriceBL;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.SortedMap;

/**
 * Created by wajeeha on 6/12/18.
 */
public class AccountLevelPriceDownloadProcessor implements ItemProcessor<AccountTypeWS, String>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String dateFormatString = "MM/dd/yyyy";
    private int entityId;

    /**JobExecution's Execution Context */
    private ExecutionContext executionContext;

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;

    private ResourceAwareItemWriterItemStream errorWriter;

    public void setErrorWriter(ResourceAwareItemWriterItemStream errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        entityId = jobParameters.getLong(ProductImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        executionContext = stepExecution.getJobExecution().getExecutionContext();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public String process(AccountTypeWS accountTypeWS) throws Exception {
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            try {
                return createCsvAccountTypeDetails(accountTypeWS);
            }  catch (Exception exception) {
                logger.error("Exception: " + exception);
                incrementErrorCount();
                writeLineToErrorFile(accountTypeWS.getId() + "," + exception.toString());
            }
            return null;
        }
    }

    private String createCsvAccountTypeDetails(AccountTypeWS accountTypeWS) {
        PlanItemWS[] accountTypePrices = webServicesSessionBean.getAccountTypePrices(accountTypeWS.getId());
        StringBuilder builder = new StringBuilder();
        boolean addPriceTag = false;

        for (PlanItemWS planItem : accountTypePrices) {
            SortedMap<Date, PriceModelWS> priceModels = planItem.getModels();
            addPriceTag = true;

            for (Date date : priceModels.keySet()) {
                PriceModelWS priceModelWS = priceModels.get(date);

                if(priceModelWS.getType().equals(PriceModelStrategy.FLAT.name()) ||
                        priceModelWS.getType().equals(PriceModelStrategy.TIERED.name())) {
                    if (addPriceTag) {
                        ItemDTOEx item = webServicesSessionBean.getItem(planItem.getItemId(), null, null);

                        if(item.getIsPlan())
                            continue;

                        builder.append("PRICE,");
                        builder.append(item.getNumber());
                        builder.append(",");
                        builder.append(accountTypeWS.getId());
                        builder.append(",");

                        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(dateFormatString);
                        builder.append(date != null ? dateFormat.format(DateConvertUtils.asLocalDateTime(date)) : "");
                        builder.append(",");

                        AccountTypePriceBL accountTypePriceBl = new AccountTypePriceBL(
                                accountTypeWS.getId(), planItem.getId());
                        Date expiryDate = accountTypePriceBl.getPrice().getPriceExpiryDate();

                        builder.append(expiryDate != null ?
                                dateFormat.format(DateConvertUtils.asLocalDate(expiryDate)) : "");
                        builder.append(",");
                        CurrencyDAS currencyDAS = new CurrencyDAS();
                        builder.append(currencyDAS.findCurrencyCodeById(priceModelWS.getCurrencyId()));
                        builder.append(",false\n");

                        addPriceTag = false;

                        incrementTotalCount();
                    }

                    if (priceModelWS.getType().equals(PriceModelStrategy.FLAT.name())) {
                        builder.append("FLAT," + priceModelWS.getRate() + "\n");
                    } else if (priceModelWS.getType().equals(PriceModelStrategy.TIERED.name())) {

                        for (String key : priceModelWS.getAttributes().keySet()) {
                            builder.append("TIER," + key + "," + priceModelWS.getAttributes().get(key) + "\n");
                        }
                    }
                }
            }
        }

        if(builder.toString().isEmpty())
            return null;

        return builder.toString();
    }

    /**
     * Increments the total number of line processed
     */
    private void incrementTotalCount() {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, cnt+1);
    }

    private void writeLineToErrorFile(String message) {
        try {
            errorWriter.write(Arrays.asList(message));
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }

    /**
     * increment the error line count
     */
    private void incrementErrorCount() {
        int cnt = executionContext.getInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt + 1);
    }
}
