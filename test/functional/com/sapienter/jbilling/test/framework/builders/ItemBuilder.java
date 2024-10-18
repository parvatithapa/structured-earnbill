package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.item.AssetStatusDTOEx;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemDependencyDTOEx;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.usageratingscheme.DynamicAttributeLineWS;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by marcolin on 06/11/15.
 */
public class ItemBuilder extends AbstractBuilder {


	/**
	 * Contains different ORDER_LINE_TYPE.
	 * Constants will be used to create categories with different types.
	 * @author krunal Bhavsar
	 *
	 */
	public enum CategoryType {

		 	ORDER_LINE_TYPE_ITEM(Constants.ORDER_LINE_TYPE_ITEM),
		    ORDER_LINE_TYPE_TAX(Constants.ORDER_LINE_TYPE_TAX),
		    ORDER_LINE_TYPE_PENALTY(Constants.ORDER_LINE_TYPE_PENALTY),
		    ORDER_LINE_TYPE_DISCOUNT(Constants.ORDER_LINE_TYPE_DISCOUNT),
		    ORDER_LINE_TYPE_SUBSCRIPTION(Constants.ORDER_LINE_TYPE_SUBSCRIPTION),
		    ORDER_LINE_TYPE_TAX_QUOTE(Constants.ORDER_LINE_TYPE_TAX_QUOTE),
		    ORDER_LINE_TYPE_ADJUSTMENT(Constants.ORDER_LINE_TYPE_ADJUSTMENT);

		 	private Integer typeId ;

		 	private CategoryType(Integer typeId) {
		 		this.typeId = typeId;
		 	}
		 	public Integer getTypeId() {
		 		return this.typeId;
		 	}
	}

    private ItemBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static ItemBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        return new ItemBuilder(api, testEnvironment);
    }

    public ItemTypeBuilder itemType() {
        return new ItemTypeBuilder();
    }

    public UsageRatingSchemeBuilder usageRatingScheme() {
        return new UsageRatingSchemeBuilder();
    }

    public ProductBuilder item() {
        return new ProductBuilder();
    }

    public ItemDependencyBuilder itemDependency(){return new ItemDependencyBuilder();}

    public PlanBuilder plan() {
        return new PlanBuilder();
    }

    public class ItemTypeBuilder {
        private String code;
        private boolean global = false;
        private Integer allowAssetManagement = 0;
        private Integer[] entities;
        private boolean onePerOrder = false;
        private boolean onePerCustomer = false;
        private Boolean useExactCode = false;
        private Integer typeId;
        private Set<MetaFieldWS> assetMetaFields = new HashSet<>(0);

        public ItemTypeBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ItemTypeBuilder useExactCode(Boolean useExactCode) {
            this.useExactCode = useExactCode;
            return this;
        }

        public ItemTypeBuilder global(boolean global){
            this.global = global;
            return this;
        }

        public ItemTypeBuilder withCategoryType(CategoryType type) {
        	typeId = type.getTypeId();
        	return this;
        }
        public ItemTypeBuilder withOnePerOrder(boolean onePerOrder){
            this.onePerOrder = onePerOrder;
            return this;
        }

        public ItemTypeBuilder withOnePerCustomer(boolean onePerCustomer){
            this.onePerCustomer = onePerCustomer;
            return this;
        }

        public ItemTypeBuilder allowAssetManagement(Integer allowAssetManagement){
            this.allowAssetManagement = allowAssetManagement;
            return this;
        }

        public ItemTypeBuilder withEntities(Integer... entities){
            this.entities = entities;
            return this;
        }

        public ItemTypeBuilder withAssetMetaFields(Set<MetaFieldWS> assetMetaFields){
            this.assetMetaFields = assetMetaFields;
            return this;
        }

        public Integer build() {
            ItemTypeWS itemType = new ItemTypeWS();
            itemType.setDescription(useExactCode?code:"TestCategory: " + System.currentTimeMillis());
            itemType.setEntityId(api.getCallerCompanyId());
            itemType.setEntities(null == entities ? Arrays.asList(api.getCallerCompanyId()) : Arrays.asList(entities));
            itemType.setOrderLineTypeId(typeId!=null ? typeId : CategoryType.ORDER_LINE_TYPE_ITEM.getTypeId());
            itemType.setGlobal(global);
            itemType.setOnePerOrder(onePerOrder);
            itemType.setOnePerCustomer(onePerCustomer);
            itemType.setAllowAssetManagement(allowAssetManagement);
            itemType.setAssetMetaFields(assetMetaFields);
            if(allowAssetManagement ==  1){

                Set<AssetStatusDTOEx> assetStatusDTOExes = new HashSet<>();
                AssetStatusDTOEx assetStatusDTOEx  = new AssetStatusDTOEx();
                assetStatusDTOEx.setDescription("Available");
                assetStatusDTOEx.setIsDefault(1);
                assetStatusDTOEx.setIsOrderSaved(0);
                assetStatusDTOEx.setIsAvailable(1);
                assetStatusDTOEx.setIsActive(0);
                assetStatusDTOEx.setIsPending(0);
                assetStatusDTOExes.add(assetStatusDTOEx);

                assetStatusDTOEx = new AssetStatusDTOEx();
                assetStatusDTOEx.setDescription("InOrder");
                assetStatusDTOEx.setIsDefault(0);
                assetStatusDTOEx.setIsOrderSaved(1);
                assetStatusDTOEx.setIsAvailable(0);
                assetStatusDTOEx.setIsActive(1);
                assetStatusDTOEx.setIsPending(0);
                assetStatusDTOExes.add(assetStatusDTOEx);

                assetStatusDTOEx = new AssetStatusDTOEx();
                assetStatusDTOEx.setDescription("Pending");
                assetStatusDTOEx.setIsDefault(0);
                assetStatusDTOEx.setIsOrderSaved(1);
                assetStatusDTOEx.setIsAvailable(0);
                assetStatusDTOEx.setIsActive(0);
                assetStatusDTOEx.setIsPending(1);
                assetStatusDTOExes.add(assetStatusDTOEx);
                itemType.setAssetStatuses(assetStatusDTOExes);
            }

            Integer itemCategory = api.createItemCategory(itemType);
            testEnvironment.add(code, itemCategory, itemType.getDescription(), api, TestEntityType.PRODUCT_CATEGORY);
            return itemCategory;
        }
    }

    public class UsageRatingSchemeBuilder extends AbstractMetaFieldBuilder<UsageRatingSchemeBuilder> {

        private String ratingSchemeCode;
        private Integer entityId;
        private String ratingSchemeType;
        private Map<String, String> fixedAttributes = new HashMap<>();

        private boolean usesDynamicAttributes = false;
        private String dynamicAttributeName;
        private SortedSet<DynamicAttributeLineWS> dynamicAttributes = new TreeSet<>();
        private Integer currentSequence = 0;

        public UsageRatingSchemeBuilder withCode(String code) {
            this.ratingSchemeCode = code;
            return this;
        }

        public UsageRatingSchemeBuilder withType(String type) {
            this.ratingSchemeType = type;
            return this;
        }

        public UsageRatingSchemeBuilder withEntity(Integer entityId) {
            this.entityId = entityId;
            return this;
        }

        public UsageRatingSchemeBuilder withFixedAttributes(Map<String, String> fixedAttributes) {
            this.fixedAttributes = fixedAttributes;
            return this;
        }

        public UsageRatingSchemeBuilder withDynamicAttributesEnabled(String dynamicAttrName) {
            this.usesDynamicAttributes = true;
            this.dynamicAttributeName = dynamicAttrName;
            return this;
        }

        public UsageRatingSchemeBuilder addDynamicAttribute(Map<String, String> attributes) {
            if (usesDynamicAttributes) {
                DynamicAttributeLineWS dynAttr = new DynamicAttributeLineWS();
                dynAttr.setSequence(currentSequence++);
                dynAttr.setAttributes(attributes);

                this.dynamicAttributes.add(dynAttr);
            }
            return this;
        }

        public Integer build() {
            UsageRatingSchemeWS ratingSchemeWS = new UsageRatingSchemeWS();
            ratingSchemeWS.setEntityId(entityId);
            ratingSchemeWS.setRatingSchemeType(ratingSchemeType);
            ratingSchemeWS.setRatingSchemeCode("Test-" + ratingSchemeCode);
            ratingSchemeWS.setFixedAttributes(fixedAttributes);

            if (usesDynamicAttributes) {
                ratingSchemeWS.setUsesDynamicAttributes(usesDynamicAttributes);
                ratingSchemeWS.setDynamicAttributeName(dynamicAttributeName);
                ratingSchemeWS.setDynamicAttributes(dynamicAttributes);
            }

            Integer id = api.createUsageRatingScheme(ratingSchemeWS);
            testEnvironment.add(ratingSchemeCode, id, ratingSchemeWS.getRatingSchemeCode(),
                    api, TestEntityType.USAGE_RATING_SCHEME);
            return id;
        }
    }

    public class ProductBuilder extends AbstractMetaFieldBuilder<ProductBuilder>{
        private String code;
        private String description;
        private List<Integer> types = new ArrayList<>();
        private SortedMap<Date, PriceModelWS> prices = new TreeMap<>();
        private List<String> dependencyTypes = new ArrayList<>();
        private Boolean useExactCode = false;
        private boolean global = false;
        private boolean allowDecimal = false;
        private Integer assetManagementEnabled = Integer.valueOf(0);
        private Integer[] entities;
        private ItemDependencyDTOEx[] dependencies;
        private Date activeSince;
        private Date activeUntil;
        private Integer companyId;
        private List<MetaFieldWS> orderLineMetaFields = new ArrayList<>();
        private SortedMap<Date, RatingConfigurationWS> ratingConfigurations = new TreeMap<>();
        private boolean isPlan;

        public ProductBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ProductBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ProductBuilder withType(Integer type) {
            this.types.add(type);
            return this;
        }

        public ProductBuilder withTypes(List<Integer> types) {
            this.types.addAll(types);
            return this;
        }

        public ProductBuilder withActiveSince (Date activeSince) {
            this.activeSince = activeSince;
            return this;
        }

        public ProductBuilder withActiveUntil (Date activeUntil) {
            this.activeUntil = activeUntil;
            return this;
        }

        public ProductBuilder withFlatPrice(String flatPrice) {
            prices.put(new Date(),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(),
                            new BigDecimal(flatPrice), api.getCallerCurrencyId()));
            return this;
        }

        public ProductBuilder withZeroPrice(String zeroPrice) {
            prices.put(new Date(01/01/1970),
                    new PriceModelWS(PriceModelStrategy.ZERO.name(),
                            new BigDecimal(zeroPrice), api.getCallerCurrencyId()));
            return this;
        }

        public ProductBuilder withLinePercentage(String percentage) {
            prices.put(new Date(),
                    new PriceModelWS(PriceModelStrategy.LINE_PERCENTAGE.name(),
                            new BigDecimal(percentage), api.getCallerCurrencyId()));
            return this;
        }

        public ProductBuilder withLinePercentage(String percentage, Date date) {
            prices.put(date,
                    new PriceModelWS(PriceModelStrategy.LINE_PERCENTAGE.name(),
                            new BigDecimal(percentage), api.getCallerCurrencyId()));
            return this;
        }

        public ProductBuilder withChainPrice (String flatPrice) {

            PriceModelWS flatPrices = new PriceModelWS();
            PriceModelWS chainedPrice = new PriceModelWS();

            chainedPrice.setCurrencyId(api.getCallerCurrencyId());
            chainedPrice.setType(PriceModelStrategy.PERCENTAGE.name());
            chainedPrice.setRate("0.10");
            chainedPrice.addAttribute("percentage", "0.10");

            flatPrices.setCurrencyId(api.getCallerCurrencyId());
            flatPrices.setType(PriceModelStrategy.FLAT.name());
            flatPrices.setRate(flatPrice);
            flatPrices.setNext(chainedPrice);
            prices.put(new Date(), flatPrices);
            return this;
        }

        public ProductBuilder withTieredPrice(Map<String, String> tiers) {
            PriceModelWS tieredPrices = new PriceModelWS();
            tieredPrices.setCurrencyId(api.getCallerCurrencyId());
            tieredPrices.setType(PriceModelStrategy.TIERED.name());
            tieredPrices.setAttributes(tiers);
            prices.put(new Date(), tieredPrices);
            return this;
        }

        public ProductBuilder withGraduatedPrice(String graduatedPrice , String includedUnits){

            PriceModelWS graduatedPrices = new PriceModelWS();
            graduatedPrices.setCurrencyId(api.getCallerCurrencyId());
            graduatedPrices.setType(PriceModelStrategy.GRADUATED.name());
            graduatedPrices.setRate(graduatedPrice);
            graduatedPrices.addAttribute("included", includedUnits);
            prices.put(new Date(), graduatedPrices);
            return this;
        }

        public ProductBuilder withPooledPrice (String pooledPrice , String poolingItemId, String multiplier) {

            PriceModelWS pooledPrices = new PriceModelWS();
            pooledPrices.setCurrencyId(api.getCallerCurrencyId());
            pooledPrices.setType(PriceModelStrategy.POOLED.name());
            pooledPrices.setRate(pooledPrice);
            pooledPrices.addAttribute("pool_item_id", poolingItemId);
            pooledPrices.addAttribute("multiplier", multiplier);
            prices.put(new Date(), pooledPrices);
            return this;
        }

        public ProductBuilder withCompanyPooledPrice (String pooledPrice , String poolItemCategoryId, String includedQuantity) {

            PriceModelWS pooledPrices = new PriceModelWS();
            pooledPrices.setCurrencyId(api.getCallerCurrencyId());
            pooledPrices.setType(PriceModelStrategy.COMPANY_POOLED.name());
            pooledPrices.setRate(pooledPrice);
            pooledPrices.addAttribute("pool_item_category_id", poolItemCategoryId);
            pooledPrices.addAttribute("included_quantity", includedQuantity);
            prices.put(new Date(), pooledPrices);
            return this;
        }

        public ProductBuilder withCDRFieldBasedPercentagePrice(String fieldName , String percentage){

            PriceModelWS graduatedPrices = new PriceModelWS();
            graduatedPrices.setCurrencyId(api.getCallerCurrencyId());
            graduatedPrices.setType(PriceModelStrategy.FIELD_BASED.name());
            graduatedPrices.addAttribute("rate_pricing_field_name", fieldName);
            graduatedPrices.addAttribute("apply_percentage", percentage);
            prices.put(new Date(), graduatedPrices);
            return this;
        }

        public ProductBuilder global(boolean global){
            this.global = global;
            return this;
        }

        public ProductBuilder allowDecimal(boolean allowDecimal){
        	this.allowDecimal = allowDecimal;
        	return this;
        }

        public ProductBuilder withAssetManagementEnabled(Integer assetManagementEnabled){
            this.assetManagementEnabled = assetManagementEnabled;
            return this;
        }

        public ProductBuilder withCompany (Integer companyId) {
            this.companyId = companyId;
            return this;
        }

        public ProductBuilder withPriceModel(PriceModelWS priceModelWS){

            this.prices.put(new Date(),priceModelWS);
            return  this;
        }

        public ProductBuilder withDatePriceModel(Date date, PriceModelWS priceModelWS){
            this.prices.put(null != date ? date : new Date(), priceModelWS);
            return  this;
        }

        public ProductBuilder withDependencies(ItemDependencyDTOEx... itemDependencyDTOExes){
            this.dependencies = itemDependencyDTOExes;
            return this;
        }

        public ProductBuilder withEntities(Integer... entities){
            this.entities = entities;
            return this;
        }

        public ProductBuilder addOrderLineMetaField(MetaFieldWS metaField){
            this.orderLineMetaFields.add(metaField);
            return this;
        }

        public ProductBuilder withOrderLineMetaFields(List<MetaFieldWS> orderLineMetaFields){
            this.orderLineMetaFields = orderLineMetaFields;
            return this;
        }

        public ProductBuilder useExactCode(Boolean useExactCode) {
            this.useExactCode = useExactCode;
            return this;
        }

        public ProductBuilder isPlan(Boolean isPlan) {
            this.isPlan = isPlan;
            return this;
        }

        public ProductBuilder addRatingConfiguration(RatingConfigurationWS ratingConfiguration) {
            return addRatingConfigurationWithDate(new Date(0L), ratingConfiguration);
        }

        public ProductBuilder addRatingConfigurationWithDate(Date ratingDate, RatingConfigurationWS ratingConfiguration) {
            this.ratingConfigurations.put(null != ratingDate ? ratingDate : new Date(0L), ratingConfiguration);
            return this;
        }

        public Integer build() {
            ItemDTOEx item = new ItemDTOEx();
            item.setDescription((useExactCode && null != description)? description :"TestItem-" + System.currentTimeMillis());
            item.setNumber(useExactCode?code:"TestItem-" + System.currentTimeMillis());
            item.setTypes(types.toArray(new Integer[types.size()]));
            item.setExcludedTypes(new Integer[0]);
            item.setActiveSince(activeSince);
            item.setActiveUntil(activeUntil);
            item.setGlobal(global);
            item.setIsPlan(isPlan);
            item.setHasDecimals(allowDecimal ? 1 : 0);
            companyId = companyId != null ? companyId : api.getCallerCompanyId();
            item.setEntityId(companyId);
            item.setDeleted(0);
            item.setAssetManagementEnabled(assetManagementEnabled);
            item.setEntities(null == entities ? Arrays.asList(api.getCallerCompanyId()) : Arrays.asList(entities));
            if (prices.isEmpty()) {
                withFlatPrice("0.0");
            }
            item.setDefaultPrices(prices);
            List<Integer> entities = new ArrayList<>();
            entities.add(api.getCallerCompanyId());
            item.setEntities(entities);
            List<ItemDependencyDTOEx> dependencyTypeDTOs = new ArrayList<>();
            for (String categoryCodeForDependency : dependencyTypes) {
                ItemDependencyDTOEx dependencyDTOEx = new ItemDependencyDTOEx();
                dependencyDTOEx.setType(ItemDependencyType.ITEM_TYPE);
                dependencyDTOEx.setDependentId(testEnvironment.idForCode(categoryCodeForDependency));
                dependencyDTOEx.setDependentDescription("Test Dependency On Type");
                dependencyDTOEx.setMaximum(2);
                dependencyDTOEx.setMinimum(1);
                dependencyTypeDTOs.add(dependencyDTOEx);
            }
            //TODO: remove this check
            item.setDependencies(this.dependencies != null ? this.dependencies : dependencyTypeDTOs.toArray(new ItemDependencyDTOEx[0]));
            MetaFieldValueWS[] metaFieldValues = buildMetaField();
            item.setMetaFields(metaFieldValues);
            item.getMetaFieldsMap().put(companyId, metaFieldValues);
            item.setOrderLineMetaFields(orderLineMetaFields.toArray(new MetaFieldWS[orderLineMetaFields.size()]));

            item.setRatingConfigurations(ratingConfigurations);

            Integer itemId = api.createItem(item);
            testEnvironment.add(code, itemId, item.getNumber(),  api, TestEntityType.PRODUCT);
            return itemId;
        }
    }

    public class PlanBuilder extends AbstractMetaFieldBuilder<PlanBuilder> {
        private String code;
        private Integer planItemId;
        private List<PlanItemWS> planItems = new LinkedList<PlanItemWS>();

        public PlanBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public PlanBuilder withPlanItem(Integer itemId) {
            this.planItemId  = itemId;
            return this;
        }

        public PlanBuilder addBundleItem(Integer item, BigDecimal rate) {
            PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), rate, 1);
            SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
            models.put(Constants.EPOCH_DATE, priceModel);

            PlanItemBundleWS bundle = new PlanItemBundleWS();
            bundle.setPeriodId(1);
            bundle.setQuantity(BigDecimal.ZERO);

            PlanItemWS pi1 = new PlanItemWS();
            pi1.setItemId(item);
            pi1.setPrecedence(-1);
            pi1.setModels(models);
            pi1.setBundle(bundle);
            this.planItems.add(pi1);
            return this;
        }

        public Integer build() {

            PlanWS plan = new PlanWS();
            plan.setItemId(this.planItemId);
            plan.setDescription(code+"-" + System.currentTimeMillis());
            plan.setPeriodId(getMonthlyOrderPeriod());

            for(PlanItemWS item : planItems){
                plan.addPlanItem(item);
            }
            MetaFieldValueWS[] metaFieldValues = buildMetaField();
            plan.setMetaFields(metaFieldValues);
            plan.getMetaFieldsMap().put(api.getCallerCompanyId(), metaFieldValues);
            Integer itemId = api.createPlan(plan);
            testEnvironment.add(code, itemId, plan.getDescription(),  api, TestEntityType.PLAN);
            return itemId;
        }

        private Integer getMonthlyOrderPeriod(){
            OrderPeriodWS[] periods = api.getOrderPeriods();
            for(OrderPeriodWS period : periods){
                if(1 == period.getValue() &&
                        PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                    return period.getId();
                }
            }
            OrderPeriodWS monthly = new OrderPeriodWS();
            monthly.setEntityId(api.getCallerCompanyId());
            monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
            monthly.setValue(1);
            monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
            return api.createOrderPeriod(monthly);
        }
    }

    public class ItemDependencyBuilder {

        private Integer itemId;
        private Integer dependentId;
        private Integer minimum;
        private Integer maximum;
        private ItemDependencyType itemDependencyType;

        public ItemDependencyBuilder withItemId(Integer itemId){
            this.itemId = itemId;
            return this;
        }

        public ItemDependencyBuilder withDependentId(Integer dependentId){
            this.dependentId = dependentId;
            return this;
        }

        public ItemDependencyBuilder withMaximum(Integer maximum){
            this.maximum = maximum;
            return this;
        }

        public ItemDependencyBuilder withMinimum(Integer minimum ){
            this.minimum= minimum;
            return this;
        }

        public ItemDependencyBuilder withItemDependencyType (ItemDependencyType itemDependencyType){
            this.itemDependencyType = itemDependencyType;
            return this;
        }

        public ItemDependencyDTOEx build(){

            ItemDependencyDTOEx itemDependencyDTOEx = new ItemDependencyDTOEx();
            itemDependencyDTOEx.setItemId(this.itemId);
            itemDependencyDTOEx.setType(this.itemDependencyType);
            itemDependencyDTOEx.setDependentId(this.dependentId);
            itemDependencyDTOEx.setMaximum(this.maximum);
            itemDependencyDTOEx.setMinimum(this.minimum);

            return itemDependencyDTOEx;
        }
    }
}
