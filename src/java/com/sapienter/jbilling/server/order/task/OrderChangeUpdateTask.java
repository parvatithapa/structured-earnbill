/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.order.task;

import static com.sapienter.jbilling.common.Util.parseDate;
import static com.sapienter.jbilling.common.Util.truncateDate;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;

/**
 * This scheduled task will apply order changes to orders if possible.
 * On error all changes will be switched to ERROR state.
 * Each hierarchy changes group is applied in separate DB transaction
 *
 * @author Alexander Aksenov
 * @since 29.07.13
 */
public class OrderChangeUpdateTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FUTURE_DATE = "future_date";

    @Override
    public String getTaskName() {
        return "order change update task , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        _init(context);
        IOrderSessionBean orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        Date onDate = getDate();
        IMethodTransactionalWrapper txExecuteWrapper = Context.getBean(IMethodTransactionalWrapper.class);
        List<Map<String, Object>> orderChangesSearchResult = txExecuteWrapper.execute(() -> new OrderChangeDAS().findApplicableChangesForGrouping(getEntityId(), onDate));
        if(!orderChangesSearchResult.isEmpty()) {
            List<Collection<Integer>> changeGroups = findOrderChangesGrouped(orderChangesSearchResult);
            for (Collection<Integer> group : changeGroups) {
                try {
                    orderSessionBean.applyChangesToOrders(group, onDate, getEntityId());
                } catch (SessionInternalError ex) {
                    logger.error("Unexpected error during changes apply", ex);
                    String errorMessage = ex.getErrorMessages() != null && ex.getErrorMessages().length > 0 ? ex.getErrorMessages()[0] : null;
                    orderSessionBean.markOrderChangesAsApplyError(getEntityId(), group, onDate, null, errorMessage);
                } catch (Exception ex) {
                    logger.error("Error during changes apply to hierarchy", ex);
                    orderSessionBean.markOrderChangesAsApplyError(getEntityId(), group, onDate, null, null);
                }
            }
        }
    }

    /**
     * Group order changes by orders hierarchy. Order changes for orders' tree (hierarchy) should be processed together
     * @param orderChangesSearchResult List of prepared for grouping order changes data in format
     *          {changeId: <order_change_id>, orderId: <order_change_order_id>, parentOrderId: <parent_for_change_order>,
     *          grandParentOrderId: <grand_parent_for_change_order>}
     *          parentOrderId and grandParentOrderId should be null if change order is the root order in hierarchy
     * @return List of order change id groups
     */
    protected List<Collection<Integer>> findOrderChangesGrouped(List<Map<String, Object>> orderChangesSearchResult) {
        Map<Integer, String> ordersHierarchyMap = new HashMap<>();
        Map<String, Set<Integer>> hierarchyToChangesMap = new HashMap<>();
        for (Map<String, Object> record : orderChangesSearchResult) {
            Integer changeId = (Integer) record.get("changeId");
            Integer orderId = (Integer) record.get("orderId");
            Integer parentOrderId = (Integer) record.get("parentOrderId");
            Integer grandParentOrderId = (Integer) record.get("grandParentOrderId");
            String key = findOrCreateHierarchyKey(ordersHierarchyMap, orderId, parentOrderId, grandParentOrderId);

            if (!hierarchyToChangesMap.containsKey(key)) {
                hierarchyToChangesMap.put(key, new HashSet<>());
            }
            hierarchyToChangesMap.get(key).add(changeId);
        }
        return new LinkedList<>(hierarchyToChangesMap.values());
    }

    /**
     * Find key for given order in hierarchyMap
     * @param hierarchyMap Map of orderIds to hierarchy tree key
     * @param orderId target order id
     * @param parentOrderId parent order id for target order
     * @param gransParentOrderId grand parent order id for target order
     * @return key for hierarchy tree if found
     */
    private String findHierarchyKey(Map<Integer, String> hierarchyMap, Integer orderId, Integer parentOrderId, Integer gransParentOrderId) {
        String key = hierarchyMap.get(orderId);
        if (key == null && parentOrderId != null) {
            key = hierarchyMap.get(parentOrderId);
        }
        if (key == null && gransParentOrderId != null) {
            key = hierarchyMap.get(gransParentOrderId);
        }
        return key;
    }

    /**
     * Find or generate hierarchy tree key for given order. Fill hierarchy map if key was generated
     * @param hierarchyMap Map of orderIds to hierarchy tree key
     * @param orderId target order id
     * @param parentOrderId parent order id for target order
     * @param gransParentOrderId grand parent order id for target order
     * @return key (GUID) for given orde
     */
    private String findOrCreateHierarchyKey(Map<Integer, String> hierarchyMap, Integer orderId, Integer parentOrderId, Integer gransParentOrderId) {
        String key = findHierarchyKey(hierarchyMap, orderId, parentOrderId, gransParentOrderId);
        if (key == null) {
            key = UUID.randomUUID().toString();
            hierarchyMap.put(orderId, key);
        }
        if (parentOrderId != null) {
            hierarchyMap.put(parentOrderId, key);
        }
        if (gransParentOrderId != null) {
            hierarchyMap.put(gransParentOrderId, key);
        }
        return key;
    }

    private Date getDate(){
        String futureDateValue = parameters.get(FUTURE_DATE);
        if(futureDateValue != null){
            return parseDate(futureDateValue);
        } else {
            return truncateDate(companyCurrentDate());
        }
    }
}
