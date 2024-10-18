package com.sapienter.jbilling.server.process.task;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.sapienter.jbilling.client.suretax.SureTaxBL;
import com.sapienter.jbilling.client.suretax.response.IResponseHeader;
import com.sapienter.jbilling.client.suretax.responsev2.SuretaxResponseV2;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.client.suretax.SuretaxClient;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.responsev1.Group;
import com.sapienter.jbilling.client.suretax.response.ItemMessage;
import com.sapienter.jbilling.client.suretax.responsev1.SuretaxResponseV1;
import com.sapienter.jbilling.client.suretax.responsev1.TaxItem;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineTypeDTO;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * This plug-in gets the tax lines from Suretax Tax engine for invoice lines in
 * an invoice. Plug-in parameters: client_number: Required. Suretax issued
 * client number. validation_key: Required. Suretax issued validation key.
 * response_group: Optional. Determines how taxes are grouped for the response.
 * Values: 00 - Taxes grouped by State (Default) 01 - Taxes grouped by State +
 * Invoice Number 02 - Tax grouped by State + Customer Number 03 - Tax grouped
 * by State + Customer Number + Invoice Number response_type: Optional. Values
 * could be 'D' or 'S'. Defaults to 'D'. D - Detailed. Tax values are returned
 * by tax type for all levels of tax (Federal, State, and Local). S - Summary.
 * Tax values are returned summarized by Federal, State and Local Taxes.
 * number_of_decimals: Optional. Number of decimals in the tax lines. Defaults
 * to 2 rollback_invoice_on_error: Optional. Whether to rollback the invoice
 * creation if an error occurs during getting of tax line from Suretax.
 */
public class SureTaxCompositionTask extends AbstractSureTaxRequestTask implements
        InvoiceCompositionTask {
    private static final FormatLogger LOG = new FormatLogger(
            Logger.getLogger(SureTaxCompositionTask.class));
    private NewInvoiceContext invoice = null;
    private Map<String, InvoiceLineDTO> lineNrInvoiceLineMap = new HashMap<>();

    public SureTaxCompositionTask() {
        super();
    }

    /**
     * This method is called for populating of the tax lines from the Suretax
     * tax engine.
     */
    @Override
    public void apply(NewInvoiceContext invoice, Integer userId)
            throws TaskException {
        this.invoice = invoice;

        extractPluginParameters(userId);
        transactionDate = dateFormatter.format(LocalDateTime.ofInstant(invoice.getBillingDate().toInstant(), ZoneId.systemDefault()));

        SuretaxRequest suretaxRequest = null;

        boolean errorOccurred = false;
        String suretaxRequestUrl = getParameter(SURETAX_REQUEST_URL, "");
        SureTaxBL sureTaxBL = new SureTaxBL(suretaxRequestUrl);

        try {
            suretaxRequest = buildAssembledRequest(invoice, userId);
        } catch (Exception e) {
            if (rollbackInvoiceOnSuretaxError)
                throw new TaskException(e);
            else
                errorOccurred = true;
        }

        if(errorOccurred) {
            return;
        }

        performRequest(suretaxRequest, sureTaxBL);
    }

    protected void handleResponse(SuretaxRequest request, IResponseHeader response) {
        OrderDTO order = invoice.getOrders().get(0).order;

        List<InvoiceLineDTO> taxLines;
        if(calculateLineItemTaxes) {
            taxLines = computeLineItemTaxLines(request, (SuretaxResponseV2) response, order);
        } else {
            taxLines = computeAggregateTaxLines(request, (SuretaxResponseV1)response, order);
        }

        for (InvoiceLineDTO taxLine : taxLines) {
            taxLine.setInvoice(invoice);
            invoice.addResultLine(taxLine);
        }
        // Add the trans id in the invoice
        MetaFieldHelper.setMetaField(invoice.getEntityId(), invoice,
                SURETAX_TRANS_ID_META_FIELD_NAME, uniqueTrackingCode);
    }

    /**
     * Creates a Suretax request from inputs from the invoice.
     *
     * @param invoice
     *            Invoice for which tax lines need to be calculated.
     * @return Returns instance of
     *         com.sapienter.jbilling.client.suretax.request.SuretaxRequest
     * @throws TaskException
     */
    private SuretaxRequest buildAssembledRequest(NewInvoiceContext invoice,
                                                 Integer userId) throws TaskException {

        // Construct a suretax request to get the tax lines.
        SuretaxRequest suretaxRequest = buildBaseSuretaxRequest();
        suretaxRequest.setReturnFileCode("0");
        suretaxRequest.setTotalRevenue(getTotalRevenue(invoice).floatValue());

        List<LineItem> itemList = new ArrayList<>();

        //Lines which are discounts.
        List<InvoiceLineDTO> discountLines = new ArrayList<>();
        //maps the invoice line to the line item created from the invoice line
        Map<InvoiceLineDTO, List<LineItem>> invoiceLineItemMap = new HashMap<InvoiceLineDTO, List<LineItem>>(itemList.size()*2);
        //maps the order id to the list of line items resulting from the order
        Map<Integer, List<LineItem>> orderIdLineItemMap = new HashMap<>();
        //maps the order id to the total for that order - used for order level discount
        Map<Integer, BigDecimal> orderIdTotalMap = new HashMap<>();

        int lineNrIdx = 0;
        for (InvoiceLineDTO invoiceLine : invoice.getResultLines()) {
            //we will subtract discount amounts from the original line
            if(invoiceLine.isDiscountLine()) {
                discountLines.add(invoiceLine);
                continue;
            }

            //If invoice line is for the tax, delegated invoice or if line's item has no Trans Type Code then ignore line for tax calculation.
            if (!invoiceLine.isTaxLine() && invoiceLine.getItem()!=null && isTransTypeCodeFound(invoiceLine)) {
                String lineNr = Integer.toString(lineNrIdx++);

                LOG.debug("Populating itemlist for invoice line: %s", invoiceLine);

                List<LineItem> lineItem = buildLineItem(invoiceLine.getItem().getId(),
                        invoiceLine, uniqueTrackingCode, null, lineNr);

                invoiceLineItemMap.put(invoiceLine, lineItem);

                //add the line item to the list of line items linked to the same order
                Integer orderId = invoiceLine.getOrder().getId();
                List<LineItem> lineItemsForOrder = orderIdLineItemMap.get(orderId);
                BigDecimal orderTotal = orderIdTotalMap.get(orderId);
                if(lineItemsForOrder == null) {
                    lineItemsForOrder = new ArrayList<>();
                    orderIdLineItemMap.put(orderId, lineItemsForOrder);

                    orderTotal = BigDecimal.ZERO;
                }
                lineItemsForOrder.addAll(lineItem);
                orderIdTotalMap.put(orderId, orderTotal.add(invoiceLine.getAmount()));

                itemList.addAll(lineItem);
            }
        }

        //process discounts
        //contains the total for order level discounts per order id
        Map<Integer, BigDecimal> orderDiscount = new HashMap<>();
        for (InvoiceLineDTO discountLine : discountLines) {
            InvoiceLineDTO parentLine = discountLine.getParentLine();
            List<LineItem> sourceLines = (parentLine != null) ? invoiceLineItemMap.get(parentLine) : null;
            //we found a source to subtract the amount from
            if(sourceLines != null && sendLineItemDiscountAsNewLine) {
                // Subtract order line discount from Line Item objects for this invoice line.
                subtractDiscountFromLines(sourceLines, parentLine.getAmount(), discountLine.getAmount());
            } else if(parentLine != null) { //there is a parent but we couldn't find it on the invoice. Should not happen
                LOG.error("Unable to find parent line %s on invoice %s", parentLine, invoice);
                String lineNr = Integer.toString(lineNrIdx++);
                List<LineItem> lineItem = buildLineItem(discountLine.getItem().getId(),
                        discountLine, uniqueTrackingCode, null, lineNr);

                invoiceLineItemMap.put(discountLine, lineItem);
                itemList.addAll(lineItem);
            } else { //this is a discount on the order total
                Integer parentOrderId = discountLine.getOrder().getParentOrder().getId();
                BigDecimal total = orderDiscount.get(parentOrderId);
                if(total == null) {
                    total = BigDecimal.ZERO;
                }
                orderDiscount.put(parentOrderId, total.add(discountLine.getAmount()));
            }
        }

        //now subtract order level discounts from line items
        for(Map.Entry<Integer, BigDecimal> orderDiscountEntry : orderDiscount.entrySet()) {
            Integer orderId = orderDiscountEntry.getKey();
            subtractDiscountFromLines(orderIdLineItemMap.get(orderId), orderIdTotalMap.get(orderId), orderDiscountEntry.getValue());
        }

        suretaxRequest.setItemList(itemList);
        return suretaxRequest;
    }

    /**
     * Check the transTypeCode meta-field availability.
     * if item is a plan then find at plan level else product level.
     * if found then set the value into transTypeCode variable and return true else return false for skip this invoiceLine.
     *
     * @param invoiceLine
     *
     * @return boolean
     */
    private boolean isTransTypeCodeFound(InvoiceLineDTO invoiceLine){
        ItemDTO item = null;
        if (invoiceLine.getItem() != null) {
            item = new ItemDAS().find(invoiceLine.getItem().getId());
        }
        if (item != null) {
            PlanDTO plan = item.firstPlan();
            transTypeCode = (plan != null) ? plan.getMetaField(transactionTypeCodeFieldname) : item.getMetaField(transactionTypeCodeFieldname);
        }
        LOG.info("transTypeCode is: " + transTypeCode);
        return  !(transTypeCode == null || transTypeCode.getValue() == null || transTypeCode.getValue().isEmpty());
    }

    protected List<LineItem> buildLineItem(Integer itemId, InvoiceLineDTO invoiceLine,
                                   String uniqueTrackingCode, PlanDTO plan, String lineNr)
            throws TaskException {
        List<NewInvoiceContext.OrderContext> orders = invoice.getOrders();
        // We need to get the fresh item from the database because
        // the item in the invoiceLine doesn't yet contain meta fields.
        ItemDTO item = new ItemDAS().find(itemId);
        OrderDTO orderDTO = null;
        for (NewInvoiceContext.OrderContext orderCtx : orders) {
            if (orderCtx.order.getId().intValue() == invoiceLine.getOrder().getId()) {
                orderDTO = orderCtx.order;
                break;
            }
        }

        if (null == orderDTO) {
            orderDTO = orders.get(0).order;
        }

        extractOrderParameters(orderDTO);

        LineItem lineItem = constructBaseLineItem(uniqueTrackingCode, item.firstPlan(), item, lineNr);

        lineItem.setUnits(invoiceLine.getQuantity() != null ? invoiceLine
                .getQuantity().intValue() : 0);
        lineItem.setRevenue(invoiceLine.getAmount().floatValue());
        lineItem.setSeconds(invoiceLine.getQuantity() != null ? invoiceLine
                .getQuantity().intValue() : 0);

        lineNrInvoiceLineMap.put(lineNr, invoiceLine);
        LinkedList<LineItem> itemList =  new LinkedList<LineItem>();
        itemList.add(lineItem) ;
        return itemList;
    }

    /**
     * Converts the instance of SuretaxResponseV1 object into tax lines.
     *
     * @param suretaxResponseV1
     * @param order
     * @return
     */
    private List<InvoiceLineDTO> computeAggregateTaxLines(SuretaxRequest request, SuretaxResponseV1 suretaxResponseV1,
                                                          OrderDTO order) {
        List<InvoiceLineDTO> taxLines = new ArrayList<InvoiceLineDTO>();
        InvoiceLineTypeDTO lineType = new InvoiceLineTypeDTO(
                Constants.INVOICE_LINE_TYPE_TAX);
        if (suretaxResponseV1.successful.equals("Y")) {
            for (Group group : suretaxResponseV1.groupList) {
                for (TaxItem taxItem : group.taxList) {
                    InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
                    invoiceLineDTO.setAmount(new BigDecimal(taxItem.taxAmount));
                    invoiceLineDTO.setDescription(taxItem.taxTypeDesc);
                    invoiceLineDTO.setInvoiceLineType(lineType);
                    invoiceLineDTO.setIsPercentage(1);
                    invoiceLineDTO.setPrice(getTaxPercentage(taxItem.taxAmount, request.totalRevenue));
                    invoiceLineDTO.setQuantity(1);
                    taxLines.add(invoiceLineDTO);
                }
            }
        }
        return taxLines;
    }

    private List<InvoiceLineDTO> computeLineItemTaxLines(SuretaxRequest request, SuretaxResponseV2 suretaxResponse,
                                                         OrderDTO order) {
        List<InvoiceLineDTO> taxLines = new ArrayList<>();
        InvoiceLineTypeDTO lineType = new InvoiceLineTypeDTO(
                Constants.INVOICE_LINE_TYPE_TAX);
        if (suretaxResponse.successful.equals("Y")) {
            for (com.sapienter.jbilling.client.suretax.responsev2.Group group : suretaxResponse.groupList) {
                for (com.sapienter.jbilling.client.suretax.responsev2.TaxItem taxItem : group.taxList) {
                    InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
                    invoiceLineDTO.setAmount(new BigDecimal(taxItem.taxAmount));
                    invoiceLineDTO.setDescription(taxItem.taxTypeDesc);
                    invoiceLineDTO.setInvoiceLineType(lineType);
                    invoiceLineDTO.setIsPercentage(1);
                    invoiceLineDTO.setQuantity(1);
                    invoiceLineDTO.setPrice(getTaxPercentage(taxItem.taxAmount, request.totalRevenue));
                    invoiceLineDTO.setParentLine(lineNrInvoiceLineMap.get(group.lineNumber));
                    taxLines.add(invoiceLineDTO);
                }
            }
        }
        return taxLines;
    }

    private BigDecimal getTaxPercentage(String taxAmount, float taxableAmount){
       /* JBIIE-1473 It is possible that generated invoice will have taxable amount greater then zero */
        if (taxAmount != null) {
            try {
                return  new BigDecimal((Float.parseFloat(taxAmount)/taxableAmount)*100f);
            } catch (NumberFormatException e) {
                LOG.error("Tax amount is not valid : %s", taxAmount);
            }
        }
        return BigDecimal.ZERO;
    }

    protected BigDecimal getTotalRevenue(NewInvoiceContext invoice) {

        // calculate TOTAL to include result lines
        invoice.calculateTotal();
        BigDecimal invoiceAmountSum = invoice.getTotal();

        // Remove CARRIED BALANCE from tax calculation to avoid double taxation
        LOG.debug("Carried balance is " + invoice.getCarriedBalance());
        if (null != invoice.getCarriedBalance()) {
            invoiceAmountSum = invoiceAmountSum.subtract(invoice
                    .getCarriedBalance());
        }

        // Remove TAX ITEMS from Invoice to avoid calculating tax on tax
        for (int i = 0; i < invoice.getResultLines().size(); i++) {
            InvoiceLineDTO invoiceLine = (InvoiceLineDTO) invoice
                    .getResultLines().get(i);
            if (null != invoiceLine.getInvoiceLineType()
                    && invoiceLine.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_TAX) {
                invoiceAmountSum = invoiceAmountSum.subtract(invoiceLine
                        .getAmount());
            }
        }

        return invoiceAmountSum;
    }
}
