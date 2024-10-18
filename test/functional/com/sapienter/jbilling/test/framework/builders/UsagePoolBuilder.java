package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vojislav Stanojevikj
 * @since 15-Jun-2016.
 */
public class UsagePoolBuilder extends AbstractBuilder{

    private UsagePoolBuilder(JbillingAPI api, TestEnvironment testEnvironment, String code) {
        super(api, testEnvironment);
        this.code = code;
    }

    public static UsagePoolBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment, String code){
        return new UsagePoolBuilder(api, testEnvironment, code);
    }

    public static UsagePoolConsumptionActionBuilder consumptionActionBuilder(){
        return new UsagePoolConsumptionActionBuilder();
    }

    private String code;
    private String name;
    private String quantity;
    private String resetValue;
    private String cyclePeriodUnit;
    private Integer cyclePeriodValue;
    private List<Integer> itemsIds = new ArrayList<>();
    private List<Integer> itemTypesIds = new ArrayList<>();
    private List<UsagePoolConsumptionActionWS> consumptionActions = new ArrayList<>();


    public UsagePoolBuilder withName(String name){
        this.name = name;
        return this;
    }

    public UsagePoolBuilder withQuantity(String quantity){
        this.quantity = quantity;
        return this;
    }

    public UsagePoolBuilder withResetValue(String resetValue){
        this.resetValue = resetValue;
        return this;
    }

    public UsagePoolBuilder addItemId(Integer itemId){
        this.itemsIds.add(itemId);
        return this;
    }

    public UsagePoolBuilder withItemIds(List<Integer> itemsIds){
        this.itemsIds = itemsIds;
        return this;
    }

    public UsagePoolBuilder addItemTypeId(Integer itemTypeId){
        this.itemTypesIds.add(itemTypeId);
        return this;
    }

    public UsagePoolBuilder withItemTypesIds(List<Integer> itemTypesIds){
        this.itemTypesIds = itemTypesIds;
        return this;
    }

    public UsagePoolBuilder withCyclePeriodUnit(String cyclePeriodUnit){
        this.cyclePeriodUnit = cyclePeriodUnit;
        return this;
    }

    public UsagePoolBuilder withCyclePeriodValue(Integer cyclePeriodValue){
        this.cyclePeriodValue = cyclePeriodValue;
        return this;
    }

    public UsagePoolBuilder addConsumptionAction(UsagePoolConsumptionActionWS consumptionAction){
        this.consumptionActions.add(consumptionAction);
        return this;
    }

    public UsagePoolBuilder withConsumptionActions(List<UsagePoolConsumptionActionWS> consumptionActions){
        this.consumptionActions = consumptionActions;
        return this;
    }

    public Integer build(){
        UsagePoolWS usagePool = new UsagePoolWS();

        usagePool.setName(name);
        usagePool.setQuantity(quantity);
        usagePool.setCyclePeriodUnit(cyclePeriodUnit);
        usagePool.setCyclePeriodValue(cyclePeriodValue);
        usagePool.setItemTypes(itemTypesIds.toArray(new Integer[itemTypesIds.size()]));
        usagePool.setItems(itemsIds.toArray(new Integer[itemsIds.size()]));
        usagePool.setEntityId(api.getCallerCompanyId());
        usagePool.setUsagePoolResetValue(resetValue);
        usagePool.setConsumptionActions(consumptionActions);

        Integer poolId = api.createUsagePool(usagePool);
        testEnvironment.add(code, poolId, name, api, TestEntityType.USAGE_POOL);

        return poolId;
    }

    public static class UsagePoolConsumptionActionBuilder{

        private String percentage;
        private String type;
        private String notificationId;
        private NotificationMediumType mediumType;
        private String productId;

        public UsagePoolConsumptionActionBuilder withPercentage(String percentage){
            this.percentage = percentage;
            return this;
        }

        public UsagePoolConsumptionActionBuilder withType(String type){
            this.type = type;
            return this;
        }

        public UsagePoolConsumptionActionBuilder withNotificationId(String notificationId){
            this.notificationId = notificationId;
            return this;
        }

        public UsagePoolConsumptionActionBuilder withMediumType(NotificationMediumType mediumType){
            this.mediumType = mediumType;
            return this;
        }

        public UsagePoolConsumptionActionBuilder withProductId(String productId){
            this.productId = productId;
            return this;
        }

        public UsagePoolConsumptionActionWS build(){

            UsagePoolConsumptionActionWS consumptionAction = new UsagePoolConsumptionActionWS();
            consumptionAction.setMediumType(mediumType);
            consumptionAction.setNotificationId(notificationId);
            consumptionAction.setPercentage(percentage);
            consumptionAction.setProductId(productId);
            consumptionAction.setType(type);

            return consumptionAction;
        }

    }
}
