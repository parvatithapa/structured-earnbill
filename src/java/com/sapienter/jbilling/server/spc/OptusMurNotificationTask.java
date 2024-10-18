package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.OptusMurJMREvent;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.task.UsageOrderReRater;
import com.sapienter.jbilling.server.util.Context;

public class OptusMurNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] { OptusMurJMREvent.class };

    @Override
    public void process(Event event) throws PluggableTaskException {
        try {
            OptusMurJMREvent murJMREvent = (OptusMurJMREvent) event;
            JbillingMediationRecord jmr = murJMREvent.getJmr();
            String assetNumber = jmr.getSource();
            SPCMediationHelperService service = Context.getBean(SPCMediationHelperService.class);
            Optional<Integer> orderId = service.findOrderByAssetNumber(assetNumber);
            if(!orderId.isPresent()) {
                logger.debug("Subscription Order not found for user {} for number {}", jmr.getUserId(), assetNumber);
                return;
            }
            OrderDTO subscriptionOrder = new OrderDAS().find(orderId.get());
            Map<UsageOrderReRater.UsagePeriod, Date> customerPeriodMap = UsageOrderReRater.getBillablePeriodFromSubscriptionOrder(subscriptionOrder);
            Date startDate = customerPeriodMap.get(UsageOrderReRater.UsagePeriod.CYCLE_START_DATE);
            Date endDate = customerPeriodMap.get(UsageOrderReRater.UsagePeriod.CYCLE_END_DATE);
            Integer userId = jmr.getUserId();
            Integer itemId = jmr.getItemId();
            String cdrType = jmr.getCdrType();
            Integer customerId = subscriptionOrder.getBaseUserByUserId().getCustomer().getId();
            List<CustomerUsagePoolDTO> customerUsagePools = new CustomerUsagePoolDAS().getCustomerUsagePoolsByCustomerIdAndDateRange(customerId, startDate, endDate);
            JMRRepository jmrRepository = Context.getBean(JMRRepository.class);
            BigDecimal oldUsageQuanity = jmrRepository.sumOfJMRQuantityForUserItemAndCdrTypeForDateRange(userId, itemId, cdrType,
                    JbillingMediationRecord.STATUS.PROCESSED.name(), false, startDate, endDate);
            if(oldUsageQuanity == null) {
                oldUsageQuanity = BigDecimal.ZERO;
            }
            ItemDTO mediatedItem = new ItemDAS().findNow(itemId);
            BigDecimal newUsageQuantity = oldUsageQuanity.add(jmr.getQuantity());
            for(CustomerUsagePoolDTO customerUsagePoolDTO : customerUsagePools) {
                if(customerUsagePoolDTO.getAllItems().contains(mediatedItem)) {
                    if(newUsageQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    logger.debug("new Usage for user {} is {} for item {}", userId, newUsageQuantity, itemId);
                    List<BigDecimal> notifyPercentages = findNotificationPercentages(getPercentages(), customerUsagePoolDTO.getInitialQuantity(),
                            oldUsageQuanity, newUsageQuantity);
                    for(BigDecimal percentage : notifyPercentages) {
                        //TODO call spc external notification api
                    }
                    if(newUsageQuantity.compareTo(customerUsagePoolDTO.getInitialQuantity()) > 0) {
                        newUsageQuantity = newUsageQuantity.subtract(customerUsagePoolDTO.getInitialQuantity());
                    }
                }
            }
        } catch(Exception ex) {
            logger.error("Error", ex);
            throw new PluggableTaskException("Error in OptusMurNotificationTask", ex);
        }
    }

    private List<BigDecimal> findNotificationPercentages(List<BigDecimal> notificationPercentages,
            BigDecimal freeQuantity, BigDecimal oldUsageQuantity, BigDecimal newUsageQuantity) {
        List<BigDecimal> applicablePercentages = new ArrayList<>();
        BigDecimal oldUsagePercentage = (oldUsageQuantity.multiply(new BigDecimal("100"))).divide(freeQuantity);
        BigDecimal newUsagePercentage = (newUsageQuantity.multiply(new BigDecimal("100"))).divide(freeQuantity);
        for (BigDecimal percentage : notificationPercentages) {
            if (oldUsagePercentage.compareTo(percentage) >= 0) {
                continue;
            }

            if (newUsagePercentage.compareTo(percentage) >= 0) {
                applicablePercentages.add(percentage);
            }
        }
        return applicablePercentages;
    }

    private List<BigDecimal> getPercentages() {
        List<BigDecimal> percentages = new ArrayList<>();
        for(Entry<String, String> paramEntry : getParameters().entrySet()) {
            String percentage = paramEntry.getKey();
            if(!NumberUtils.isNumber(percentage)) {
                logger.error("{} is not number", percentage);
                throw new IllegalArgumentException("Please Enter valid percentage number");
            }
            percentages.add(new BigDecimal(percentage));
        }
        return percentages;
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

}
