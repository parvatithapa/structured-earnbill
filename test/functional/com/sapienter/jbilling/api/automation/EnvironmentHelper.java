package com.sapienter.jbilling.api.automation;

import com.sapienter.jbilling.api.automation.orders.OrdersTestHelper;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;

import static com.sapienter.jbilling.test.TestUtils.buildDescriptions;

/**
 * Immutable instance for
 * a set of APIs.
 * Meant to be used as a helper
 * object to obtain order periods id
 * order change statuses etc.
 *
 * @author Vojislav Stanojevikj
 * @since 15-JUN-2016.
 */
public final class EnvironmentHelper {

    private EnvironmentHelper(Set<JbillingAPI> apis){
        initializeCachedInstances(apis);
    }

    /**
     * Creates and returns a new immutable
     * instance for one or more APIs.
     *
     * @param api required api
     * @param apis optional apis
     * @return new instance of EnvironmentHelper.
     */
    public static EnvironmentHelper getInstance(JbillingAPI api, JbillingAPI... apis){
        Set<JbillingAPI> apiSet = new HashSet<>(Arrays.asList(api));
        if (null != apis && apis.length > 0){
            apiSet.addAll(Arrays.asList(apis));
        }
        return new EnvironmentHelper(apiSet);
    }

    private enum JBillingEntityType{
        ORDER_PERIOD_MONTH(TestEntityType.ORDER_PERIOD, "Monthly"),
        ORDER_PERIOD_DAY(TestEntityType.ORDER_PERIOD, "Daily"),
        ORDER_PERIOD_WEEK(TestEntityType.ORDER_PERIOD, "Weekly"),
        ORDER_PERIOD_ONE_TIME(TestEntityType.ORDER_PERIOD, "One Time"),
        ORDER_CHANGE_STATUS_APPLY(TestEntityType.ORDER_CHANGE_STATUS, "Apply"),
        USAGE_POOL_CONSUMPTION_NOTIFICATION_TYPE(TestEntityType.NOTIFICATION_TYPE, "Usage Pool Notification"),
        USER_OVERDUE_NOTIFICATION_TYPE(TestEntityType.NOTIFICATION_TYPE, "User Status Notification"),
        ORDER_CHANGE_TYPE(TestEntityType.ORDER_CHANGE_TYPE, "Default Order Change Type");

        TestEntityType testEntityType;
        String testEntityName;

        JBillingEntityType(TestEntityType testEntityType, String testEntityName) {
            this.testEntityType = testEntityType;
            this.testEntityName = testEntityName;
        }
    }

    private final Map<Integer, Map<JBillingEntityType, Integer>> cachedInstancesIds = new HashMap<>();


    public Integer getOrderPeriodOneTime(JbillingAPI api) {
        safeApi(api);
        return findEntityId(api, JBillingEntityType.ORDER_PERIOD_ONE_TIME,
                JBillingEntityType.ORDER_PERIOD_ONE_TIME.testEntityName);
    }

    public Integer getOrderPeriodMonth(JbillingAPI api){
        safeApi(api);
        return findEntityId(api, JBillingEntityType.ORDER_PERIOD_MONTH,
                JBillingEntityType.ORDER_PERIOD_MONTH.testEntityName);
    }

    public Integer getOrderPeriodWeek(JbillingAPI api){
        safeApi(api);
        return findEntityId(api, JBillingEntityType.ORDER_PERIOD_WEEK,
                JBillingEntityType.ORDER_PERIOD_WEEK.testEntityName);
    }

    public Integer getOrderPeriodDay(JbillingAPI api){
        safeApi(api);
        return findEntityId(api, JBillingEntityType.ORDER_PERIOD_DAY,
                JBillingEntityType.ORDER_PERIOD_DAY.testEntityName);
    }

    public Integer getOrderChangeStatusApply(JbillingAPI api){
        safeApi(api);
        return findEntityId(api, JBillingEntityType.ORDER_CHANGE_STATUS_APPLY,
                JBillingEntityType.ORDER_CHANGE_STATUS_APPLY.testEntityName);
    }

    public Integer getDefaultOrderChangeType(JbillingAPI api){
        safeApi(api);
        return findEntityId(api, JBillingEntityType.ORDER_CHANGE_TYPE,
                JBillingEntityType.ORDER_CHANGE_TYPE.testEntityName);
    }

    public Integer getUsagePoolNotificationId(JbillingAPI api) {
        safeApi(api);
        return findEntityId(api, JBillingEntityType.USAGE_POOL_CONSUMPTION_NOTIFICATION_TYPE,
                JBillingEntityType.USAGE_POOL_CONSUMPTION_NOTIFICATION_TYPE.testEntityName);
    }

    public Integer getUserOverdueNotificationId(JbillingAPI api) {
        safeApi(api);
        return findEntityId(api, JBillingEntityType.USER_OVERDUE_NOTIFICATION_TYPE,
                JBillingEntityType.USER_OVERDUE_NOTIFICATION_TYPE.testEntityName);
    }

    private void initializeCachedInstances(Set<JbillingAPI> apis){

        apis.forEach(api -> {
            Map<JBillingEntityType, Integer> entitiesMap = new EnumMap<>(JBillingEntityType.class);
            // Order periods
            entitiesMap.put(JBillingEntityType.ORDER_PERIOD_ONE_TIME, Constants.ORDER_PERIOD_ONCE);
            entitiesMap.put(JBillingEntityType.ORDER_PERIOD_MONTH, OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(
                    Constants.PERIOD_UNIT_MONTH, 1, JBillingEntityType.ORDER_PERIOD_MONTH.testEntityName, api).getId());
            entitiesMap.put(JBillingEntityType.ORDER_PERIOD_WEEK, OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(
                    Constants.PERIOD_UNIT_WEEK, 1, JBillingEntityType.ORDER_PERIOD_WEEK.testEntityName, api).getId());
            entitiesMap.put(JBillingEntityType.ORDER_PERIOD_DAY, OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(
                    Constants.PERIOD_UNIT_DAY, 1, JBillingEntityType.ORDER_PERIOD_DAY.testEntityName, api).getId());
            // Order change status apply
            entitiesMap.put(JBillingEntityType.ORDER_CHANGE_STATUS_APPLY,
                    getOrCreateOrderChangeStatus(api, ApplyToOrder.YES, Integer.valueOf(1)));
            // Default order change type
            entitiesMap.put(JBillingEntityType.ORDER_CHANGE_TYPE, getOrCreateDefaultOrderChangeType(api));
            // Usage pool consumption notification
            entitiesMap.put(JBillingEntityType.USAGE_POOL_CONSUMPTION_NOTIFICATION_TYPE,
                    Constants.NOTIFICATION_TYPE_USAGE_POOL_CONSUMPTION);
            // User overdue notification
            entitiesMap.put(JBillingEntityType.USER_OVERDUE_NOTIFICATION_TYPE,
                    Constants.NOTIFICATION_TYPE_USER_OVERDUE);
            cachedInstancesIds.put(api.getCallerCompanyId(), entitiesMap);
        });

    }

    private Integer getOrCreateOrderChangeStatus(JbillingAPI api, ApplyToOrder applyToOrder, Integer order){
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for(OrderChangeStatusWS orderChangeStatus : list){
            if(orderChangeStatus.getApplyToOrder().equals(applyToOrder)
                    && orderChangeStatus.getEntityId()!=null){
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if(statusId != null){
            return statusId;
        }else{
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(applyToOrder);
            newStatus.setDeleted(0);
            newStatus.setEntityId(api.getCallerCompanyId());
            newStatus.setOrder(order);
            newStatus.addDescription(new InternationalDescriptionWS(api.getCallerLanguageId(), "status"+order));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private Integer getOrCreateDefaultOrderChangeType(JbillingAPI api){

        OrderChangeTypeWS[] types = api.getOrderChangeTypesForCompany();
        for (OrderChangeTypeWS type : types){
            if (type.isDefaultType()){
                return type.getId();
            }
        }

        OrderChangeTypeWS orderChangeType = new OrderChangeTypeWS();
        orderChangeType.setEntityId(api.getCallerCompanyId());
        orderChangeType.setAllowOrderStatusChange(false);
        orderChangeType.setDefaultType(true);
        orderChangeType.setName("Default");

        return api.createUpdateOrderChangeType(orderChangeType);
    }
    private void safeApi(JbillingAPI api){
        if (null == api || !cachedInstancesIds.containsKey(api.getCallerCompanyId())){
            throw new IllegalArgumentException("Supplied API can not be null!");
        }
    }

    private Integer findEntityId(JbillingAPI api, JBillingEntityType entityType, String entityName){
        for (Map.Entry<JBillingEntityType, Integer> entry : cachedInstancesIds.get(api.getCallerCompanyId()).entrySet()){
            if (entry.getKey() == entityType && entry.getKey().testEntityName.equals(entityName)){
                return entry.getValue();
            }
        }
        throw new IllegalStateException("Required entity not found!");
    }

}
