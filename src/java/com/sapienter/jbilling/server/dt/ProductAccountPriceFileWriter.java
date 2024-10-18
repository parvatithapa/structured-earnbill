package com.sapienter.jbilling.server.dt;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.batch.WrappedObjectLine;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypePriceBL;

import javax.annotation.Resource;

/**
 * Created by Taimoor Choudhary on 12/11/17.
 */
public class ProductAccountPriceFileWriter implements ItemWriter<WrappedObjectLine<ProductFileItem>> {


    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** JobExecution's Execution Context */
    private ExecutionContext executionContext;

    /** error file writer */
    private ResourceAwareItemWriterItemStream errorWriter;
    private int entityId;
    private BulkLoaderProductFactory productFactory;
    private String previousLine;

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;

    @BeforeStep
    void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        executionContext = stepExecution.getJobExecution().getExecutionContext();
        entityId = jobParameters.getLong(ProductImportConstants.JOB_PARAM_ENTITY_ID).intValue();
        productFactory = new BulkLoaderProductFactory(entityId, webServicesSessionBean);
        previousLine = StringUtils.EMPTY;
    }

    @Override
    public void write(List<? extends WrappedObjectLine<ProductFileItem>> wrappedObjectLines) throws ValidationException {

        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            for (WrappedObjectLine<ProductFileItem> wrappedObjectLine : wrappedObjectLines) {
                ProductFileItem productAccountTypePriceObject = wrappedObjectLine.getObject();

                if(StringUtils.isNotBlank(previousLine) && previousLine.equals(productAccountTypePriceObject.toString())){
                    return;
                }

                previousLine = productAccountTypePriceObject.toString();

                logger.debug("Bulk Upload - Account Type: Processing Account Price where File Item: " + productAccountTypePriceObject.toString());

                Integer accountTypeId = productAccountTypePriceObject.getAccountTypeId();
                String productCode = productAccountTypePriceObject.getProductCode();
                PriceModelWS priceModelWS = productAccountTypePriceObject.getPriceModelWS();
                Date priceEffectiveDate = productAccountTypePriceObject.getPriceEffectiveDate();
                Date priceExpiryDate = productAccountTypePriceObject.getPriceExpiryDate();

                AccountTypeDTO accountTypeDTO = new AccountTypeDAS().find(accountTypeId);

                try {

                    if (accountTypeDTO.getCompany() == null) {
                        writeLineToErrorFile(wrappedObjectLine.getLine(), "Account Type doesn't exists with the given ID");
                        logger.error("Bulk Upload - Account Type: Account Type doesn't exists with the given ID " + wrappedObjectLine.getLine());
                        return;
                    }
                } catch (ObjectNotFoundException exception) {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), "Account Type doesn't exists with the given ID");
                    logger.error("Bulk Upload - Account Type: Account Type doesn't exists with the given ID " + wrappedObjectLine.getLine());
                    return;
                }

                Integer itemId = productFactory.getApi().getItemID(productCode);

                if (null != itemId) {
                    AccountTypePriceBL accountTypePriceBL = new AccountTypePriceBL(accountTypeId);
                    List<PlanItemDTO> planItemDTOList = accountTypePriceBL.getAccountTypePrices(itemId);

                    if (!CollectionUtils.isEmpty(planItemDTOList)) {
                        try {

                            PlanItemWS planItemWS = productFactory.getApi().getAccountTypePrice(accountTypeId, itemId);

                            if (null == planItemWS) {
                                planItemWS = new PlanItemWS();
                                planItemWS.setPrecedence(-1);
                                planItemWS.setItemId(itemId);
                                planItemWS.addModel(priceEffectiveDate, priceModelWS);
                            } else {
                                PriceModelWS priceModel = planItemWS.getModels().get(priceEffectiveDate);

                                if (productAccountTypePriceObject.isChained() && null != priceModel) {
                                    while (true) {

                                        BulkLoaderUtility.validatePricingModel(priceModel, priceModelWS);

                                        if (null == priceModel.getNext()) {
                                            priceModel.setNext(priceModelWS);
                                            break;
                                        } else {
                                            priceModel = priceModel.getNext();
                                        }
                                    }
                                } else {
                                    SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
                                    models.put(priceEffectiveDate, priceModelWS);
                                    planItemWS.setModels(models);
                                }
                            }

                            logger.debug("Bulk Upload - Account Type: Updating account type price where Plan Item: " + planItemWS.toString());

                            // Update Account Level Price
                            productFactory.getApi().updateAccountTypePrice(accountTypeId, planItemWS, priceExpiryDate);

                        } catch (Exception exception) {
                            writeLineToErrorFile(wrappedObjectLine.getLine(), exception.getMessage());
                            throw new ValidationException(exception.getMessage());
                        }
                    } else {

                        try {

                            PlanItemWS planItemWS = new PlanItemWS();
                            planItemWS.setPrecedence(-1);
                            planItemWS.setItemId(itemId);
                            planItemWS.addModel(priceEffectiveDate, priceModelWS);

                            productFactory.getApi().createAccountTypePrice(accountTypeId, planItemWS, priceExpiryDate);

                        } catch (Exception exception) {
                            writeLineToErrorFile(wrappedObjectLine.getLine(), exception.getMessage());
                            throw new ValidationException(exception.getMessage());
                        }
                    }
                } else {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), "Item doesn't exist with given Product Code OR is not available to the calling Company.");
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
     * @param itemLine      Object containing the error
     * @param message   Error message to append to line
     * @throws Exception Thrown when an error occurred while trying to write to the file
     */
    private void writeLineToErrorFile(String itemLine, String message) {
        incrementErrorCount();
        try {
            errorWriter.write(Arrays.asList(itemLine + ',' + message));
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(errorWriter, "ErrorWriter must be set");
    }
}
