package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Vojislav Stanojevikj
 * @since 15-Jun-2016.
 */
public class PlanBuilder extends AbstractBuilder {

    private String code;
    private Integer itemId;
    private Integer periodId;
    private String description;
    private List<PlanItemWS> planItems = new ArrayList<>();
    private List<Integer> usagePoolsIds = new ArrayList<>();
    private List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
    private boolean freeTrial;
    private Integer numberOfFreeCalls;
    private Integer freeTrialPeriodValue;
    private String freeTrialPeriodUnit;


    private PlanBuilder(JbillingAPI api, TestEnvironment testEnvironment, String code) {
        super(api, testEnvironment);
        this.code = code;
    }

    public static PlanBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment, String code){
        return new PlanBuilder(api, testEnvironment, code);
    }

    public PlanBuilder withItemId(Integer itemId){
        this.itemId = itemId;
        return this;
    }

    public PlanBuilder withPeriodId(Integer periodId){
        this.periodId = periodId;
        return this;
    }

    public PlanBuilder withDescription(String description){
        this.description = description;
        return this;
    }

    public PlanBuilder withFreeTrial(boolean freeTrial){
        this.freeTrial = freeTrial;
        return this;
    }

    public PlanBuilder withNumberOfFreeCalls(Integer numberOfFreeCalls) {
        this.numberOfFreeCalls = numberOfFreeCalls;
        return this;
    }

    public PlanBuilder withFreeTrialPeriodUnit(String freeTrialPeriodUnit) {
        this.freeTrialPeriodUnit = freeTrialPeriodUnit;
        return this;
    }

    public PlanBuilder withFreeTrialPeriodValue(Integer freeTrialPeriodValue) {
        this.freeTrialPeriodValue = freeTrialPeriodValue;
        return this;
    }

    public PlanBuilder addPlanItem(PlanItemWS planItem){
        this.planItems.add(planItem);
        return this;
    }

    public PlanBuilder withPlanItems(List<PlanItemWS> planItems){
        this.planItems = planItems;
        return this;
    }

    public PlanBuilder addUsagePoolId(Integer usagePoolId){
        this.usagePoolsIds.add(usagePoolId);
        return this;
    }

    public PlanBuilder withUsagePoolsIds(List<Integer> usagePoolsIds){
        this.usagePoolsIds = usagePoolsIds;
        return this;
    }

    public PlanBuilder addMetaFieldValue(MetaFieldValueWS metaFieldValue){
        this.metaFieldValues.add(metaFieldValue);
        return this;
    }

    public PlanBuilder withMetaFields(List<MetaFieldValueWS> metaFieldValues){
        this.metaFieldValues = metaFieldValues;
        return this;
    }

    public PlanWS build(){

        PlanWS plan = new PlanWS();
        plan.setItemId(itemId);
        plan.setPeriodId(periodId);
        plan.setDescription(description);
        plan.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[metaFieldValues.size()]));
        plan.setPlanItems(planItems);
        plan.setUsagePoolIds(usagePoolsIds.toArray(new Integer[usagePoolsIds.size()]));
        plan.setFreeTrial(freeTrial);
        plan.setFreeTrialPeriodUnit(freeTrialPeriodUnit);
        plan.setFreeTrialPeriodValue(freeTrialPeriodValue);
//        plan.setNumberOfFreeCalls(numberOfFreeCalls);
        Integer planId = api.createPlan(plan);

        testEnvironment.add(code, planId, description, api, TestEntityType.PLAN);

        return api.getPlanWS(planId);
    }

    public static class PlanItemBuilder{

        private static final Integer DEFAULT_PRECEDENCE = Integer.valueOf(-1);

        private Integer itemId;
        private SortedMap<Date, PriceModelWS> models = new TreeMap<>();
        private PriceModelWS model;
        private String bundledQuantity;
        private Integer bundledPeriodId;
        private Integer precedence = DEFAULT_PRECEDENCE;

        private PlanItemBuilder(){}

        public static PlanItemBuilder getBuilder(){
            return new PlanItemBuilder();
        }


        public PlanItemBuilder withItemId(Integer itemId){
            this.itemId = itemId;
            return this;
        }

        public PlanItemBuilder withModel(PriceModelWS model){
            this.model = model;
            return this;
        }

        public PlanItemBuilder withBundledQuantity(String bundledQuantity){
            this.bundledQuantity = bundledQuantity;
            return this;
        }

        public PlanItemBuilder withBundledPeriodId(Integer bundledPeriodId){
            this.bundledPeriodId = bundledPeriodId;
            return this;
        }

        public PlanItemBuilder addModel(PriceModelWS model){
            this.models.put(new Date(), model);
            return this;
        }

        public PlanItemBuilder addModel(Date date, PriceModelWS model){
            this.models.put(date, model);
            return this;
        }

        public PlanItemBuilder withModels(SortedMap<Date, PriceModelWS> models){
            this.models = models;
            return this;
        }

        public PlanItemBuilder withPrecedence(Integer precedence){
            this.precedence = precedence;
            return this;
        }

        public PlanItemWS build(){

            PlanItemWS planItem = new PlanItemWS();
            planItem.setItemId(itemId);
            planItem.setModel(model);
            planItem.setModels(models);
            planItem.setPrecedence(precedence);
            PlanItemBundleWS planItemBundle = new PlanItemBundleWS();
            planItemBundle.setPeriodId(bundledPeriodId);
            planItemBundle.setQuantity(bundledQuantity);
            planItem.setBundle(planItemBundle);

            return planItem;
        }

    }
}
