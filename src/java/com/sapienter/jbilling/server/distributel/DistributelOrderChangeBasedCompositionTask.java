package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.OrderChangeBasedCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class DistributelOrderChangeBasedCompositionTask extends OrderChangeBasedCompositionTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription OL_LEVEL_INVOICE_NOTE_MF_NAME =
            new ParameterDescription("Invoice Note Meta Field Name", false, ParameterDescription.Type.BOOLEAN);

    public DistributelOrderChangeBasedCompositionTask() {
        descriptions.add(OL_LEVEL_INVOICE_NOTE_MF_NAME);
    }

    @Override
    protected void addNoteOnInvoice(NewInvoiceContext invoiceContext, OrderDTO order) {
        super.addNoteOnInvoice(invoiceContext, order);
        if(shouldSkip()) {
            logger.debug("Paramter {} not configured for plugin {}", OL_LEVEL_INVOICE_NOTE_MF_NAME.getName(),
                    DistributelOrderChangeBasedCompositionTask.class.getSimpleName());
            return ;
        }
        String invoiceNote = getOrderLevelInvoiceNoteMfValue(order);
        if(StringUtils.isEmpty(invoiceNote)) {
            return;
        }
        logger.debug("Invoice Note {} from order {} Added on Invoice", invoiceNote, order.getId());
        invoiceContext.appendCustomerNote(invoiceNote);
    }

    private boolean shouldSkip() {
        String invoiceNoteMfName = getParameter(OL_LEVEL_INVOICE_NOTE_MF_NAME.getName(), StringUtils.EMPTY);
        return StringUtils.isEmpty(invoiceNoteMfName);
    }

    private String getOrderLevelInvoiceNoteMfValue(OrderDTO order) {
        String invoiceNoteMfName = getParameter(OL_LEVEL_INVOICE_NOTE_MF_NAME.getName(), StringUtils.EMPTY);
        @SuppressWarnings("rawtypes")
        Optional<MetaFieldValue> orderMfValue = order.getMetaFields()
             .stream()
             .filter(mfValue -> mfValue.getField().getName().equals(invoiceNoteMfName))
             .findFirst();
        if(!orderMfValue.isPresent()) {
            logger.debug("MetaField {} Not found on order {}", invoiceNoteMfName, order.getId());
            return StringUtils.EMPTY;
        }
        return (String) orderMfValue.get().getValue();

    }

}
