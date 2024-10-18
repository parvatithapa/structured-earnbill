package com.sapienter.jbilling.api.automation.plans;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import org.joda.time.DateTime;

/**
 * Created by dario on 01/07/16.
 */
class PlansProductPricingTestHelper {
    
    private EnvironmentHelper envHelper;

    public PlansProductPricingTestHelper () {}

    public Integer buildAndPersistDefaultMonthlyCustomer(String username, Date nextInvoiceDate, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder){

        UserWS userWS = testEnvironmentBuilder
                .customerBuilder(api)
                .withUsername(username)
                .addTimeToUsername(false)
                .build();

        if (null != nextInvoiceDate){
            DateTime nid = new DateTime(nextInvoiceDate);
            userWS.setMainSubscription(new MainSubscriptionWS(envHelper.getOrderPeriodMonth(api),
                    nid.getDayOfMonth()));
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
        }

        return userWS.getId();
    }

    
    public Integer buildAndPersistCategory(String code, boolean global, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .itemType()
                .withCode(code)
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistCategoryWithAssetManagementEnabled(String code, boolean global, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder){
        return testEnvironmentBuilder
                .itemBuilder(api)
                .itemType()
                .withCode(code)
                .allowAssetManagement(1)
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistCategoryOnePerOrder(String code, boolean global, boolean onePerOrder, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder){
        return testEnvironmentBuilder
                .itemBuilder(api)
                .itemType()
                .withCode(code)
                .withOnePerOrder(onePerOrder)
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistCategoryOnePerCustomer (String code, boolean global, boolean onePerCustomer, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .itemType()
                .withCode(code)
                .withOnePerCustomer(onePerCustomer)
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistFlatProduct(String code, boolean global, Integer categoryId,
                                              BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistFlatProductWithAssetManagementEnabled(String code, boolean global, Integer categoryId,
                                                                        BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withAssetManagementEnabled(1)
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .build();
    }


    
    public Integer buildAndPersistGraduatedProduct(String code, boolean global, Integer categoryId,
                                                   BigDecimal graduatedPrice, String includedUnits, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return  testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withType(categoryId)
                .withCode(code)
                .withGraduatedPrice(String.valueOf(graduatedPrice), includedUnits)
                .global(global)
                .build();
    }


    
    public Integer buildAndPersistFlatProduct(String code, boolean global, Integer categoryId,
                                              BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder, List<MetaFieldWS> orderLineMetaFields) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .withOrderLineMetaFields(orderLineMetaFields)
                .build();
    }

    
    public Integer buildAndPersistFlatProductMultipleCategories(String code, boolean global, Integer categoryId, Integer categoryId2,
                                                                BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withType(categoryId2)
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .build();
    }


    
    public Integer buildAndPersistFlatProductMultipleCategories(String code, boolean global, Integer categoryId, Integer categoryId2,
                                                                BigDecimal flatPrice, List<MetaFieldWS> metaFieldWSes ,JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withType(categoryId2)
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .withOrderLineMetaFields(metaFieldWSes)
                .build();
    }

    
    public Integer buildAndPersistFlatProductWithCompany(String code, boolean global, Integer categoryId, Integer companyId,
                                                         BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(String.valueOf(flatPrice))
                .withCompany(companyId)
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistPooledProduct (String code, boolean global, Integer categoryId,
                                                 BigDecimal pooledPrice, Integer poolingItemId, Integer multiplier, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withPooledPrice(String.valueOf(pooledPrice), String.valueOf(poolingItemId), String.valueOf(multiplier))
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistCompanyPooledProduct (String code, boolean global, Integer categoryId,
                                                        BigDecimal companyPooledPrice, Integer poolItemCategoryId, Integer includedQuantity, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withCompanyPooledPrice(String.valueOf(companyPooledPrice), String.valueOf(poolItemCategoryId), String.valueOf(includedQuantity))
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistFlatProductWithChain(String code, boolean global, Integer categoryId,
                                                       BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withChainPrice(String.valueOf(flatPrice))
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistFlatProductWithDate(String code, boolean global, Integer categoryId,
                                                      BigDecimal flatPrice, Date activeSince, Date activeUntil, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {
        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(String.valueOf(flatPrice))
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .global(global)
                .build();
    }


    
    public Integer buildAndPersistProductWithDependency (String code, boolean global, Integer categoryId, Integer dependentId,
                                                         Integer minimum, Integer maximum, BigDecimal flatPrice, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder) {

        return testEnvironmentBuilder
                .itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withDependencies(
                        testEnvironmentBuilder
                                .itemBuilder(api)
                                .itemDependency()
                                .withDependentId(dependentId)
                                .withMinimum(minimum)
                                .withMaximum(maximum)
                                .withItemDependencyType(ItemDependencyType.ITEM)
                                .build())
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .build();
    }

    
    public Integer buildAndPersistUsagePool(String code, String name,
                                            String quantity, String resetValue,
                                            List<Integer> itemIds, List<Integer> itemTypesIds,
                                            JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder, UsagePoolConsumptionActionWS... consumptionActions) {
        return testEnvironmentBuilder
                .usagePoolBuilder(api, code)
                .withName(name)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                .withCyclePeriodValue(Integer.valueOf(1))
                .withQuantity(quantity)
                .withResetValue(resetValue)
                .withItemIds(itemIds)
                .withItemTypesIds(itemTypesIds)
                .withConsumptionActions(Arrays.asList(consumptionActions))
                .build();
    }

    
    public Integer buildAndPersistPlan(String code, String description,
                                       Integer periodId, Integer itemId,
                                       JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder, List<Integer> usagePools,
                                       PlanItemWS... planItems) {
        return testEnvironmentBuilder
                .planBuilder(api, code)
                .withDescription(description)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    
    public Integer buildAndPersistPlan(String code, String description,
                                       Integer periodId, Integer itemId,
                                       JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder,
                                       PlanItemWS... planItems) {
        return testEnvironmentBuilder
                .planBuilder(api, code)
                .withDescription(description)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    
    public Integer buildAndPersistPlanWithMetaFields(String code, String description,
                                                     Integer periodId, Integer itemId,
                                                     JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder, List<Integer> usagePools,
                                                     List<MetaFieldValueWS> metaFieldValues, PlanItemWS... planItems) {
        return testEnvironmentBuilder
                .planBuilder(api, code)
                .withDescription(description)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .withMetaFields(metaFieldValues)
                .build().getId();
    }

    
    public Integer buildAndPersistPlanWithMetaFields(String code, String description,
                                                     Integer periodId, Integer itemId,
                                                     JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder,
                                                     List<MetaFieldValueWS> metaFieldValues, PlanItemWS... planItems) {
        return testEnvironmentBuilder
                .planBuilder(api, code)
                .withDescription(description)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withPlanItems(Arrays.asList(planItems))
                .withMetaFields(metaFieldValues)
                .build().getId();
    }

    
    public Integer buildAndPersistOrder(String code, Integer userId, Date activeSince,
                                        Date activeUntil, Integer orderPeriodId, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder,
                                        Integer... productsIds) {

        return testEnvironmentBuilder
                .orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withPeriod(orderPeriodId)
                .withProducts(productsIds)
                .build();
    }

    
    public Integer buildAndPersistOrder(String code, Integer userId, Date activeSince,
                                        Date activeUntil, Integer orderPeriodId,
                                        JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder, Map<Integer, BigDecimal> productQuantityMap) {
        OrderBuilder orderBuilder = testEnvironmentBuilder
                .orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withPeriod(orderPeriodId);

        for (Map.Entry<Integer, BigDecimal> entry : productQuantityMap.entrySet()){
            orderBuilder.withOrderLine(
                    orderBuilder.orderLine()
                            .withItemId(entry.getKey())
                            .withQuantity(entry.getValue())
                            .build());
        }

        return orderBuilder.build();
    }

    
    public Integer buildAndPersistOrderWithOrderLines(String code, Integer userId, Date activeSince,
                                                      Date activeUntil, Integer orderPeriodId, JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder,
                                                      OrderLineWS... orderLines) {
        return testEnvironmentBuilder
                .orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withPeriod(orderPeriodId)
                .withOrderLines(Arrays.asList(orderLines))
                .build();
    }

}
