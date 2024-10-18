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
import com.sapienter.jbilling.server.user.CustomerPriceBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;

import javax.annotation.Resource;


/**
 * Created by wajeeha on 12/18/17.
 */
public class ProductCustomerPriceFileWriter implements ItemWriter<WrappedObjectLine<ProductFileItem>> {

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
                ProductFileItem productCustomerTypePriceObject = wrappedObjectLine.getObject();

                if (StringUtils.isNotBlank(previousLine) && previousLine.equals(productCustomerTypePriceObject.toString())) {
                    return;
                }

                previousLine = productCustomerTypePriceObject.toString();

                logger.debug("Bulk Upload - Customer Level: Processing Customer Price where File Item: {}",
                        productCustomerTypePriceObject.toString());

                String customerIdentifier = productCustomerTypePriceObject.getCustomerIdentifier();
                String productCode = productCustomerTypePriceObject.getProductCode();
                PriceModelWS priceModelWS = productCustomerTypePriceObject.getPriceModelWS();
                Date priceEffectiveDate = productCustomerTypePriceObject.getPriceEffectiveDate();
                Date priceExpiryDate = productCustomerTypePriceObject.getPriceExpiryDate();

                Integer customerBaseUserId = null;
                try {

                    customerBaseUserId = new UserDAS().findUserByMetaFieldNameAndValue(Constants.DeutscheTelekom.EXTERNAL_ACCOUNT_IDENTIFIER, customerIdentifier, this.entityId);

                    if (customerBaseUserId == null) {
                        writeLineToErrorFile(wrappedObjectLine.getLine(), "Customer doesn't exist with the given Identifier");
                        logger.error("Bulk Upload - Customer Level: Customer doesn't exist with the given Identifier: {}", wrappedObjectLine.getLine());
                        return;
                    }
                } catch (ObjectNotFoundException exception) {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), "Customer doesn't exists with the given Identifier");
                    logger.error("Bulk Upload - Customer Level: Customer doesn't exists with the given Identifier: {}", wrappedObjectLine.getLine());
                    return;
                }

                Integer itemId = productFactory.getApi().getItemID(productCode);

                if (null != itemId) {
                    CustomerPriceBL customerLevelPriceBL = new CustomerPriceBL(customerBaseUserId);
                    List<PlanItemDTO> planItemDTOList = customerLevelPriceBL.getCustomerPrices(itemId);

                    if (!CollectionUtils.isEmpty(planItemDTOList)) {
                        try {

                            PlanItemWS planItemWS = productFactory.getApi().getCustomerPrice(customerBaseUserId, itemId);

                            if (null == planItemWS) {
                                planItemWS = new PlanItemWS();
                                planItemWS.setPrecedence(-1);
                                planItemWS.setItemId(itemId);
                                planItemWS.addModel(priceEffectiveDate, priceModelWS);
                            } else {
                                PriceModelWS priceModel = planItemWS.getModels().get(priceEffectiveDate);

                                if (productCustomerTypePriceObject.isChained() && null != priceModel) {
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
                                    SortedMap<Date, PriceModelWS> models = new TreeMap<>();
                                    models.put(priceEffectiveDate, priceModelWS);
                                    planItemWS.setModels(models);
                                }
                            }

                            logger.debug("Bulk Upload - Customer Level: Updating customer type price where Plan Item: {}", planItemWS.toString());

                            // Update Customer Level Price
                            productFactory.getApi().updateCustomerPrice(customerBaseUserId, planItemWS, priceExpiryDate);

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

                            productFactory.getApi().createCustomerPrice(customerBaseUserId, planItemWS, priceExpiryDate);

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
