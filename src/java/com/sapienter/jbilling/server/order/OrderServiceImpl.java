package com.sapienter.jbilling.server.order;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.pricing.db.DataTableQueryDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.JMRQuantity;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.pluggableTask.UndoMediationFilterTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import scala.Array;
import scala.Option;
import scala.tools.cmd.Opt;

import static com.sapienter.jbilling.server.util.BetaCustomerConstants.*;


/**
 * Created by marcolin on 13/10/15.
 */
public class OrderServiceImpl implements OrderService {
    IWebServicesSessionBean webServicesSessionBean;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public MediationEventResultList addMediationEventList(List<JbillingMediationRecord> jmrList) {

        MediationEventResultList resultList = new MediationEventResultList(jmrList.size());
        for (JbillingMediationRecord jmr : jmrList) {
            MediationEventResult result = addMediationEvent(jmr);

            if (result == null || result.hasException()) {
                logger.info("Error JMR Key: {}", jmr.getRecordKey());
                resultList.setRolledBack(true);
                resultList.clear();
                // return with flag `rolledBack` set
                return resultList;
            }
            resultList.addResult(result);
        }
        return resultList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public MediationEventResult addMediationEvent(JbillingMediationRecord jmr) {
        MediationEventResult mediationEventResult = new MediationEventResult();

        Integer userId = jmr.getUserId();
        Integer itemId = jmr.getItemId();

        OrderDTO order = null;
        //normal processing
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
                    mediationEventResult.setExceptionMessage(resolvedJmrQty.getErrors());
                    return mediationEventResult;
                }
            }
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
             * Recalculate prices for mediated lines if Lines use FUP.
             */
            if (null != order) {
                for (OrderLineDTO line : order.getLines()) {
                    if (line.hasOrderLineUsagePools() && itemId.equals(line.getItemId()) &&
                            (line.getPrice() != null && line.getQuantity() != null)) {
                        line.setAmount(line.getPrice().multiply(line.getQuantity()));
                    }
                    calculateOrderLinePrice(line, eventDate);
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

            if (!diffLines.isEmpty()) {
                //do processing for billable record
                mediationEventResult.setOrderLinedId(diffLines.get(0).getId());
                // generate NewQuantityEvents
                fireOrderLineQuantitiesEvent(oldLines, bl.getDTO(), jmr);

                mediationEventResult.setQuantityEvaluated(diffLines.get(0).getQuantity());
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
        }
    }

    protected void fireOrderLineQuantitiesEvent(List<OrderLineDTO> oldLines, OrderDTO order, JbillingMediationRecord jmr) {
        OrderBL bl = new OrderBL(order);
        UserBL userBL = new UserBL(jmr.getUserId());
        Integer entityId = userBL.getEntityId(jmr.getUserId());
        bl.checkOrderLineQuantities(oldLines, bl.getDTO().getLines(), entityId, bl.getDTO().getId(), true, false);
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

    private void recalculatePriceForRouteRateCardProduct(OrderBL orderBL, List<JbillingMediationRecord> orderRecordLines) {
        if (null != orderRecordLines) {
            for (JbillingMediationRecord recordLine : orderRecordLines) {
                if (recordLine.getOrderLineId() != null) {
                    OrderLineDTO orderLine = orderBL.getDTO().getLineById(recordLine.getOrderLineId());
                    calculateOrderLinePrice(orderLine, recordLine.getEventDate());
                }
            }
        }
    }

    private void calculateOrderLinePrice(OrderLineDTO orderLine, Date eventDate) {
        if (orderLine.getUseItem() && orderLine.getItem().isRouteRateCardPricingProduct(eventDate)) {
            logger.debug("Recalculate Price for RouteRateCard Product Id : {}",orderLine.getItemId());
            if(BigDecimal.ZERO.compareTo(orderLine.getQuantity()) != 0) {
                orderLine.setPrice(orderLine.getAmount().divide(orderLine.getQuantity(),
                        Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
            }
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

    public Map<Integer, List<OrderWS>> getOrderForCustomInvoice(File file) {
        List<CustomInvoiceCsvData> dataList = readAndCreateDataObject(file);
        Map<Integer, List<OrderWS>> returnData = new HashMap<>();

        for( CustomInvoiceCsvData data : dataList ) {
            List<OrderWS> orderList = new ArrayList<>();
            Integer customerId = data.getCustomerId();
            Integer orderCount = VALUE_ZERO;
            for( OrderMap orderMap : data.getOrderMaps() ) {
                orderCount++;
                Integer entityId = webServicesSessionBean.getUserWS(customerId).getEntityId(); // this method will throw exception if user is not found, it will not return null object.
                OrderWS order = new OrderWS();
                order.setUserId(customerId);
                order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
                order.setPeriod(orderMap.getOrderPeriod());
                order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
                try {
                    String[] date = data.getInvoiceDate().split("/");
                    String invoiceDate = date[0] + "/" + date[1].replaceFirst("\\d{2}", "01") + "/" + date[2];
                    order.setActiveSince(new SimpleDateFormat("MM/dd/yyyy").parse(invoiceDate));
                } catch (ParseException parseException) {
                    logAndThrowError("An exception occurred while parsing the Date :" + parseException);
                }
                order.setNotes(orderMap.getOrderNote());
                List<MetaFieldValueWS> arrayList = new ArrayList<>();

                MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

                MetaFieldValueWS tabletypeValue = getMetaFieldValueWS(metaFieldDAS, entityId, TABLE_TYPE, orderMap.getTabType());
                arrayList.add(tabletypeValue);
                if( orderCount == 1 ) {
                    MetaFieldValueWS customInvoiceNumberValue = getMetaFieldValueWS(metaFieldDAS, entityId, CUSTOM_INVOICE_NUMBER, data.getCustomInvoiceNumber());
                    arrayList.add(customInvoiceNumberValue);

                    MetaFieldValueWS invoiceDateValue = getMetaFieldValueWS(metaFieldDAS, entityId, INVOICE_DATE, data.getInvoiceDate());
                    arrayList.add(invoiceDateValue);
                    MetaFieldValueWS invoiceDueDateValue = getMetaFieldValueWS(metaFieldDAS, entityId, INVOICE_DUE_DATE, data.getInvoiceDueDate());
                    arrayList.add(invoiceDueDateValue);

                    int i = 1;
                    for( String sacCode : data.getSacCodes() ) {
                        MetaFieldValueWS sacValue = getMetaFieldValueWS(metaFieldDAS, entityId, SAC + i++, sacCode);
                        arrayList.add(sacValue);
                    }
                }
                order.setMetaFields(arrayList.toArray(new MetaFieldValueWS[arrayList.size()]));
                OrderLineWS[] orderLines = createOrderLines(orderMap, entityId);
                order.setOrderLines(orderLines);
                orderList.add(order);
            }
            returnData.put(customerId, orderList);
        }
        return returnData;
    }

    private static MetaFieldValueWS getMetaFieldValueWS(MetaFieldDAS metaFieldDAS, Integer entityId, String fieldName, String fieldValue) {
        MetaFieldValueWS tabletypeValue = new MetaFieldValueWS();
        MetaFieldWS tableTypeMfield = MetaFieldBL.getWS(metaFieldDAS.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, fieldName));
        tabletypeValue.setMetaField(tableTypeMfield);
        tabletypeValue.setStringValue(fieldValue);
        return tabletypeValue;
    }

    private  List<CustomInvoiceCsvData> readAndCreateDataObject(File file) {
        List<CustomInvoiceCsvData> dataList = new ArrayList<>();
        CustomInvoiceCsvData currentCsvData = null;
        String[] errmsgs = new String[1];
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String csvLine;
            String previousLineType = null;
            Integer lineNumber = VALUE_ZERO;
            String currentTableType = null;

            while ((csvLine = br.readLine()) != null) {
                lineNumber++;
                String[] csvLineData = csvLine.split(",", -1);
                if( csvLineData.length > 0 ) {
                    String lineType = csvLineData[0];
                    boolean lineTypeFlag = isValidLineType(previousLineType, lineType, currentCsvData);
                    if( lineTypeFlag ) {
                        switch (lineType) {
                            case H_TYPE:
                                currentCsvData = validateAndAddHLine(csvLineData, lineNumber);
                                dataList.add(currentCsvData);
                                break;
                            case O_TYPE:
                                currentTableType = validateAndAddOLine(csvLineData, lineNumber, currentCsvData);
                                break;
                            case L_TYPE:
                                validateAndAddLLine(csvLineData, lineNumber, currentCsvData, currentTableType);
                                break;
                            default:
                                break;
                        }
                        previousLineType = lineType;
                    } else {
                        logAndThrowError("Error recognising the line format");
                    }
                }
            }
            if( !previousLineType.equals(L_TYPE) ) {
                logAndThrowError("No order Lines for respective data ");
            }
        } catch (SessionInternalError sessionInternalError) {
            throw sessionInternalError;
        } catch (Exception exception) {
            throw new SessionInternalError(errmsgs);
        }
        return dataList;
    }

    private static List<String> addSacCode(String[] csvLineData) {
        List<String> sacCode = new ArrayList<>();
        for( int i = 5; i < csvLineData.length; i++ ) {
            if( StringUtils.isNotBlank(csvLineData[i]) ) {
                sacCode.add(csvLineData[i]);
            }
        }
        if( sacCode.isEmpty() ) {
            logAndThrowError("An exception occurred for SAC code .Please review the details ");
        }
        return sacCode;
    }

    private static boolean isValidLineType(String previousLineType, String lineType, CustomInvoiceCsvData currentCsvData) {
        boolean lineTypeFlag = currentCsvData == null && lineType.equals(H_TYPE);
        if( previousLineType != null ) {
            if( previousLineType.equals(H_TYPE) ) {
                lineTypeFlag = lineType.equals(O_TYPE);
            } else if( previousLineType.equals(O_TYPE) ) {
                lineTypeFlag = lineType.equals(L_TYPE);
            } else {
                lineTypeFlag = lineType.equals(H_TYPE) || lineType.equals(L_TYPE) || lineType.equals(O_TYPE);
            }
        }
        return lineTypeFlag;
    }

    private static CustomInvoiceCsvData validateAndAddHLine(String[] csvLineData, Integer lineNumber) {
        incompleteDataError(csvLineData, lineNumber, 5);
        String customerId = csvLineData[1];
        isValidInteger(csvLineData[1], lineNumber, 1);
        String invoiceId = csvLineData[2];
        String invoiceDate = csvLineData[3];
        String invoiceDueDate = csvLineData[4];
        validateLine(new String[] { customerId, invoiceId, invoiceDate, invoiceDueDate }, lineNumber);
        return new CustomInvoiceCsvData(Integer.parseInt(customerId), invoiceId, getValidDate(invoiceDate),
                getValidDate(invoiceDueDate), addSacCode(csvLineData), new ArrayList<>());
    }

    private String validateAndAddOLine(String[] csvLineData, Integer lineNumber, CustomInvoiceCsvData currentCsvData) {

        incompleteDataError(csvLineData, lineNumber, 2);
        String currentTableType = null;
        Integer orderPeriodId = 1;
        validateLine(csvLineData, lineNumber);
        if( currentCsvData != null ) {
            String orderNote = csvLineData[1];
            String tabType = csvLineData[2];
            if(csvLineData.length == 4) {
                orderPeriodId = getUnitId(Optional.ofNullable(csvLineData[3]));
            }
            currentTableType = tabType;
            currentCsvData.addOrderMap(new OrderMap(orderNote, tabType,orderPeriodId, new HashMap<>()));
        }
        return currentTableType;
    }

    private static void validateAndAddLLine(String[] csvLineData, Integer lineNumber, CustomInvoiceCsvData currentCsvData,
                                            String currentTableType) {
        String quantity=null;
        incompleteDataError(csvLineData, lineNumber, 2);
        if( !currentCsvData.getOrderMaps().isEmpty() ) {
            Map<String, String> productTypeMap = new HashMap<>();
            productTypeMap.put(TABLE_TYPE_MONTH, "");
            productTypeMap.put(TABLE_TYPE_WEEKDAY, WD);
            productTypeMap.put(TABLE_TYPE_WEEKEND_HOLIDAY, WH);
            productTypeMap.put(TABLE_TYPE_SPEED_DELIVERY, SD);
            productTypeMap.put(TABLE_TYPE_SUBSCRIPTION, "");
            validateLine(csvLineData, lineNumber);
            String productCode = csvLineData[1] + productTypeMap.get(currentTableType);
            if( csvLineData[2].matches(REG_EX_FOR_DECIMAL) ) {
                quantity = csvLineData[2];
            }else {
                logAndThrowError("An exception occurred at Line " + lineNumber + ". Product quantity should not be character");
            }
            currentCsvData.getOrderMaps().get(currentCsvData.getOrderMaps().size() - 1).addOrderLine(productCode, quantity);
        }
    }

    private static void validateLine(String[] csvLineData, Integer lineNumber) {
        for( String data : csvLineData ) {
            if( data.trim().isEmpty() ) {
                logAndThrowError("An exception occurred at Line " + lineNumber + " .Please review the details ");
            }
        }
    }

    private OrderLineWS[] createOrderLines(OrderMap orderMap, Integer entityId) {
        ArrayList<OrderLineWS> orderLines = new ArrayList<>();
        for( Map.Entry<String, String> entry : orderMap.getOrderLineMap().entrySet() ) {
            String productCode = entry.getKey();
            String quantity = entry.getValue();

            OrderLineWS orderLine = new OrderLineWS();
            orderLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            orderLine.setQuantity(quantity);

            ItemDTO item = new ItemDAS().findItemByInternalNumber(productCode, entityId);
            if( item != null ) {
                orderLine.setDescription(item.getDescription());
                orderLine.setItemId(item.getId());
                PriceModelDTO priceModelDTO = item.getPrice(TimezoneHelper.companyCurrentDate(item.getPriceModelCompanyId()), item.getPriceModelCompanyId());
                orderLine.setPrice(priceModelDTO.getRate());
                orderLine.setAmount(priceModelDTO.getRate().multiply(new BigDecimal(quantity)));
            } else {
                logAndThrowError("No product found with product code: " + productCode);
            }
            orderLines.add(orderLine);
        }
        return orderLines.toArray(new OrderLineWS[orderLines.size()]);
    }

    private static void incompleteDataError(String[] csvLineData, Integer lineNumber, Integer lineCount) {
        if( csvLineData.length <= lineCount ) {
            logAndThrowError("Incomplete Line detail at line " + lineNumber + ". Provide complete data");
        }
    }

    private static void isValidInteger(String csvLineData, Integer lineNumber, Integer index) {
        if( !csvLineData.matches(REG_EX_FOR_INTEGER) || Integer.parseInt(csvLineData) <= 0 ) {
            logAndThrowError("An exception occurred at Line " + lineNumber + " : Provide positive number at index " + (index + 1));
        }
    }

    private static String getValidDate(String invoiceDate) {
        if( invoiceDate.split("/").length != 3 ) {
            logAndThrowError("Invalid date format : " + invoiceDate + " is not a valid date.");
        }
        DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
        try {
            invoiceDate = new SimpleDateFormat("MM/dd/yyyy").format(sdf.parse(invoiceDate));
        } catch (ParseException e) {
            logAndThrowError("Invalid date: " + invoiceDate + " is not a valid date.");
        }
        return invoiceDate;
    }

    private static void logAndThrowError(String errorMessage) {
        logger.debug(errorMessage);
        throw new SessionInternalError(new String[] { errorMessage });
    }

    private Integer getUnitId(Optional<String> orderPeriod) {
        final Map<String, Integer> PERIOD_UNIT_MAP = new HashMap<>();
            PERIOD_UNIT_MAP.put("daily", PeriodUnitDTO.DAY);
            PERIOD_UNIT_MAP.put("monthly", PeriodUnitDTO.MONTH);
            PERIOD_UNIT_MAP.put("weekly", PeriodUnitDTO.WEEK);
            PERIOD_UNIT_MAP.put("yearly", PeriodUnitDTO.YEAR);
            PERIOD_UNIT_MAP.put("quarterly", PeriodUnitDTO.YEAR);

        if (orderPeriod.isPresent()) {
            String op = orderPeriod.get().trim().toLowerCase();
            if (PERIOD_UNIT_MAP.containsKey(op)) {
                Integer unitId = PERIOD_UNIT_MAP.get(op);
                try {
                    return new OrderPeriodDAS().findOrderPeriod(webServicesSessionBean.getCallerCompanyId(), 1, unitId).getId();
                } catch (Exception e) {
                    logAndThrowError("Order period '" + orderPeriod.get() + "' is not configured in your system.");
                }
            }
        }
        /* orderPeriod id 1 is for One time order period*/
        return 1;
    }

}
