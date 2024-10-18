package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.batch.WrappedObjectLine;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.usagePool.UsagePoolResetValueEnum;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.validator.ValidationException;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.sapienter.jbilling.server.integration.Constants.PLAN_DURATION_MF;
import static com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MF;

/**
 * Created by Taimoor Choudhary on 3/28/18.
 */
public class PlanPriceFileWriter implements ItemWriter<WrappedObjectLine<PlanFileItem>> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** JobExecution's Execution Context */
    private ExecutionContext executionContext;

    /** error file writer */
    private ResourceAwareItemWriterItemStream errorWriter;
    private int entityId;
    private BulkLoaderProductFactory productFactory;
    private EnumerationWS paymentOptions;
    private EnumerationWS durations;
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

        populatePaymentOptions();
        populateDurations();
    }

    @Override
    public void write(List<? extends WrappedObjectLine<PlanFileItem>> wrappedObjectLines) throws ValidationException {

        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            for (WrappedObjectLine<PlanFileItem> wrappedObjectLine : wrappedObjectLines) {

                PlanFileItem planFileItem = wrappedObjectLine.getObject();

                if (StringUtils.isNotBlank(previousLine) && previousLine.equals(planFileItem.toString())) {
                    return;
                }

                previousLine = planFileItem.toString();

                logger.debug("Bulk Upload - Plan: Processing Plan where File item: {}", planFileItem.toString());

                if (paymentOptions == null) {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), "No Payment Options enumeration defined in the given company");
                    throw new ValidationException("No Payment Options enumeration defined in the given company");
                } else {
                    EnumerationValueWS paymentOptionEnumeration = paymentOptions.getValues().stream()
                            .filter(enumerationValueWS -> enumerationValueWS.getValue().equals(planFileItem.getPaymentOption()))
                            .findFirst().orElse(null);
                    if (paymentOptionEnumeration == null) {
                        writeLineToErrorFile(wrappedObjectLine.getLine(), "No Payment Options enumeration found for the provided value: " + planFileItem.getPaymentOption());
                        throw new ValidationException("No Payment Options enumeration found for the provided value: " + planFileItem.getPaymentOption());
                    }
                }

                if (durations == null) {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), "No Durations enumeration defined in the given company");
                    throw new ValidationException("No Durations enumeration defined in the given company");
                } else {
                    EnumerationValueWS durationEnumeration = durations.getValues().stream()
                            .filter(enumerationValueWS -> enumerationValueWS.getValue().equals(planFileItem.getDuration()))
                            .findFirst().orElse(null);
                    if (durationEnumeration == null) {
                        writeLineToErrorFile(wrappedObjectLine.getLine(), "No Duration enumeration found for the provided value: " + planFileItem.getDuration());
                        throw new ValidationException("No Duration enumeration found for the provided value: " + planFileItem.getDuration());
                    }
                }

                PlanWS planWS;
                ItemDTOEx itemDTOEx;

                if (planFileItem.getPlanId() != null) {

                    planWS = productFactory.getApi().getPlanWS(planFileItem.getPlanId());
                    itemDTOEx = productFactory.getApi().getItem(planWS.getItemId(), null, null);

                } else {
                    planWS = new PlanWS();
                    itemDTOEx = new ItemDTOEx();

                    planWS.setEditable(1);
                    itemDTOEx.setHasDecimals(0);
                }

                itemDTOEx.setEntityId(this.entityId);

                // Set Plan Description
                if (!CollectionUtils.isEmpty(planFileItem.getDescriptions())) {
                    itemDTOEx.setDescriptions(planFileItem.getDescriptions());
                }

                // Set Plan Availability Start Date
                if (planFileItem.getAvailabilityStartDate() != null) {
                    itemDTOEx.setActiveSince(planFileItem.getAvailabilityStartDate());
                }

                // Set Plan Availability End Date
                if (planFileItem.getAvailabilityEndDate() != null) {
                    itemDTOEx.setActiveUntil(planFileItem.getAvailabilityEndDate());
                }

                // Set Unique Plan Identifier
                if (StringUtils.isNotBlank(planFileItem.getPlanNumber())) {
                    itemDTOEx.setNumber(planFileItem.getPlanNumber());
                }

                // Set Plan Category

                ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
                ItemTypeDTO itemTypeDTO = null;
                Integer categoryId = null;

                itemTypeDTO = itemTypeDAS.findByGlobalDescription(this.entityId, planFileItem.getPlanCategory());

                if (null == itemTypeDTO) {
                    List<ItemTypeDTO> itemTypeDTOList = itemTypeDAS.findByEntityId(this.entityId);

                    for (ItemTypeDTO tempItemTypeDTO : itemTypeDTOList) {
                        if (null != tempItemTypeDTO && tempItemTypeDTO.getDescription().equals(planFileItem.getPlanCategory())) {
                            itemTypeDTO = tempItemTypeDTO;
                            break;
                        }
                    }
                }

                //if category is not Global then findByDescription
                if (null == itemTypeDTO) {
                    ItemTypeWS itemType = new ItemTypeWS();
                    itemType.setDescription(planFileItem.getPlanCategory());
                    itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
                    itemType.setAllowAssetManagement(0);
                    itemType.setGlobal(true);
                    categoryId = productFactory.getApi().createItemCategory(itemType);
                } else {
                    categoryId = itemTypeDTO.getId();
                }

                // Set Plan Rate
                if (planFileItem.getRate() != null) {

                    PriceModelWS priceModel = null;
                    if (planWS.getId() == null) {
                        priceModel = new PriceModelWS();
                        priceModel.setType(PriceModelStrategy.FLAT.name());
                        itemDTOEx.getDefaultPrices().put(CommonConstants.EPOCH_DATE, priceModel);

                    } else {
                        priceModel = itemDTOEx.getDefaultPrices().get(CommonConstants.EPOCH_DATE);
                    }

                    priceModel.setRate(planFileItem.getRate());

                    // Set Plan Currency
                    if (planFileItem.getCurrencyId() != null) {
                        priceModel.setCurrencyId(planFileItem.getCurrencyId());
                    }

                }

                // Set Plan Period
                if (planFileItem.getPlanPeriodId() != null) {
                    planWS.setPeriodId(planFileItem.getPlanPeriodId());
                }

                List<Integer> planBundleItemIds = new ArrayList<>();
                List<Integer> planBundleItemCategoryIds = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(planFileItem.getPlanProducts())) {

                    List<PlanItemWS> planItemWSList = new ArrayList<>();

                    planFileItem.getPlanProducts().forEach(planProduct -> {

                        ItemDTOEx planBundleItemDTOEx = productFactory.getApi().getItem(planProduct.getItemId(), null, null);

                        planBundleItemIds.add(planProduct.getItemId());
                        planBundleItemCategoryIds.addAll(Arrays.stream(planBundleItemDTOEx.getTypes()).collect(Collectors.toList()));

                        PlanItemWS planItemWS = planWS.getPlanItems().stream().filter(planItem -> planItem.getItemId().equals(planProduct.getItemId()))
                                .findFirst().orElse(null);

                        if (planItemWS == null) {
                            planItemWS = new PlanItemWS();
                            PlanItemBundleWS planItemBundleWS = new PlanItemBundleWS();

                            // Add newly created bundle to Plan Item to be used later
                            planItemWS.setBundle(planItemBundleWS);
                        }

                        if(null == planProduct.getPriceModelWS()) {

                            planItemWS.setModels(planBundleItemDTOEx.getDefaultPrices());

                            // Clearing Ids otherwise the Plan update doesn't work as existing PriceModel Ids cause conflicts
                            planItemWS.getModels().values().stream().forEach(priceModelWS -> priceModelWS.setId(null));
                        }else {

                            planItemWS.addModel(CommonConstants.EPOCH_DATE, planProduct.getPriceModelWS());
                        }

                        planItemWSList.add(planItemWS);

                        // Set Plan Item ID and Price Model
                        planItemWS.setItemId(planProduct.getItemId());

                        // Set Plan Bundle Period and Quantity
                        planItemWS.getBundle().setPeriodId(planProduct.getItemBundlePeriod());
                        planItemWS.getBundle().setQuantity(new BigDecimal(planProduct.getItemBundledQuantity()));
                    });

                    // Add newly Plan Items to Plan
                    planWS.setPlanItems(planItemWSList);
                }

                // Get Usage Pool Ids for existing one's or create new Usage Pools
                List<Integer> usagePoolIds = new ArrayList<>();

                planFileItem.getFreeUsagePools().stream().forEach(planUsagePool -> {

                    if (planUsagePool.getUsagePoolId() != null) {

                        usagePoolIds.add(planUsagePool.getUsagePoolId());

                        UsagePoolWS usagePool = productFactory.getApi().getUsagePoolWS(planUsagePool.getUsagePoolId());

                        usagePool.setItemTypes(planBundleItemCategoryIds.stream().toArray(Integer[]::new));
                        usagePool.setItems(planBundleItemIds.stream().toArray(Integer[]::new));

                        productFactory.getApi().updateUsagePool(usagePool);
                    } else {

                        UsagePoolWS usagePool = new UsagePoolWS();

                        usagePool.setEntityId(this.entityId);
                        usagePool.setName(planUsagePool.getUsagePoolName());
                        usagePool.setQuantity(String.valueOf(planUsagePool.getUsagePoolQuantity()));
                        usagePool.setPrecedence(new Integer(1));
                        usagePool.setCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS);
                        usagePool.setCyclePeriodValue(new Integer(1));
                        usagePool.setItemTypes(planBundleItemCategoryIds.stream().toArray(Integer[]::new));
                        usagePool.setItems(planBundleItemIds.stream().toArray(Integer[]::new));
                        usagePool.setUsagePoolResetValue(UsagePoolResetValueEnum.HOURS_PER_CALENDER_MONTH.getResetValue());

                        usagePoolIds.add(productFactory.getApi().createUsagePool(usagePool));
                    }
                });

                // Add Usage Pool IDs
                planWS.setUsagePoolIds(usagePoolIds.stream().toArray(Integer[]::new));

                // Set Plan Meta-Fields
                if (planWS.getId() != null) {
                    for (MetaFieldValueWS metaField : planWS.getMetaFields()) {
                        if (metaField.getFieldName().equals(PLAN_PAYMENT_OPTION_MF)) {

                            metaField.setStringValue(planFileItem.getPaymentOption());
                        } else if (metaField.getFieldName().equals(PLAN_DURATION_MF)) {

                            metaField.setStringValue(planFileItem.getDuration());
                        }
                    }
                }

                // Set/Update Plan Category
                planBundleItemCategoryIds.add(categoryId);
                itemDTOEx.setTypes(planBundleItemCategoryIds.stream().toArray(Integer[]::new));

                try {
                    if (planWS.getId() == null) {

                        itemDTOEx.setIsPlan(true);
                        planWS.setId(productFactory.createPlan(planWS, itemDTOEx));

                        PlanWS createdPlanWS = productFactory.getApi().getPlanWS(planWS.getId());

                        for (MetaFieldValueWS metaField : createdPlanWS.getMetaFields()) {
                            if (metaField.getFieldName().equals(PLAN_PAYMENT_OPTION_MF)) {

                                metaField.setStringValue(planFileItem.getPaymentOption());
                            } else if (metaField.getFieldName().equals(PLAN_DURATION_MF)) {

                                metaField.setStringValue(planFileItem.getDuration());
                            }
                        }

                        productFactory.getApi().updatePlan(createdPlanWS);

                    } else {

                        itemDTOEx.setIsPlan(true);
                        productFactory.updatePlan(planWS, itemDTOEx);
                    }

                } catch (SessionInternalError exception) {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), exception.getMessage());

                    if (planWS.getId() == null) {

                        itemDTOEx.setIsPlan(true);
                        Integer itemId = productFactory.getApi().getItemID(itemDTOEx.getNumber());
                        if (itemId != null) {
                            productFactory.getApi().deleteItem(itemId);
                        }
                    }

                    throw new ValidationException(exception.getMessage());

                } catch (Exception exception) {
                    writeLineToErrorFile(wrappedObjectLine.getLine(), exception.getMessage());
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
        int cnt = executionContext.getInt(PlanImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(PlanImportConstants.JOB_PARAM_ERROR_LINE_COUNT, cnt+1);
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

    private void populatePaymentOptions(){
        try{
            paymentOptions = productFactory.getApi().getEnumerationByNameAndCompanyId(com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MF, this.entityId);
        }catch (Exception exception){
            logger.error("Bulk Upload - Exception occurred while fetching Payment Options Enumeration ", exception);
        }
    }

    private void populateDurations(){
        try{
            durations = productFactory.getApi().getEnumerationByNameAndCompanyId(com.sapienter.jbilling.server.integration.Constants.PLAN_DURATION_MF, this.entityId);
        }catch (Exception exception){
            logger.error("Bulk Upload - Exception occurred while fetching Durations Enumeration ", exception);
        }
    }
}
