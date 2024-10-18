package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
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
import java.util.List;
import java.util.SortedMap;

import static com.sapienter.jbilling.server.integration.Constants.PLAN_DURATION_MF;
import static com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MF;

/**
 * Created by wajeeha on 6/14/18.
 */
public class PlanDownloadProcessor implements ItemProcessor<PlanDTO, String>, StepExecutionListener {

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
    public String process(PlanDTO plan) throws Exception {
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            try {
                return createCsvAccountTypeDetails(plan);
            }  catch (Exception exception) {
                logger.error("Exception: " + exception);
                incrementErrorCount();
                writeLineToErrorFile(exception.toString());
            }
            return null;
        }
    }

    private String createCsvAccountTypeDetails(PlanDTO plan) {

        List<PlanItemDTO> planItems = plan.getPlanItems();
        StringBuilder builder = new StringBuilder();

        ItemDTOEx item = webServicesSessionBean.getItem(plan.getItemId(), null, null);

        try {

            builder.append("PLAN,");
            builder.append(item.getNumber());
            builder.append(",");
            builder.append(item.getDescription());
            builder.append(",");
            builder.append(plan.getPeriod().getDescription(1));
            builder.append(",");
            CurrencyDAS currencyDAS = new CurrencyDAS();
            builder.append(currencyDAS.findCurrencyCodeById(item.getCurrencyId()));
            builder.append(",");
            builder.append(item.getPrice());
            builder.append(",");

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(dateFormatString);
            Date activeSince = item.getActiveSince();
            builder.append(activeSince != null ? dateFormat.format(
                    DateConvertUtils.asLocalDateTime(activeSince)) : "");
            builder.append(",");

            Date activeUntil = item.getActiveUntil();
            builder.append(activeUntil != null ? dateFormat.format(
                    DateConvertUtils.asLocalDateTime(activeUntil)) : "");
            builder.append(",");
            builder.append("\"");

            for (Integer category : item.getTypes()) {
                builder.append(webServicesSessionBean.getItemCategoryById(category) != null?
                        webServicesSessionBean.getItemCategoryById(category).getDescription() : "");
            }

            //builder.replace(builder.lastIndexOf(","), builder.lastIndexOf(","), "\"");
            builder.append("\",");
            String paymentOptionValue = null;
            String duration = null;

            for (MetaFieldValue metaFieldValue : plan.getMetaFields()) {
                if (metaFieldValue.getField().getName().equals(PLAN_PAYMENT_OPTION_MF)) {
                    paymentOptionValue = metaFieldValue.getValue() != null? metaFieldValue.getValue().toString() : "";
                } else if (metaFieldValue.getField().getName().equals(PLAN_DURATION_MF)) {
                    duration = metaFieldValue.getValue() != null? metaFieldValue.getValue().toString() : "";
                }
            }

            builder.append(paymentOptionValue);
            builder.append(",");
            builder.append(duration);
            builder.append("\n");

            if (null != plan.getUsagePools()) {
                for (UsagePoolDTO pool : plan.getUsagePools()) {
                    builder.append("FUP,");
                    UsagePoolWS usagePoolWS = webServicesSessionBean.getUsagePoolWS(pool.getId());
                    builder.append(usagePoolWS.getNames().get(0).getContent());
                    builder.append("\n");
                }
            }

            incrementTotalCount();

            for (PlanItemDTO planItem : planItems) {
                SortedMap<Date, PriceModelDTO> priceModels = planItem.getModels();

                builder.append("ITEM,");

                if (null != planItem.getBundle()) {
                    builder.append(planItem.getBundle().getQuantity());
                    builder.append(",");
                    builder.append(planItem.getBundle().getPeriod().getDescription());
                    builder.append(",");
                } else {
                    builder.append(",,");
                }

                ItemDTO itemDTO = planItem.getItem();

                for(Integer categoryId: itemDTO.getTypes()){
                    String itemCategory =  webServicesSessionBean.getItemCategoryById(categoryId).getDescription();
                    int startIndex = builder.indexOf(itemCategory);

                    if(startIndex > -1) {
                        builder.replace(startIndex, (startIndex + itemCategory.length()), "");
                    }
                }

                builder.append(itemDTO.getNumber());
                builder.append("\n");

                for (Date date : priceModels.keySet()) {
                    PriceModelDTO priceModel = priceModels.get(date);

                    if (priceModel.getType().equals(PriceModelStrategy.FLAT.name())) {
                        builder.append("FLAT," + priceModel.getRate() + "\n");
                    } else if (priceModel.getType().equals(PriceModelStrategy.TIERED.name())) {

                        for (String key : priceModel.getAttributes().keySet()) {
                            builder.append("TIER," + key + "," + priceModel.getAttributes().get(key) + "\n");
                        }
                    }
                }
            }

            if (builder.toString().isEmpty())
                return null;

            return builder.toString();
        }catch (Exception exception) {
            logger.error("Exception: " + exception);
            incrementErrorCount();
            writeLineToErrorFile(item.getNumber() + "," + exception.toString());
            return null;
        }
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
