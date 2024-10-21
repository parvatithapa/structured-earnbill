package com.sapienter.jbilling.server.order;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.IJMRPostProcessor;
import com.sapienter.jbilling.server.mediation.JMRQuantity;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.pluggableTask.UndoMediationFilterTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * Created by marcolin on 13/10/15.
 */
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS;
    private IWebServicesSessionBean webServicesSessionBean;
    @Resource(name = "jBillingMediationJdbcTemplate")
    private JdbcTemplate mediationJdbcTemplate;
    @Autowired
    private JMRRepository jmrRepository;

    public void setWebServicesSessionBean(IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
    }

    @Override
    public OrderWS getCurrentOrder(Integer userId, Date date) {
        return webServicesSessionBean.getCurrentOrder(userId, date);
    }

    @Override
    public OrderWS getOrder(Integer userId, Integer orderId) {
        UserBL userBL = new UserBL(userId);
        OrderDAS das = new OrderDAS();
        OrderDTO order = das.findNow(orderId);
        if (order == null) { // not found
            return null;
        }
        OrderBL bl = new OrderBL(order);
        if (order.getDeleted() == 1) {
            logger.debug("Returning deleted order {}", orderId);
        }
        return bl.getWS(userBL.getLanguage());
    }

    @Override
    public List<OrderWS> lastOrders(Integer userId, int numberOfOrdersToRetrieve) {
        return Arrays.asList(webServicesSessionBean.getLastOrders(userId, numberOfOrdersToRetrieve))
                .stream().map(i -> webServicesSessionBean.getOrder(i)).collect(Collectors.toList());
    }

    @Override
    public List<OrderChangeStatusWS> getOrderChangeStatusesForCompany() {
        return Arrays.asList(webServicesSessionBean.getOrderChangeStatusesForCompany());
    }

    @Override
    public List<OrderChangeTypeWS> getOrderChangeTypesForCompany() {
        return Arrays.asList(webServicesSessionBean.getOrderChangeTypesForCompany());
    }

    @Resource
    private SessionFactory sessionFactory;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public MediationEventResultList addMediationEventList(List<JbillingMediationRecord> jmrList) {
        MediationEventResultList resultList = new MediationEventResultList();
        int count = 0;
        for (JbillingMediationRecord jmr : jmrList) {
            MediationEventResult result = addMediationEvent(jmr);
            if (null == result || result.hasException()) {
                logger.info("Error JMR Key: {}", jmr.getRecordKey());
                resultList.setRolledBack(true);
                resultList.clear();
                // return with flag `rolledBack` set
                return resultList;
            }
            // flush and clear session after processing 10 jmrs.
            if(++count % 10 == 0) {
                Session session = sessionFactory.getCurrentSession();
                session.flush();
                session.clear();
                logger.debug("session flush and cleared for user {}", jmr.getUserId());
            }
            resultList.addResult(jmr, result);
        }
        return resultList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jbillingMediationTransactionManager")
    public boolean isJMRProcessed(JbillingMediationRecord jmr) {
        if(jmrRepository.isJMRProcessed(jmr.getRecordKey())) {
            logger.debug("jmr {} alreday processed", jmr.getRecordKey());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public MediationEventResult addMediationEvent(JbillingMediationRecord jmr) {
        long startTime = System.currentTimeMillis();
        MediationEventResult mediationEventResult = new MediationEventResult();
        try {
            String pricingFields = jmr.getPricingFields();
            PricingField[] fieldsArray = parsePricingFields(pricingFields);

            JMRQuantity resolvedJmrQty = resolveQuantity(jmr, fieldsArray);
            if (!JMRQuantity.NONE.equals(resolvedJmrQty)) {
                if (!resolvedJmrQty.hasErrors()) {
                    jmr.setQuantity(resolvedJmrQty.getQuantity());
                } else {
                    mediationEventResult.setQuantityResolutionSuccess(false);
                    mediationEventResult.setErrorCodes(resolvedJmrQty.getErrors());
                    return mediationEventResult;
                }
            }

            Integer userId = jmr.getUserId();
            Integer itemId = jmr.getItemId();

            OrderDTO order = null;
            //normal processing
            UserBL userbl = new UserBL(userId);

            //NOTE: should be the same as ownerEntityId at this point
            Integer companyId = userbl.getEntityId(userId);

            // get currency from the user
            Integer currencyId = userbl.getCurrencyId();

            // get language from the caller
            Integer languageId = userbl.getLanguage();

            Date eventDate = getTimeZonedEventDate(jmr.getEventDate(), companyId);

            // convert the JMR to record. Important thing
            // here are the pricing fields, they weill be
            // subsequently used in ItemBL to pricing
            List<CallDataRecord> records = null;

            CallDataRecord callDataRecord = convertJMRtoRecord(jmr);
            if(null != callDataRecord){
                records = new ArrayList<>(1);
                records.add(callDataRecord);
            }

            // get the current order and init OrderBL
            order = getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, true, jmr.getProcessId().toString(), companyId, fieldsArray);
            mediationEventResult.setCurrentOrderId(order.getId());
            List<OrderLineDTO> oldLines = copyOldOrderLinesAndUpdateJMROnLine(order, jmr);
            OrderBL bl = new OrderBL(order);
            UserDTO reseller = new CompanyDAS().find(companyId).getReseller();
            BigDecimal costAmount = BigDecimal.ZERO;
            if (reseller != null) {
                processLines(
                        bl.getDTO(), languageId, reseller.getEntity().getId(),
                        reseller.getId(), reseller.getCurrency().getId(), pricingFields, itemId);

                List<OrderLineDTO> diffLines =
                        OrderLineBL.diffOrderLines(oldLines, bl.getDTO().getLines());
                for (int index = 0; index < diffLines.size(); index++) {
                    OrderLineDTO line = diffLines.get(index);
                    BigDecimal costAmountForMediationEvent = line.getPrice().multiply(line.getQuantity());
                    costAmount = costAmount.add(costAmountForMediationEvent);
                    mediationEventResult.setCostOrderLineId(line.getId());
                    mediationEventResult.setCostAmountForChange(costAmountForMediationEvent);
                }
            }

            // process lines to update prices
            // and details from the source items
            processLines(
                    bl.getDTO(), languageId, companyId,
                    userId, currencyId, pricingFields, itemId);
            /**
             * Recalculate amount for mediated lines after processing lines.
             */
            if (null != order) {
                for (OrderLineDTO line : getLinesFromOrder(order, jmr)) {
                    if (line.hasOrderLineUsagePools() && null!= line.getPrice() && null!= line.getQuantity()) {
                        line.setAmount(line.getPrice().multiply(line.getQuantity()));
                    }
                    calculateOrderLinePrice(line);
                }
            }

            List<OrderLineDTO> diffLines = calculateDifflines(oldLines, bl.getDTO(), jmr);

            //once we have inserted the mediation_records and
            //we have their row keys now we insert rows in JMR table
            BigDecimal amountForEvent = BigDecimal.ZERO;
            for (int index = 0; index < diffLines.size(); index++) {
                BigDecimal amount = diffLines.get(index).getAmount();
                amountForEvent = amountForEvent.add(amount);
            }
            mediationEventResult.setAmountForChange(amountForEvent);
            logger.debug("diffLines = {}", diffLines);
            logger.debug("difflines size {} for jmr {}", diffLines.size(), jmr.getRecordKey());
            if (!diffLines.isEmpty()) {
                //do processing for billable record
                mediationEventResult.setOrderLinedId(diffLines.get(0).getId());
                mediationEventResult.setQuantityEvaluated(diffLines.get(0).getQuantity());
                // fire jmr post processing task.
                fireJmrPostProcessorTask(jmr, order, mediationEventResult);
                // generate NewQuantityEvents
                fireOrderLineQuantitiesEvent(oldLines, bl.getDTO(), jmr);
            } else {
                /**
                 * The QuantityEvaluated attribute is used as a flag in order to differentiate
                 * between 'Billable' & 'Not Billable' in the JMR processor writer step. Here,
                 * the non zero evaluated quantity indicates Billable and zero means Not Billable.
                 */
                mediationEventResult.setQuantityEvaluated(BigDecimal.ZERO);
            }
            logger.debug("MediationEventResult = {}", mediationEventResult);
            return mediationEventResult;
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("Exception while creating order", ex);
            mediationEventResult.setExceptionMessage(ex.getMessage());
            return mediationEventResult;
        } finally {
            logger.debug("{} milliseconds taken to mediate one jmr for user {}",
                    (System.currentTimeMillis() - startTime), jmr.getUserId());
        }
    }

    protected void fireOrderLineQuantitiesEvent(List<OrderLineDTO> oldLines, OrderDTO order, JbillingMediationRecord jmr) {
        OrderBL bl = new OrderBL(order);
        UserBL userBL = new UserBL(jmr.getUserId());
        Integer entityId = userBL.getEntityId(jmr.getUserId());
        bl.checkOrderLineQuantities(oldLines, bl.getDTO().getLines(), entityId, bl.getDTO().getId(), true, false);
    }

    /**
     * fires Jmr post processing task.
     * @param jmr
     * @param oldLines
     * @param diffLines
     * @param order
     * @param mediationEventResult
     * @throws PluggableTaskException
     */
    public void fireJmrPostProcessorTask(JbillingMediationRecord jmr,
            OrderDTO order, MediationEventResult mediationEventResult) throws PluggableTaskException {
        PluggableTaskManager<IJMRPostProcessor> taskManager = new PluggableTaskManager<>(jmr.getjBillingCompanyId(),
                pluggableTaskTypeCategoryDAS.findByInterfaceName(IJMRPostProcessor.class.getName()).getId());
        IJMRPostProcessor task = taskManager.getNextClass();
        while(null!= task) {
            task.afterProcessing(jmr, order, mediationEventResult);
            task = taskManager.getNextClass(); // fetch next task.
        }
    }

    private PricingField[] parsePricingFields(String pricingFields) {
        return PricingField.getPricingFieldsValue(pricingFields);
    }

    private List<CallDataRecord> getCallDataRecords(PricingField[] fieldsArray) {
        List<CallDataRecord> records = null;
        if (fieldsArray != null) {
            CallDataRecord record = new CallDataRecord();
            for (PricingField field : fieldsArray) {
                record.addField(field, false); // don't care about isKey
            }
            records = new ArrayList<>(1);
            records.add(record);
        }
        return records;
    }

    // convert the JMR to record. Important thing
    // here are the pricing fields, they weill be
    // subsequently used in ItemBL to pricing
    protected List<CallDataRecord> getCallDataRecords(String pricingFields) {
        PricingField[] fieldsArray = parsePricingFields(pricingFields);
        return getCallDataRecords(fieldsArray);
    }

    protected void processLines(OrderDTO order, Integer languageId,
            Integer entityId, Integer userId, Integer currencyId,
            String pricingFields, Integer itemId) {
        logger.debug("Processing order lines for item {}", itemId);
        new OrderBL(order).processLines(order, languageId,
                entityId, userId, currencyId, pricingFields);
    }

    /**
     * Copy old orderlines and update jmr on order line.
     * @param order
     * @param jmr
     * @return
     */
    protected List<OrderLineDTO> copyOldOrderLinesAndUpdateJMROnLine(OrderDTO order, JbillingMediationRecord jmr) {
        OrderBL bl = new OrderBL(order);
        UserBL userBL = new UserBL(jmr.getUserId());
        List<OrderLineDTO> oldLines = OrderHelper.copyOrderLinesToDto(bl.getDTO().getLines());
        Integer itemId = jmr.getItemId();
        Integer entityId = userBL.getEntityId(jmr.getUserId());
        BigDecimal quantity = jmr.getQuantity();
        // add the line to the current order
        bl.addItem(itemId, quantity, userBL.getLanguage(), jmr.getUserId(), entityId,
                userBL.getCurrencyId(), getCallDataRecords(jmr.getPricingFields()), true, jmr.getEventDate());

        // set isMediated flag true if line pass from mediation.
        for (OrderLineDTO orderLine : bl.getDTO().getLines()) {
            orderLine.setMediated(true);
        }
        if (null != order) {
            for (OrderLineDTO line : order.getLines()) {
                if (itemId.equals(line.getItemId())) {
                    line.setMediatedQuantity(quantity);
                }
            }
        }
        return oldLines;
    }


    protected List<OrderLineDTO> calculateDifflines(List<OrderLineDTO> oldLines, OrderDTO order, JbillingMediationRecord jmr) {
        logger.debug("Calculating diff lines for jmr {}", jmr.getRecordKey());
        return  OrderLineBL.diffOrderLines(oldLines, order.getLines());
    }

    protected Date getTimeZonedEventDate(Date eventDate, Integer entityId) {
        return eventDate;
    }

    protected OrderDTO getOrCreateCurrentOrder(Integer userId, Date eventDate, Integer itemId,
            Integer currencyId, boolean persist, String processId, Integer entityId, PricingField[] pricingFields){
        return OrderBL.getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, persist, processId, entityId);
    }

    @Override
    public void undoMediation(UUID processId) {
        MediationProcessService mediationProcessService = Context.getBean("mediationProcessService");
        MediationService service = Context.getBean(MediationService.BEAN_NAME);
        OrderDAS orderDAS = new OrderDAS();
        OrderLineDAS orderLineDAS = new OrderLineDAS();
        MediationProcess process = mediationProcessService.getMediationProcess(processId);
        int entityId = process.getEntityId();
        List<Integer> ordersToRemove = getOrdersToRemove(processId, entityId);

        List<JbillingMediationRecord> recordsToRemove = new ArrayList<>();
        IJMRPostProcessor jmrPostProcessor = loadJMRPostProcessorForEntity(entityId);
        for (Integer orderId : ordersToRemove) {
            OrderBL orderBL = new OrderBL(orderId);
            if (orderBL.getEntity() != null && orderBL.getEntity().getDeleted() == 0) {
                //preserve old line to be able to compare with new lines
                List<OrderLineDTO> oldLines = OrderHelper.copyOrderLinesToDto(orderBL.getDTO().getLines());

                List<JbillingMediationRecord> orderRecordLines = service.getMediationRecordsForProcessAndOrder(processId, orderId);
                if (null != orderRecordLines) {
                    for (JbillingMediationRecord recordLine : orderRecordLines) {
                        if(recordLine.getOrderLineId()!=null) {
                            OrderLineDTO orderLine = orderBL.getDTO().getLineById(recordLine.getOrderLineId());
                            orderLine.setQuantity(orderLine.getQuantity().subtract(recordLine.getQuantity()));
                            orderLine.setAmount(orderLine.getAmount().subtract(recordLine.getRatedPrice()));
                            orderLine.setCallCounter(orderLine.getCallCounter()-1);
                            orderLine.setTotalReadOnly(true);
                            if(null!= jmrPostProcessor) {
                                jmrPostProcessor.afterProcessingUndo(recordLine, orderLine);
                            }
                            new OrderLineDAS().save(orderLine);
                        }
                        recordsToRemove.add(recordLine);
                    }
                }

                // Clearing OrderLineUsagePools Association
                for (OrderLineDTO line : orderBL.getDTO().getLines()) {
                    if (line.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        line.getOrderLineUsagePools().clear();
                    }
                }

                //recalculates the price
                OrderDTO order = orderBL.getDTO();
                Integer userId = order.getUserId();
                Integer languageId = order.getUser().getLanguage().getId();
                Integer currencyId = order.getUser().getCurrencyId();
                orderBL.processLines(order, languageId, entityId, userId, currencyId, null);

                // calculate price for RouteRateCard Products
                recalculatePriceForRouteRateCardProduct(orderBL, orderRecordLines);

                // total amount, taxes ... based on new prices
                orderBL.recalculate(entityId);

                //fire new quantity events
                orderBL.checkOrderLineQuantities(
                        oldLines, orderBL.getDTO().getLines(),
                        entityId, orderId, true, false);

                OrderHelper.synchronizeOrderLines(orderBL.getDTO());

                //remove OrderLine with Zero quantity
                for (OrderLineDTO line : orderBL.getDTO().getLines()) {
                    if (line.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        orderBL.getDTO().removeLineById(line.getId());
                        orderLineDAS.delete(line);
                    }
                }

                OrderHelper.desynchronizeOrderLines(orderBL.getDTO());

                //once we update all the order lines from the affected order
                //check if this order still has any lines left
                List<OrderLineDTO> lines = orderBL.getDTO().getLines();
                if (null == lines || lines.isEmpty()) {
                    orderDAS.delete(orderBL.getDTO());
                } else {
                    orderDAS.save(orderBL.getDTO());
                }
            }
        }

        //Delete The mediation process Data
        service.deleteErrorMediationRecords(processId);
        service.deleteDuplicateMediationRecords(processId);
        service.deleteMediationRecords(recordsToRemove);
        mediationProcessService.deleteMediationProcess(processId);
    }

    private IJMRPostProcessor loadJMRPostProcessorForEntity(Integer entityId) {
        try {
            PluggableTaskManager<IJMRPostProcessor> taskManager = new PluggableTaskManager<>(entityId,
                    pluggableTaskTypeCategoryDAS.findByInterfaceName(IJMRPostProcessor.class.getName()).getId());
            return taskManager.getNextClass();
        } catch(PluggableTaskException ex) {
            throw new SessionInternalError("JMRPostProcessor plugin loading failed", ex);
        }

    }
    private void recalculatePriceForRouteRateCardProduct(OrderBL orderBL, List<JbillingMediationRecord> orderRecordLines) {
        if (null != orderRecordLines) {
            for (JbillingMediationRecord recordLine : orderRecordLines) {
                if (recordLine.getOrderLineId() != null) {
                    OrderLineDTO orderLine = orderBL.getDTO().getLineById(recordLine.getOrderLineId());
                    calculateOrderLinePrice(orderLine);
                }
            }
        }
    }

    private void calculateOrderLinePrice(OrderLineDTO orderLine) {
        if(BigDecimal.ZERO.compareTo(orderLine.getQuantity()) != 0) {
            orderLine.setPrice(orderLine.getAmount().divide(orderLine.getQuantity(),
                    Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
        }
    }

    private List<Integer> getOrdersToRemove(UUID processId, Integer entityId) {
        MediationService service = Context.getBean(MediationService.BEAN_NAME);
        try {
            PluggableTaskManager<UndoMediationFilterTask> taskManager = new PluggableTaskManager<>(entityId,Constants.PLUGGABLE_TASK_UNDO_MEDIATION_TASK_FILTER);
            UndoMediationFilterTask task = taskManager.getNextClass();
            if(task!=null) {
                return task.getOrderIdsEligibleForUndoMediation(processId);
            } else{
                return service.getOrdersForMediationProcess(processId);
            }
        } catch (Exception e) {
            throw new SessionInternalError("UndoMediationFilterTask error", OrderServiceImpl.class, e);
        }
    }

    /**
     * Converts a JMR object into a Record object which represents a CDR.
     *
     * @param jmr
     * @return Record object for the JMR object
     */
    public static CallDataRecord convertJMRtoRecord(JbillingMediationRecord jmr) {
        CallDataRecord record = null;
        PricingField[] fieldsArray = PricingField.getPricingFieldsValue(jmr.getPricingFields());
        if (fieldsArray != null) {
            record = new CallDataRecord();
            for (PricingField field : fieldsArray) {
                record.addField(field, false); // don't care about isKey
            }
        }
        return record;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public MediationEventResult addMediationEventDistributel(JbillingMediationRecord jmr) {
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public void updateOrderStatus(Integer entity, Integer userId, Integer orderId, OrderStatusFlag orderStatusFlag) {
        UserBL userBL = new UserBL(userId);
        OrderDAS das = new OrderDAS();
        OrderDTO order = das.findNow(orderId);

        OrderBL orderBL = new OrderBL(order);
        orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(orderStatusFlag, order.getUser().getCompany().getId()));
        das.save(orderBL.getDTO());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public void updateCustomOrderStatus(Integer entity, Integer userId, Integer orderId, int orderStatus) {
        UserBL userBL = new UserBL(userId);
        OrderDAS das = new OrderDAS();
        OrderDTO order = das.findNow(orderId);

        OrderBL orderBL = new OrderBL(order);
        orderBL.setStatus(null, orderStatus);
        das.save(orderBL.getDTO());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public void updateOrderMetafield(Integer entity, Integer orderId, String metafieldName, Object value) {
        OrderDAS das = new OrderDAS();
        OrderDTO order = das.findNow(orderId);
        MetaFieldHelper.setMetaField(entity, order, metafieldName, value);
    }

    /**
     * Returns lines from order based on {@link JbillingMediationRecord}.
     * @param order
     * @param jmr
     * @return
     */
    protected List<OrderLineDTO> getLinesFromOrder(OrderDTO order, JbillingMediationRecord jmr) {
        return order.getLines();
    }

}
