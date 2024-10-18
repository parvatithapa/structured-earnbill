package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineTypeDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Created by neeraj on 6/7/16.
 * NGES Plugin: This plugin used to adjust the previous rebill order(Created by rebill meter read) on the current invoice. This plugin find all the suspended and rebill orders of the customer
 * and adjust them in the current invoice and mark the rebill order as Finished.
 *
 * Also rebill order will be adjusted only if an invoice contains an 'one time' order because we always create one time order for a Meter read and a rebill order should be adjusted
 * with the MeterRead order's invoice.
 */
public class CancelRebillCompostitonTask  extends PluggableTask implements InvoiceCompositionTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CancelRebillCompostitonTask.class));

    public void apply (NewInvoiceContext invoice, Integer userId) throws TaskException {

        if(!invoiceContainsMeterReadOrder(invoice)){
            LOG.debug("Invoice does not contains One time order so escaping the processing of rebill order");
            return;
        }

        UserDTO userDTO=new UserDAS().findByUserId(userId, getEntityId());
        LOG.debug("User : "+userDTO);
        OrderStatusFlag orderStatusFlag=OrderStatusFlag.FINISHED;
        Integer orderStatus = new OrderStatusDAS().getDefaultOrderStatusId(orderStatusFlag,  getEntityId());

        for(OrderDTO order:userDTO.getOrders()){

            if(order.getOrderStatus().getOrderStatusFlag()==OrderStatusFlag.NOT_INVOICE ){
                //checking the order is rebill order.
                MetaFieldValue<Boolean> isRebillOrder=order.getMetaField(FileConstants.IS_REBILL_ORDER);
                LOG.debug("Order "+ order.getId()+" rebill metafield value : " + isRebillOrder);
                if(isRebillOrder==null || isRebillOrder.getValue()==null || !isRebillOrder.getValue()){
                    continue;
                }

                LOG.debug("Rebill order : "+ order);
                invoice.getResultLines().addAll(createInvoiceLines(order, invoice));
                //updating rebill order to finish if it is real billing run.
                if(!invoice.isReviewInvoice()){
                    order.setStatusId(orderStatus);
                }
            }
        }
    }

    // this method create invoice line form the rebill order.
    private List<InvoiceLineDTO> createInvoiceLines(OrderDTO order, InvoiceDTO invoice){
       List<InvoiceLineDTO> invoiceLineDTOs=new ArrayList<>();


        for(OrderLineDTO orderLine: order.getLines()){
            InvoiceLineTypeDTO lineType = new InvoiceLineTypeDTO(
                    Constants.INVOICE_LINE_TYPE_ITEM_ONETIME);
            InvoiceLineDTO invoiceLineDTO=new InvoiceLineDTO();

            invoiceLineDTO.setAmount(orderLine.getAmount());
            invoiceLineDTO.setDescription(orderLine.getDescription());
            invoiceLineDTO.setInvoiceLineType(lineType);
            invoiceLineDTO.setIsPercentage(0);
            invoiceLineDTO.setQuantity(orderLine.getQuantity());
            invoiceLineDTO.setPrice(orderLine.getPrice());
            invoiceLineDTO.setInvoice(invoice);
            invoiceLineDTO.setOrder(order);
            invoiceLineDTO.setItem(orderLine.getItem());
            invoiceLineDTOs.add(invoiceLineDTO);
        }

        return invoiceLineDTOs;
    }

    //checking an invoice contains one time order(Meter read order).
    private Boolean invoiceContainsMeterReadOrder(NewInvoiceContext invoice){
        Boolean hasOneTimeOrder=false;
        if(invoice.getOrders().stream().filter((NewInvoiceContext.OrderContext orderContext)-> orderContext.order.getOrderPeriod().getId()==Constants.ORDER_PERIOD_ONCE).count()>0){
            hasOneTimeOrder= true;
        }

        return hasOneTimeOrder;
    }

}
