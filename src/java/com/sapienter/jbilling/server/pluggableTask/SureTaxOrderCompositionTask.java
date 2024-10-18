package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.client.suretax.SureTaxBL;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.response.IResponseHeader;
import com.sapienter.jbilling.client.suretax.responsev1.Group;
import com.sapienter.jbilling.client.suretax.responsev1.SuretaxResponseV1;
import com.sapienter.jbilling.client.suretax.responsev1.TaxItem;
import com.sapienter.jbilling.client.suretax.responsev2.SuretaxResponseV2;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.process.task.AbstractSureTaxRequestTask;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by Fernando G. Morales on 3/2/15.
 * # 11776
 *
 */
public class SureTaxOrderCompositionTask extends AbstractSureTaxRequestTask implements OrderProcessingTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SureTaxOrderCompositionTask.class));
    private OrderDTO order = null;
    private Map<String, OrderLineDTO> lineNrOrderLineMap = new HashMap<>();

    public SureTaxOrderCompositionTask() {
    }

    @Override
    public void doProcessing(OrderDTO inOrder) throws TaskException {
        order = inOrder;

        extractPluginParameters(order.getUserId());

        SureTaxBL sureTaxBL = new SureTaxBL(getParameter(AbstractSureTaxRequestTask.SURETAX_REQUEST_URL, ""));
        SuretaxRequest suretaxRequest = null;

        boolean errorOccurred = false;
        try {
            suretaxRequest = buildAssembledRequest(order, order.getUserId());
        } catch (Exception e) {
            if (rollbackInvoiceOnSuretaxError) {
                throw new TaskException(e);
            } else {
                errorOccurred = true;
            }
        }

        if(errorOccurred) {
            return;
        }

        performRequest(suretaxRequest, sureTaxBL);
    }

    /**
     * Creates a Suretax request from inputs from the invoice.
     *
     * @param order
     *            Order for which tax lines need to be calculated.
     * @return Returns instance of
     *         com.sapienter.jbilling.client.suretax.request.SuretaxRequest
     * @throws TaskException
     */
    private SuretaxRequest buildAssembledRequest(OrderDTO order, Integer userId) throws TaskException {
        String uniqueTrackingCode = SureTaxBL.nextTransactionId();
        SuretaxRequest suretaxRequest = buildBaseSuretaxRequest();

        transactionDate = dateFormatter.format(LocalDateTime.ofInstant(order.getActiveSince().toInstant(), ZoneId.systemDefault()));

        /* Q – Quote purposes – taxes are computed and returned in the response
               message for generating quotes. No detailed tax information is saved in
               the SureTax tables for reporting.
         */
        suretaxRequest.setReturnFileCode("Q");
        BigDecimal totalRevenue = order.getTotal();

        int idx=0;
        List<LineItem> itemList = new ArrayList<>();
        for (OrderLineDTO orderLine : order.getLines()) {
            if(orderLine.isTaxQuote()) {
                orderLine.setDeleted(1);
            } else if(orderLine.isDiscount()) {
                //if it is a global discount
                if(orderLine.getItem() == null) {
                    idx = createLineItemsForGlobalDiscount(order.getParentOrder(), orderLine, idx, itemList);
                } else {  //if it is a discount on a product
                    LineItem lineItem = buildLineItem(orderLine.getItem().getId(), orderLine, uniqueTrackingCode, Integer.toString(idx++), null, null);
                    itemList.add(lineItem);
                }
            } if (orderLine.getItemId() != null) {
                LOG.debug("Populating itemlist for order line: %s", orderLine);

                LineItem lineItem = buildLineItem(orderLine.getItem().getId(), orderLine, uniqueTrackingCode, Integer.toString(idx++), null, null);
                itemList.add(lineItem);
            }
        }

/*        BigDecimal discountOnTotal = BigDecimal.ZERO;
        //order level discounts must be applied to all lines
        for(OrderDTO childOrder: order.getChildOrders()) {
            if(childOrder.isDiscountOrder()) {
                for (OrderLineDTO orderLine : order.getLines()) {
                    //if it is a order level discount
                    if(orderLine.isDiscount() && orderLine.getParentLine() == null) {
                        discountOnTotal = discountOnTotal.add(orderLine.getAmount());
                    }
                }
            }
        }

        if(!discountOnTotal.equals(BigDecimal.ZERO)) {
            subtractOrderLevelDiscountFromLines(itemList, totalRevenue, discountOnTotal);
        }
        suretaxRequest.setTotalRevenue(totalRevenue.add(discountOnTotal).floatValue()); */

        suretaxRequest.setItemList(itemList);
        return suretaxRequest;
    }


    protected int createLineItemsForGlobalDiscount(OrderDTO order, OrderLineDTO discountLine, int startIdx, List<LineItem> lineItems) throws TaskException {
        BigDecimal orderTotal = BigDecimal.ZERO;
        List<OrderLineDTO> linesToGetDiscount = new ArrayList<>();
        for(OrderLineDTO line : order.getLines()) {
            if(line.isLineTypeItem() || line.isPenalty() || line.isSubscription()) {
                orderTotal = orderTotal.add(line.getAmount());
                linesToGetDiscount.add(line);
            }
        }

        BigDecimal discount = discountLine.getAmount().abs();
        BigDecimal discountFraction = discount.divide(orderTotal, 2, RoundingMode.HALF_UP);
        for(OrderLineDTO line : linesToGetDiscount) {
            BigDecimal appliedDiscount = discount.min(line.getAmount().multiply(discountFraction));
            discount = discount.subtract(appliedDiscount);

            String lineNr = Integer.toString(startIdx++);
            LineItem lineItem = buildLineItem(line.getItem().getId(), line, uniqueTrackingCode, lineNr, Integer.valueOf(1), -1 * Float.valueOf(appliedDiscount.floatValue()));
            lineNrOrderLineMap.put(lineNr, discountLine);
            lineItems.add(lineItem);
        }

        return startIdx;
    }

    protected void handleResponse(SuretaxRequest request, IResponseHeader response) {

        List<OrderLineDTO> taxLines = null;
        if(calculateLineItemTaxes) {
            taxLines = computeLineItemTaxLines(request, (SuretaxResponseV2) response);
        } else {
            taxLines = computeAggregateTaxLines((SuretaxResponseV1)response);
        }

        BigDecimal orderTotal = order.getTotal();
        for (OrderLineDTO taxLine : taxLines) {
            order.getLines().add(taxLine);
            orderTotal = orderTotal.add(taxLine.getAmount());
        }
        order.setTotal(orderTotal);

        // Add the trans id in the invoice
        MetaFieldHelper.setMetaField(getEntityId(), order, AbstractSureTaxRequestTask.SURETAX_TRANS_ID_META_FIELD_NAME, uniqueTrackingCode);

    }

    private LineItem buildLineItem(Integer itemId, OrderLineDTO orderLine,
                                   String uniqueTrackingCode, String lineNr, Integer quantity, Float revenue)
            throws TaskException {
        ItemDTO item = new ItemDAS().find(itemId);

        extractOrderParameters(orderLine.getPurchaseOrder());

        LineItem lineItem = constructBaseLineItem(uniqueTrackingCode, item.firstPlan(), item, lineNr);

        if(quantity == null) {
            quantity = orderLine.getQuantity() != null ? orderLine
                    .getQuantity().intValue() : 0;
        }
        if(revenue == null) {
            revenue = orderLine.getAmount().floatValue();
        }
        lineItem.setUnits(quantity);
        lineItem.setRevenue(revenue);
        lineItem.setSeconds(quantity);

        lineNrOrderLineMap.put(lineNr, orderLine);
        return lineItem;
    }

    protected BigDecimal getTotalRevenue(OrderDTO order) {

        // calculate TOTAL to include result lines
        order.getTotal();
        BigDecimal orderTotal = order.getTotal();

        // Remove TAX ITEMS from Invoice to avoid calculating tax on tax
        for(OrderLineDTO orderLine : order.getLines()) {
            if(!orderLine.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX_QUOTE)) {
                orderTotal = orderTotal.subtract(orderLine.getAmount());
            }
        }

        return orderTotal;
    }

    private List<OrderLineDTO> computeLineItemTaxLines(SuretaxRequest request, SuretaxResponseV2 suretaxResponse) {
        List<OrderLineDTO> taxLines = new ArrayList<OrderLineDTO>();
        Map<String, OrderLineDTO> uniqueLinesMap = new HashMap<>();
        if (suretaxResponse.successful.equals("Y")) {
            for (com.sapienter.jbilling.client.suretax.responsev2.Group group : suretaxResponse.groupList) {
                for (com.sapienter.jbilling.client.suretax.responsev2.TaxItem taxItem : group.taxList) {
                    //in the case of a order level discount we sent multiple lines to suretax (one for each product). Now we aggregate the lines.
                    String key = lineNrOrderLineMap.get(group.getLineNumber()).hashCode() + "::" + taxItem.taxTypeCode + ":" + taxItem.taxTypeDesc;
                    OrderLineDTO taxLine = uniqueLinesMap.get(key);
                    if(taxLine != null) {
                        BigDecimal amount = taxLine.getAmount().add(new BigDecimal(taxItem.taxAmount));
                        taxLine.setAmount(amount);
                        taxLine.setPrice(amount);
                    } else {
                        OrderLineDTO orderLineDTO = new OrderLineDTO();
                        orderLineDTO.setAmount(new BigDecimal(taxItem.taxAmount));
                        orderLineDTO.setDescription(taxItem.taxTypeCode + ":"
                                + taxItem.taxTypeDesc);
                        orderLineDTO.setTypeId(Constants.ORDER_LINE_TYPE_TAX_QUOTE);
                        orderLineDTO.setPrice(new BigDecimal(taxItem.taxAmount));
                        orderLineDTO.setQuantity(1);
                        orderLineDTO.setCreateDatetime(Calendar.getInstance().getTime());
                        OrderLineDTO parentLine = lineNrOrderLineMap.get(group.getLineNumber());
                        if(parentLine != null) {
                            orderLineDTO.setParentLine(parentLine);
                            parentLine.getChildLines().add(orderLineDTO);
                        }
                        orderLineDTO.setPurchaseOrder(order);
                        orderLineDTO.setUseItem(Boolean.FALSE);
                        taxLines.add(orderLineDTO);
                        uniqueLinesMap.put(key, orderLineDTO);
                    }
                }
            }
        }
        return taxLines;
    }

    /**
     * Converts the instance of SuretaxResponseV2 object into tax lines.
     *
     * @param suretaxResponseV1
     * @return
     */
    private List<OrderLineDTO> computeAggregateTaxLines(SuretaxResponseV1 suretaxResponseV1) {
        List<OrderLineDTO> taxLines = new ArrayList<OrderLineDTO>();
        if (suretaxResponseV1.successful.equals("Y")) {
            for (Group group : suretaxResponseV1.groupList) {
                for (TaxItem taxItem : group.taxList) {
                    OrderLineDTO orderLineDTO = new OrderLineDTO();
                    orderLineDTO.setAmount(new BigDecimal(taxItem.taxAmount));
                    orderLineDTO.setDescription(taxItem.taxTypeCode + ":"
                            + taxItem.taxTypeDesc);
                    orderLineDTO.setTypeId(Constants.ORDER_LINE_TYPE_TAX_QUOTE);
                    orderLineDTO.setPrice(new BigDecimal(taxItem.taxAmount));
                    orderLineDTO.setQuantity(1);
                    orderLineDTO.setCreateDatetime(Calendar.getInstance().getTime());
                    orderLineDTO.setPurchaseOrder(order);
                    orderLineDTO.setUseItem(Boolean.FALSE);
                    taxLines.add(orderLineDTO);
                }
            }
        }
        return taxLines;
    }

}
