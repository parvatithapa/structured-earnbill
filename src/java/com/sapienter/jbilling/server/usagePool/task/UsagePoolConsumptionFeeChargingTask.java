/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool.task;

import grails.plugin.springsecurity.SpringSecurityService;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CollectionUtil;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionFeeChargingEvent;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * UsagePoolConsumptionFeeChargingTask
 * This is an event handler that listens to occurrence of UsagePoolConsumptionFeeChargingEvent.
 * When this event occurs, it picks up the fee product and creates a new one-time post paid 
 * order for this fee product. This fee is thus charged to the customer through one-time order.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

public class UsagePoolConsumptionFeeChargingTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private static final ParameterDescription PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME =
            new ParameterDescription("Subscription order id meta field name", true, ParameterDescription.Type.STR);

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        UsagePoolConsumptionFeeChargingEvent.class
    };

    public Class<Event>[] getSubscribedEvents() { return events; }
    
    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("processing UsagePoolConsumptionFeeChargingTask");
        if (!(event instanceof UsagePoolConsumptionFeeChargingEvent))
                throw new PluggableTaskException("Cannot process event " + event);

        UsagePoolConsumptionFeeChargingEvent feeChargingEvent = (UsagePoolConsumptionFeeChargingEvent) event;
        Integer customerUsagePoolId = feeChargingEvent.getCustomerUsagePoolId();

        logger.debug("Processing event: customerUsagePoolId {}", customerUsagePoolId);
        CustomerUsagePoolDTO customerUsagePool = new CustomerUsagePoolBL(customerUsagePoolId).getEntity();
        Integer itemId = feeChargingEvent.getAction().getProductId();
        UserDTO user = customerUsagePool.getCustomer().getBaseUser();
        Date activeSince = feeChargingEvent.getActiveSince();
        SpringSecurityService springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        Runnable createOrder = () -> {
            try {
                createOrder(user, itemId, customerUsagePool, activeSince);
            } catch(PluggableTaskException ex) {
                throw new SessionInternalError("error creating order!", ex.getCause());
            }
        };
        // check user LoggedIn or not.
        if(!springSecurityService.isLoggedIn()) {
            // need to login when task executed by mediation process.
            try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())) {
                createOrder.run();
            }
        } else {
            createOrder.run();
        }
    }

    private Integer createOrder(UserDTO user, Integer itemId, CustomerUsagePoolDTO customerUsagePool, Date activeSince) throws PluggableTaskException {
        logger.debug("creating fee charging order");
        IWebServicesSessionBean api = Context.getBean("webServicesSession");
        Integer entityId = getEntityId();
        // create the order
        OrderWS orderWS = new OrderWS();
        orderWS.setUserId(user.getId());
        Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();
        orderWS.setActiveSince(getActiveSince(activeSince, nextInvoiceDate));
        orderWS.setPeriod(Constants.ORDER_PERIOD_ONCE);
        orderWS.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        orderWS.setCreateDate(Calendar.getInstance().getTime());
        orderWS.setCurrencyId(user.getCurrency().getId());
        OrderLineWS line = new OrderLineWS();
        ItemBL itemBL;
        if (itemId != null) {
            try {
                itemBL = new ItemBL(itemId);
            } catch (SessionInternalError e) {
                throw new PluggableTaskException("Cannot find configured fee charges item: " + itemId, e);
            }
        } else {
            throw new PluggableTaskException("No product id configured for fee charge, for consumption action id:");
        }

        String description = itemBL.getEntity().getDescription(user.getLanguageIdField());
        line.setItemId(itemId);
        line.setQuantity(BigDecimal.ONE);
        line.setUseItem(Boolean.FALSE);
        line.setTypeId(itemBL.getOrderLineTypeId());
        BigDecimal price = itemBL.getPrice(user.getId(), BigDecimal.ONE, entityId);
        line.setPrice(price);
        line.setAmount(price);

        setOrderLineServiceId(line, customerUsagePool);

        line.setDescription(description);
        String parameter = getParameters().get(PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME.getName());
        if(parameter!=null) {
        	List<MetaField> metaFieldList =  new MetaFieldDAS().getAvailableMetaFields(entityId, EntityType.ORDER, parameter, Boolean.TRUE);
        	if(CollectionUtils.isNotEmpty(metaFieldList)) {
        		OrderDTO planOrder=customerUsagePool.getOrder();
        		MetaFieldValueWS orderIdMetaField = new MetaFieldValueWS(parameter,
        				null, DataType.INTEGER, false, planOrder.getId());
        		orderWS.setMetaFields(new MetaFieldValueWS[] { orderIdMetaField });
        	}
        }
        orderWS.setOrderLines(new OrderLineWS[] { line });
        OrderChangeWS orderChanges [] = OrderChangeBL.buildFromOrder(orderWS,
                new OrderChangeStatusDAS().findApplyStatus(entityId).getId());
        for(OrderChangeWS orderChange : orderChanges) {
            Date startDate = orderWS.getActiveSince();
            orderChange.setStartDate(startDate);
            orderChange.setApplicationDate(startDate);
            orderChange.setAppliedManually(1);
        }
        return api.createUpdateOrder(orderWS, orderChanges);
    }

    protected void setOrderLineServiceId(OrderLineWS line, CustomerUsagePoolDTO customerUsagePool) {
        logger.debug("Not setting service id at order line level in UsagePoolConsumptionFeeChargingTask");
    }
    
    protected Date getActiveSince(Date activeSince, Date nextInvoiceDate) {
    	return null != activeSince && activeSince.before(nextInvoiceDate)
                ? DateUtils.addDays(nextInvoiceDate, -1) : companyCurrentDate();
    }
}
