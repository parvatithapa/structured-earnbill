package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.FlatPricingStrategy;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
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

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Created by Taimoor Choudhary on 6/11/18.
 */
public class ProductDefaultPriceDownloadProcessor implements ItemProcessor<ItemDTOEx, String>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String dateFormatString = "MM/dd/yyyy";
    private int entityId;

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
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    @Override
    public String process(ItemDTOEx itemDTOEx) throws Exception {
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            try {

                if(!itemDTOEx.getIsPlan() && itemDTOEx.getDeleted() == 0) {

                    return createCsvItemDetails(itemDTOEx);
                }
            } catch (Exception exception) {
                logger.error("Exception: " + exception);
                incrementErrorCount();
                writeLineToErrorFile(itemDTOEx.getNumber() + "," + exception.toString());
            }
            return null;
        }
    }

    private String createCsvItemDetails(ItemDTOEx itemDTOEx){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(getProductDetails(itemDTOEx));
        stringBuilder.append(getRatingConfigurationDetails(itemDTOEx));
        stringBuilder.append(getMetaFieldDetails(itemDTOEx));
        stringBuilder.append(getPriceDetails(itemDTOEx));

        return stringBuilder.toString();
    }

    private String getProductDetails(ItemDTOEx itemDTOEx){

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(dateFormatString);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("PROD,");

        stringBuilder.append(itemDTOEx.getNumber());
        stringBuilder.append(",\"");

        itemDTOEx.getDescriptions().stream().forEach(internationalDescriptionWS -> {
            stringBuilder.append(internationalDescriptionWS.getLanguageId());
            stringBuilder.append(":");
            stringBuilder.append(internationalDescriptionWS.getContent());
            stringBuilder.append(",");
        });
        stringBuilder.replace(stringBuilder.lastIndexOf(","), stringBuilder.lastIndexOf(","), "\"");

        stringBuilder.append(itemDTOEx.getHasDecimals().equals(1)? "true": "false");
        stringBuilder.append(",\"");

        for (int i = 0; i < itemDTOEx.getTypes().length; i++) {

            ItemTypeWS itemCategory = webServicesSessionBean.getItemCategoryById(itemDTOEx.getTypes()[i]);

            stringBuilder.append(itemCategory.getDescription());

            if(i < (itemDTOEx.getTypes().length -1)){
                stringBuilder.append(",");
            }
        }
        stringBuilder.append("\",");

        stringBuilder.append(itemDTOEx.getActiveSince() != null ? dateFormat.format(DateConvertUtils.asLocalDateTime(itemDTOEx.getActiveSince())) : "");
        stringBuilder.append(",");

        stringBuilder.append(itemDTOEx.getActiveUntil() != null ? dateFormat.format(DateConvertUtils.asLocalDateTime(itemDTOEx.getActiveUntil())) : "");
        stringBuilder.append(",");

        if(!itemDTOEx.isGlobal()) {
            if (itemDTOEx.getEntityId() != null) {
                CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(itemDTOEx.getEntityId());
                stringBuilder.append(companyWS.getDescription());
            }
        }
        stringBuilder.append("\n");

        return stringBuilder.toString();
    }

    private String getPriceDetails(ItemDTOEx itemDTOEx){

        CompanyWS companyWS = null;
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(dateFormatString);

        if(itemDTOEx.getPriceModelCompanyId() != null) {
            companyWS = webServicesSessionBean.getCompanyByEntityId(itemDTOEx.getPriceModelCompanyId());
        }

        StringBuilder stringBuilder = new StringBuilder();

        for(Map.Entry<Date, PriceModelWS> entrySet : itemDTOEx.getDefaultPrices().entrySet()) {
            stringBuilder.append("PRICE,");

            stringBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(entrySet.getKey())));
            stringBuilder.append(",");

            String currencyCode = new CurrencyDAS().findCurrencyCodeById(entrySet.getValue().getCurrencyId());

            stringBuilder.append(currencyCode);
            stringBuilder.append(",");

            if(companyWS != null) {
                stringBuilder.append(companyWS.getDescription());
            }
            stringBuilder.append(",false\n");

            if (entrySet.getValue().getType().equals(PriceModelStrategy.FLAT.name())){
                stringBuilder.append("FLAT,");

                stringBuilder.append(entrySet.getValue().getRate());
                stringBuilder.append("\n");

            }else if(entrySet.getValue().getType().equals(PriceModelStrategy.TIERED.name())){

                entrySet.getValue().getAttributes().forEach((quantity, rate) -> {

                    stringBuilder.append("TIER,");
                    stringBuilder.append(quantity);
                    stringBuilder.append(",");
                    stringBuilder.append(rate);
                    stringBuilder.append("\n");
                });

            }
        }

        return stringBuilder.toString();
    }

    private String getMetaFieldDetails(ItemDTOEx itemDTOEx){

        StringBuilder stringBuilder = new StringBuilder();

        itemDTOEx.getMetaFieldsMap().forEach((id, metaFieldValueWSes) -> {
            if(this.entityId == id){
                for(MetaFieldValueWS metaFieldValueWS : metaFieldValueWSes){
                    if(metaFieldValueWS.getFieldName().equals(com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF)){

                        if(StringUtils.isNotBlank(metaFieldValueWS.getStringValue())) {
                            stringBuilder.append("META,\"");
                            stringBuilder.append(metaFieldValueWS.getStringValue());
                            stringBuilder.append("\"\n");
                        }
                        break;
                    }
                }
            }
        });

        return stringBuilder.toString();
    }

    private String getRatingConfigurationDetails(ItemDTOEx itemDTOEx){

        StringBuilder stringBuilder = new StringBuilder();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(dateFormatString);

        itemDTOEx.getRatingConfigurations().forEach((date, ratingConfigurationWS) -> {
            stringBuilder.append("RATING,");

            stringBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(date)));
            stringBuilder.append(",");

            if(ratingConfigurationWS.getRatingUnit() != null) {
                stringBuilder.append(ratingConfigurationWS.getRatingUnit().getName());
            }
            stringBuilder.append(",");

            if(ratingConfigurationWS.getUsageRatingScheme() != null) {
                stringBuilder.append(ratingConfigurationWS.getUsageRatingScheme().getRatingSchemeCode());
            }
            stringBuilder.append(",");

            if(CollectionUtils.isNotEmpty(ratingConfigurationWS.getPricingUnit())) {

                stringBuilder.append("\"");
                ratingConfigurationWS.getPricingUnit().stream().forEach(internationalDescriptionWS -> {

                    stringBuilder.append(internationalDescriptionWS.getLanguageId());
                    stringBuilder.append(":");
                    stringBuilder.append(internationalDescriptionWS.getContent());
                    stringBuilder.append(",");
                });
                stringBuilder.setLength(stringBuilder.length() - 1);
                stringBuilder.append("\"");
            }
            stringBuilder.append("\n");
        });

        return stringBuilder.toString();
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
