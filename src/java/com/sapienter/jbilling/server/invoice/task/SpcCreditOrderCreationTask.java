package com.sapienter.jbilling.server.invoice.task;

import com.sapienter.jbilling.server.item.AssetWS;
import grails.plugin.springsecurity.SpringSecurityService;


import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.AboutToGenerateInvoices;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.spc.SpcCreditPoolInfo;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class SpcCreditOrderCreationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_CREDIT_POOL_TABLE_NAME =
            new ParameterDescription("Credit pool table name", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME =
            new ParameterDescription("Subscription order id meta field name", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_CREDIT_POOL_TARIFF_CODES_MF_NAME =
            new ParameterDescription("Credit pools tariff code MF name", true, ParameterDescription.Type.STR);

    public static final ParameterDescription PARAM_TAX_DATE_FORMAT =
            new ParameterDescription("tax date format", false, ParameterDescription.Type.STR, "dd-MM-yyyy");
    
    public static final ParameterDescription PARAM_TAX_TABLE_NAME =
            new ParameterDescription("tax table name", false, ParameterDescription.Type.STR, "route_70_tax_scheme");

    public static final ParameterDescription PARAM_SERVICE_ID_MF_NAME =
            new ParameterDescription("Service Id meta field name", false, ParameterDescription.Type.STR, "ServiceId");

    public SpcCreditOrderCreationTask() {
        descriptions.add(PARAM_CREDIT_POOL_TABLE_NAME);
        descriptions.add(PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME);
        descriptions.add(PARAM_CREDIT_POOL_TARIFF_CODES_MF_NAME);
        descriptions.add(PARAM_TAX_DATE_FORMAT);
        descriptions.add(PARAM_TAX_TABLE_NAME);
        descriptions.add(PARAM_SERVICE_ID_MF_NAME);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        AboutToGenerateInvoices.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (!(event instanceof AboutToGenerateInvoices)) {
            throw new PluggableTaskException("Cannot process event " + event);
        }
                
        SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
        String tableName = getMandatoryStringParameter(PARAM_CREDIT_POOL_TABLE_NAME.getName());
        spcHelperService.isTablePresent(tableName);
        MetaField subscriptionOrderIdMf = spcHelperService.validateAndGetMetaField(getEntityId(),
                getMandatoryStringParameter(PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME.getName()),
                EntityType.ORDER, DataType.INTEGER);

        MetaField creditPoolTariffCodesMf = spcHelperService.validateAndGetMetaField(getEntityId(),
                getMandatoryStringParameter(PARAM_CREDIT_POOL_TARIFF_CODES_MF_NAME.getName()),
                EntityType.ORDER, DataType.STRING);

        AboutToGenerateInvoices invEvent = (AboutToGenerateInvoices) event;
        UserDTO user = new UserDAS().find(invEvent.getUserId());
        OrderDAS orderDAS = new OrderDAS();
        List<String> creditOrderCreatedForPlan = new ArrayList<>();
        for(OrderDTO mediatedOrder : orderDAS.findActiveMediatedOrdersByUserIdByDate(user.getId(), user.getCustomer().getNextInvoiceDate())) {
            for(OrderLineDTO mediatedLine : mediatedOrder.getLines()) {
                String callIdentifier = mediatedLine.getCallIdentifier();
                if(StringUtils.isEmpty(callIdentifier)) {
                    throw new PluggableTaskException("call identifer not found for order "+ mediatedOrder.getId());
                }
                long queryStartTime = System.currentTimeMillis();
                OrderDTO planOrder = orderDAS.findOrderByUserAssetIdentifierEffectiveDate(user.getId(), callIdentifier, mediatedOrder.getActiveSince());
                logger.debug(" findOrderByUserAssetIdentifierEffectiveDate query execution took {} in miliseconds", (System.currentTimeMillis() - queryStartTime));
                if(null == planOrder) {
                    logger.debug("plan order not found for asset identifier {}", callIdentifier);
                    continue;
                }
                PlanDTO plan = planOrder.getPlanFromOrder();
                if(null == plan) {
                    logger.debug("plan not found for asset identifier {}", callIdentifier);
                    continue;
                }
                // fetch all the creditPools for the plan
                queryStartTime = System.currentTimeMillis();
                List<SpcCreditPoolInfo> creditPools = spcHelperService.getCreditPoolsForPlan(plan.getId(), tableName);
                logger.debug(" getCreditPoolsForPlan query execution took {} in miliseconds", (System.currentTimeMillis() - queryStartTime));
                if(CollectionUtils.isEmpty(creditPools)) {
                    continue;
                }
                logger.debug("credit order creating for plan {}", plan.getId());
                queryStartTime = System.currentTimeMillis();
                List<String> planAssets = orderDAS.findAssetIdentifiersByUserOrderEffectiveDate(user.getId(), planOrder.getId(), mediatedOrder.getActiveSince());
                logger.debug(" findAssetIdentifiersByUserOrderEffectiveDate query execution took {} in miliseconds", (System.currentTimeMillis() - queryStartTime));
                for(SpcCreditPoolInfo creditPool : creditPools) {
                    String planIdTarrifCodeKey = plan.getId()+"-"+planOrder.getId()+"-"+creditPool.getTariffCodes();
                    if(creditOrderCreatedForPlan.contains(planIdTarrifCodeKey)) {
                        logger.debug("credit order for plan {} and tariff code {} already present", plan.getId(), creditPool.getTariffCodes());
                        continue;
                    }
                    SpringSecurityService springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
                    spcHelperService.getCreditOrderForSubscriptionOrder(planOrder.getUserId(), subscriptionOrderIdMf.getName(), 
                    		creditPoolTariffCodesMf.getName(), planOrder.getId(), creditPool.getCreditPoolName())
                            .stream()
                            .forEach(creditOrderId -> {
                                logger.debug("credit order {} already created for subscription order {} for plan {}",
                                        creditOrderId, planOrder.getId(), plan.getId());
                                OrderDTO orderDto = new OrderBL(creditOrderId).getDTO();
                                for (OrderLineDTO lineDto : orderDto.getLines()) {
                                    lineDto.setDeleted(1);
                                }
                                orderDto.setDeleted(1);
                            });
                    creditOrderCreatedForPlan.add(planIdTarrifCodeKey);
                    queryStartTime = System.currentTimeMillis();
                    BigDecimal usageAmount = spcHelperService.getUsageForAssetsAndTariffCodesFromJMR(planAssets, mediatedOrder.getId(),
                            creditPool.getTariffCodeList(),user.getId(), user.getCustomer().getNextInvoiceDate());
                    logger.debug(" getUsageForAssetsAndTariffCodesFromJMR query execution took {} in miliseconds", (System.currentTimeMillis() - queryStartTime));
                    if(usageAmount.compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    logger.debug("Usage amount for user {} is {}", user.getId() ,usageAmount);
                    Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();
                    String taxDateFormat = getParameter(PARAM_TAX_DATE_FORMAT.getName(), PARAM_TAX_DATE_FORMAT.getDefaultValue());
                    String taxTableName  = getParameter(PARAM_TAX_TABLE_NAME.getName(), PARAM_TAX_TABLE_NAME.getDefaultValue());
                    BigDecimal itemTaxRate = new ItemBL(mediatedLine.getItemId()).getTaxRate(nextInvoiceDate, taxTableName, taxDateFormat);
                    BigDecimal excludingAmountRate = itemTaxRate.divide(new BigDecimal("100")).add(BigDecimal.ONE);
                    logger.debug("excludingAmountRate is {}", excludingAmountRate);
                    BigDecimal unRoundedExcludingAmount = usageAmount.divide(excludingAmountRate, CommonConstants.BIGDECIMAL_PRECISION_DECIMAL128, RoundingMode.HALF_EVEN);
                    logger.debug("unRoundedExcludingAmount is {}", unRoundedExcludingAmount);
                    BigDecimal orderCreditAmount;
                    BigDecimal freeAmount = creditPool.getFreeAmountAsDecimal();
                    if(unRoundedExcludingAmount.compareTo(freeAmount) > 0 ) {
                        orderCreditAmount = freeAmount;
                    } else {
                        orderCreditAmount = unRoundedExcludingAmount;
                    }
                    logger.debug("creating credit usage order with amount {} for plan {} for tariff codes {}",
                            orderCreditAmount, plan.getId(), creditPool.getTariffCodes());
                    // creating usage credit order.
                    Runnable createOrder = () -> {
                        try {
                            createOrder(user, creditPool.getCreditItemIdAsInt(), plan.getId(), orderCreditAmount.negate(),
                                    invEvent.getRunDate(), planOrder.getId(), creditPool.getCreditPoolName(), callIdentifier);
                        } catch(PluggableTaskException ex) {
                            throw new SessionInternalError("error creating credit order!", ex.getCause());
                        }
                    };
                    // check user LoggedIn or not.
                    if(!springSecurityService.isLoggedIn()) {
                        // need to login when task executed by billing process.
                        try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())) {
                            createOrder.run();
                        }
                    } else {
                        createOrder.run();
                    }
                }

            }
        }

    }

    /**
     * Creates credit usage order for given amount
     * @param user
     * @param itemId
     * @param planId
     * @param amount
     * @param billingDate
     * @throws PluggableTaskException
     */
    private Integer createOrder(UserDTO user, Integer itemId, Integer planId, BigDecimal amount,
            Date billingDate, Integer subscriptionOrderId, String creditPoolName, String callIdentifier) throws PluggableTaskException {
    	
    	Calendar activeSinceDate = Calendar.getInstance();
    	activeSinceDate.setTime(user.getCustomer().getNextInvoiceDate());
    	activeSinceDate.add(Calendar.DATE, -1);
    	
    	// get the bean
        IWebServicesSessionBean api = Context.getBean("webServicesSession");
        Integer entityId = getEntityId();
        // create the order
        OrderWS orderWS = new OrderWS();
        orderWS.setUserId(user.getId());
        
        //orderWS.setActiveSince(user.getCustomer().getNextInvoiceDate());
        //JBSPC-850 : To handle the call credit issue on the Postpaid Order. This would not have impact on the Prepaid order.
        orderWS.setActiveSince(activeSinceDate.getTime());
        
        orderWS.setPeriod(Constants.ORDER_PERIOD_ONCE);
        orderWS.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        orderWS.setCreateDate(Calendar.getInstance().getTime());
        orderWS.setCurrencyId(user.getCurrency().getId());
        orderWS.setNotes("Auto generated Usage credit order for plan "+ planId);
        ItemDTOEx item = api.getItem(itemId, null, null);
        String description = item.getDescription();
        OrderLineWS line = new OrderLineWS();
        line.setItemId(itemId);
        line.setQuantity(BigDecimal.ONE);
        line.setUseItem(Boolean.FALSE);
        line.setTypeId(item.getOrderLineTypeId());
        line.setPrice(amount.setScale(CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND));
        line.setAmount(amount.setScale(CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND));
        line.setDescription(description);
        String serviceIdMFName = getParameter(PARAM_SERVICE_ID_MF_NAME.getName(), PARAM_SERVICE_ID_MF_NAME.getDefaultValue());
        String serviceId = getServiceId(api, callIdentifier, serviceIdMFName);
        if(StringUtils.isNotBlank(serviceId)) {
            MetaFieldValueWS serviceIdMetaField = new MetaFieldValueWS();
            serviceIdMetaField.setFieldName(serviceIdMFName);
            serviceIdMetaField.setDataType(DataType.STRING);
            serviceIdMetaField.setValue(serviceId);
            serviceIdMetaField.setEntityId(getEntityId());
            line.setMetaFields(new MetaFieldValueWS[] { serviceIdMetaField });
            logger.debug("service id : {} for the order line : {} has been set", serviceId, line.getId());
        }
        orderWS.setOrderLines(new OrderLineWS[] { line });
        MetaFieldValueWS orderIdMetaField = new MetaFieldValueWS(getMandatoryStringParameter(PARAM_SUBSCRIPTION_ORDER_ID_MF_NAME.getName()),
                null, DataType.INTEGER, false, subscriptionOrderId);
        MetaFieldValueWS tariffCodesMetaField = new MetaFieldValueWS(getMandatoryStringParameter(PARAM_CREDIT_POOL_TARIFF_CODES_MF_NAME.getName()),
                null, DataType.STRING, false, creditPoolName);
        orderWS.setMetaFields(new MetaFieldValueWS[] { orderIdMetaField, tariffCodesMetaField });
        Integer orderId = api.createUpdateOrder(orderWS, OrderChangeBL.buildFromOrder(orderWS,
                findApplyOrderChangeStatusForEntity(entityId).getId()));
        logger.debug("credit usage order {} created for plan {}", orderId, planId);
        return orderId;
    }

    /**
     * Helper method to fetch the service id for the provided asset identifier
     * @param api
     * @param callIdentifier
     * @param serviceIdMFName
     * @return serviceId
     */
    private String getServiceId(IWebServicesSessionBean api, String callIdentifier, String serviceIdMFName) {
        String serviceId = "";
        AssetWS asset = api.getAssetByIdentifier(callIdentifier);
        for(MetaFieldValueWS mf : asset.getMetaFields()) {
            if(mf.getFieldName().equalsIgnoreCase(serviceIdMFName) && StringUtils.isNotBlank(mf.getStringValue())) {
                serviceId = mf.getStringValue();
                break;
            }
        }
        return StringUtils.isNotBlank(serviceId) ? serviceId : callIdentifier;
    }

    private OrderChangeStatusDTO findApplyOrderChangeStatusForEntity(Integer entityId) {
        List<OrderChangeStatusDTO> statusDTOs = new OrderChangeStatusDAS().findOrderChangeStatuses(entityId);
        for(OrderChangeStatusDTO status : statusDTOs) {
            if(status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status;
            }
        }
        throw new SessionInternalError("No order Change Apply status found for entity id "+ entityId);
    }
}
