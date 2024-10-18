package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.validator.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Taimoor Choudhary on 3/29/18.
 */
public class PlanUploadProcessor {

    public static void processPlanFields(FieldSet fieldSet, PlanFileItem planFileItem, int entityId) throws ValidationException{

        // Validate fields for correct format
        Integer planId = PlanPriceFileValidator.validatePlanDetails(fieldSet, entityId);

        if(planId != null) {
            planFileItem.setPlanId(planId);
        }

        // Unique Number for Identify Plan
        planFileItem.setPlanNumber(fieldSet.readString(PlanImportConstants.PLAN_NUMBER_COL));

        setMultiLanguageDescription(fieldSet, planFileItem);

        // Set Plan Period ID
        planFileItem.setPlanPeriodId(getPeriodId(fieldSet.readString(PlanImportConstants.PLAN_PERIOD_COL), entityId, false));

        // Set Currency ID
        int currencyId = getCurrencyId(fieldSet.readString(PlanImportConstants.CURRENCY_CODE_COL), entityId);
        planFileItem.setCurrencyId(currencyId);

        // Set Availability Start Date
        if (StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.AVAILABILITY_START_COL))) {
            planFileItem.setAvailabilityStartDate(fieldSet.readDate(PlanImportConstants.AVAILABILITY_START_COL, "MM/dd/yyyy"));
        }

        // Set Availability End Date
        if (StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.AVAILABILITY_END_COL))) {
            planFileItem.setAvailabilityEndDate(fieldSet.readDate(PlanImportConstants.AVAILABILITY_END_COL, "MM/dd/yyyy"));
        }

        // Set Plan Rate
        if (StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.PLAN_RATE_COL))) {
            planFileItem.setRate(new BigDecimal(fieldSet.readString(PlanImportConstants.PLAN_RATE_COL)));
        }

        // Set Plan Category
        planFileItem.setPlanCategory(fieldSet.readString(PlanImportConstants.PLAN_CATEGORY_COL));

        // Set Payment Option Meta-Field
        planFileItem.setPaymentOption(fieldSet.readString(PlanImportConstants.PLAN_MF_PAYMENT_OPTION_COL));

        // Set Duration Meta-Field
        planFileItem.setDuration(fieldSet.readString(PlanImportConstants.PLAN_MF_DURATION_COL));
    }

    public static void processPlanItemFields(FieldSet fieldSet, PlanFileItem planFileItem, int entityId) throws ValidationException{

        // Validate fields for correct format
        int itemId = PlanPriceFileValidator.validatePlanItemDetails(fieldSet, entityId);

        if(CollectionUtils.isNotEmpty(planFileItem.getPlanProducts())){

            PlanProduct existingPlanProduct = planFileItem.getPlanProducts().stream().filter(planProduct -> planProduct.getItemId().equals(itemId))
                    .findFirst().orElse(null);

            if(existingPlanProduct != null){
                throw new ValidationException("ItemID already added in the current Plan: " + itemId);
            }
        }

        PlanProduct planProduct = new PlanProduct();

        planProduct.setItemId(itemId);

        if(StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.BUNDLE_QTY_COL))) {
            planProduct.setItemBundledQuantity(fieldSet.readInt(PlanImportConstants.BUNDLE_QTY_COL));
        }else {
            planProduct.setItemBundledQuantity(0);
        }

        int itemPeriodId = getPeriodId(fieldSet.readString(PlanImportConstants.BUNDLE_PERIOD_COL), entityId ,true);

        planProduct.setItemBundlePeriod(itemPeriodId);

        // Add Bundled Product to exiting list
        planFileItem.getPlanProducts().add(planProduct);
    }

    public static void processPlanUsagePoolFields(FieldSet fieldSet, PlanFileItem planFileItem, int entityId) throws ValidationException{

        // Validate fields for correct format
        PlanPriceFileValidator.validatePlanFreeUsagePoolDetails(fieldSet);

        // Set Free Usage pool ID
        if (StringUtils.isNotBlank(fieldSet.readString(PlanImportConstants.FREE_USAGE_POOL_COL))) {

            String usagePoolName = fieldSet.readString(PlanImportConstants.FREE_USAGE_POOL_COL);

            Integer usagePoolId = getFreeUsagePoolId(fieldSet.readString(PlanImportConstants.FREE_USAGE_POOL_COL), entityId);

            if(CollectionUtils.isNotEmpty(planFileItem.getFreeUsagePools())){

                if( null != planFileItem.getFreeUsagePools().stream()
                        .filter(planUsagePool -> planUsagePool.getUsagePoolName().equals(usagePoolName)).findFirst().orElse(null)){
                    throw new ValidationException("Row has duplicate Usage Pools: " + fieldSet.readString(PlanImportConstants.FREE_USAGE_POOL_COL));
                }
            }

            PlanUsagePool planUsagePool = new PlanUsagePool();

            planUsagePool.setUsagePoolName(usagePoolName);
            planUsagePool.setUsagePoolQuantity(0);

            if(usagePoolId != null) {
                planUsagePool.setUsagePoolId(usagePoolId);
            }

            planFileItem.getFreeUsagePools().add(planUsagePool);
        }
    }

    private static void setMultiLanguageDescription(FieldSet fieldSet, PlanFileItem fileItem){
        try{

            String descriptions[] = fieldSet.readString(PlanImportConstants.PLAN_DESC_COL).split(",");

            // NOTE: Temporary code keep the current format in working state
            if(descriptions.length == 1){

                String descriptionDetails[] = descriptions[0].split(":");

                try {

                    InternationalDescriptionWS internationalDescriptionWS = new InternationalDescriptionWS();

                    if(descriptionDetails.length == 2) {
                        int languageId = Integer.parseInt(descriptionDetails[0].trim());

                        internationalDescriptionWS.setLanguageId(languageId);
                        internationalDescriptionWS.setContent(descriptionDetails[1].trim());
                    }else {
                        // Set default language English
                        internationalDescriptionWS.setLanguageId(1);
                        internationalDescriptionWS.setContent(descriptionDetails[0].trim());
                    }

                    fileItem.getDescriptions().add(internationalDescriptionWS);
                } catch (NumberFormatException e) {
                    throw new ValidationException("Language ID should be a valid INTEGER - " + descriptionDetails);
                }

                return;
            }

            for(String description: descriptions){
                String descriptionDetails[] = description.split(":");

                if(descriptionDetails.length < 2){
                    throw new ValidationException("Language ID or Plan Description missing - " + description);
                }

                try {

                    InternationalDescriptionWS internationalDescriptionWS = new InternationalDescriptionWS();

                    int languageId = Integer.parseInt(descriptionDetails[0].trim());

                    internationalDescriptionWS.setLanguageId(languageId);
                    internationalDescriptionWS.setContent(descriptionDetails[1].trim());

                    fileItem.getDescriptions().add(internationalDescriptionWS);
                } catch (NumberFormatException e) {
                    throw new ValidationException("Language ID should be a valid INTEGER - " + description);
                }
            }

        }catch (Exception exception){
            throw new ValidationException("Invalid detail in plan description");
        }
    }

    /**
     * Returns Currency ID for the given currency code, if it doesn't exists returns currency id for the Entity
     * @param currencyCode
     * @param entityId
     * @return
     */
    private static Integer getCurrencyId(String currencyCode, int entityId) throws ValidationException{
        Integer currencyId = null;

        if(!StringUtils.isEmpty(currencyCode)) {

            CurrencyDTO currencyDTO = new CurrencyDAS().findCurrencyByCode(currencyCode);
            try {
                if(CurrencyBL.entityHasCurrency(entityId, currencyDTO.getId()))
                {
                    currencyId = currencyDTO.getId();
                }
            } catch (Exception exception) {
                throw new ValidationException("Invalid Currency Code." + exception);
            }
        }

        if (null == currencyId) {
            // If the currency is not specified use the default company currency
            throw new ValidationException("Currency Code does not exist for the provided Company: " + currencyCode);
        }

        return currencyId;
    }

    private static Integer getPeriodId(String planPeriod, int entityId, boolean isItemPeriod) throws ValidationException{

        if(isItemPeriod){

            if(planPeriod.equalsIgnoreCase("one time")){
                return Constants.ORDER_PERIOD_ONCE;
            }
        }

        List<OrderPeriodDTO> orderPeriods = new OrderPeriodDAS().getOrderPeriods(entityId);

        OrderPeriodDTO orderPeriod = orderPeriods.stream()
                .filter(orderPeriodDTO -> orderPeriodDTO.getDescription(Constants.LANGUAGE_ENGLISH_ID).contains(planPeriod))
                .findFirst().orElse(null);

        if(orderPeriod != null) {
            return orderPeriod.getId();
        }else {
            throw new ValidationException(String.format("Plan Period %s does not exist for the Company", planPeriod));
        }
    }

    private static Integer getFreeUsagePoolId(String freeUsagePool, int entityId) throws ValidationException{

        List<UsagePoolDTO> usagePools = new UsagePoolDAS().findByEntityId(entityId);

        if(CollectionUtils.isNotEmpty(usagePools)) {

            UsagePoolDTO usagePoolDTO = usagePools.stream().filter(usagePool ->
                    getAllUsagePoolNames(usagePool.getId()).stream().anyMatch(internationalDescriptionWS -> internationalDescriptionWS.getContent().equalsIgnoreCase(freeUsagePool))
            ).findFirst().orElse(null);

            if(usagePoolDTO != null){
                return usagePoolDTO.getId();
            }
        }

        return null;
    }

    private static List<InternationalDescriptionWS> getAllUsagePoolNames(int usagePoolId) {

        JbillingTableDAS tableDas = Context
                .getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_USAGE_POOL);

        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas
                .findAll(table.getId(), usagePoolId, "name");

        List<InternationalDescriptionWS> names = new ArrayList<>();
        for (InternationalDescriptionDTO descriptionDTO : descriptionsDTO) {
            names.add(DescriptionBL.getInternationalDescriptionWS(descriptionDTO));
        }

        return names;
    }

    public static void processFlatPriceModel(FieldSet fieldSet, PlanFileItem planFileItem) {
        PlanProduct planProduct = planFileItem.getPlanProducts().get(planFileItem.getPlanProducts().size() - 1);

        PriceModelWS priceModel = new PriceModelWS();
        priceModel.setType(PriceModelStrategy.FLAT.name());
        priceModel.setRate(new BigDecimal(fieldSet.readString(ProductImportConstants.FLAT_RATE_COL)));
        priceModel.setCurrencyId(planFileItem.getCurrencyId());
        planProduct.setPriceModelWS(priceModel);

    }

    public static void processTierPriceModel(FieldSet fieldSet, PlanFileItem planFileItem) {

        PlanProduct planProduct = planFileItem.getPlanProducts().get(planFileItem.getPlanProducts().size() - 1);
        PriceModelWS priceModel = planProduct.getPriceModelWS();

        if(null == priceModel) {
            priceModel = new PriceModelWS();
            priceModel.setCurrencyId(planFileItem.getCurrencyId());
            priceModel.setType(BulkLoaderUtility.checkPriceStrategy(PriceModelStrategy.TIERED.name()));
        }

        priceModel.addAttribute(fieldSet.readString(ProductImportConstants.QTY_FROM_COL), fieldSet.readString(ProductImportConstants.TIER_RATE_COL));
        planProduct.setPriceModelWS(priceModel);
    }
}
