package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.CustomerNoteDAS;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;


public class TelcoOrderLineBasedCompositionTask extends InvoiceComposition {

	public static final ParameterDescription PARAMETER_INVOICE_DATE_FORMAT =
            new ParameterDescription("invoice_date_format", false, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_SUPPRESS_ASSET_LINES_WITH_ZERO_PRICE =
	        new ParameterDescription("suppress_asset_lines_with_zero_price", false, ParameterDescription.Type.BOOLEAN);
    private static final CustomerNoteDAS customerNoteDAS = new CustomerNoteDAS();

    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_INVOICE_DATE_FORMAT);
        descriptions.add(PARAMETER_SUPPRESS_ASSET_LINES_WITH_ZERO_PRICE);
    }

	public void apply(NewInvoiceContext invoiceCtx, Integer userId) throws TaskException {
        // initialize resource bundle once, if not initialized
        if (!resourceBundleInitialized) {
            initializeResourceBundleProperties(userId);
        }
        invoiceCtx.appendCustomerNote(getNotesInInvoice(userId, invoiceCtx.getIsReview()));
        /*
         * Process each order being included in this invoice
         */
        for (NewInvoiceContext.OrderContext orderCtx : invoiceCtx.getOrders()) {

            OrderDTO order = orderCtx.order;
            BigDecimal orderContribution = BigDecimal.ZERO;

            if (Integer.valueOf(1).equals(order.getNotesInInvoice())) {
                invoiceCtx.appendCustomerNote(order.getNotes());
            }
            // Add order lines - excluding taxes
            for (OrderLineDTO orderLine : order.getLines()) {
                if (orderLine.getDeleted() == 1) {
                    continue;
                }

                String assetIdentifier = getAssetIdentifiers(orderLine);

                Integer usagePlanId = null;
                if (null != orderLine.getCallIdentifier()) {
                    usagePlanId = getUsagePlanId(order.getUser().getEntity().getId(),
                                                userId,
                                                orderLine.getItemId(),
                                                order.getActiveSince(),orderLine.getCallIdentifier());
                }

                for (PeriodOfTime period : orderCtx.periods) {
                    orderContribution = orderContribution.add(composeInvoiceLine(invoiceCtx, userId,
                                                                                 getAmountByType(orderLine, period),
                                                                                 orderLine, period, null,
                                                                                 orderLine.getCallIdentifier(),
                                                                                 orderLine.getCallCounter(),
                                                                                 assetIdentifier, usagePlanId, allowLineWithZero(orderLine)));
                }
            }
        }


        /*
         * add delegated invoices
         */
        delegateInvoices(invoiceCtx);
    }

    /**
     * Get usage plan for give user and usage item
     * @param userId
     * @param itemId
     * @param usageActiveSince
     * @return
     */
    private Integer getUsagePlanId(Integer entityId, Integer userId, Integer itemId, Date usageActiveSince, String identifier) {
        Integer dormancyPlanId = getDormancyPlanId(entityId);
        Integer usagePlanId;
        try {
            usagePlanId = new InvoiceDAS().getUsagePlanId(userId, itemId, usageActiveSince,identifier);
            /*  This is FC Specific condition to skip Dormancy plan Id while setting usage plan id on usage
             *  invoice line But If company level meta field not defined then it work as per generic implementation
             */
            if (null == dormancyPlanId || (null != usagePlanId && !dormancyPlanId.equals(usagePlanId))) {
                return usagePlanId;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

		return null;
    }

    /**
     * FC specific company level meta field
     * Fetch Company level meta field Dormancy Plan id
     * @param entityId
     * @return plan Id
     */
    private Integer getDormancyPlanId(Integer entityId) {
    	String dormancyPlanId = new MetaFieldDAS().getComapanyLevelMetaFieldValue(MetaFieldName.DORMANCY_PLAN_ID.getMetaFieldName(), entityId);
        return (null != dormancyPlanId) ? Integer.valueOf(dormancyPlanId) : null;
    }

    @Override
    protected void setDateFormater(String invoiceDateFormat) {
        dateFormat = Util.isValidDateFormat(getOptionalParameter(PARAMETER_INVOICE_DATE_FORMAT.getName(),invoiceDateFormat)) ?
		getOptionalParameter(PARAMETER_INVOICE_DATE_FORMAT.getName(),invoiceDateFormat) :
		invoiceDateFormat;
	}

    protected final String getOptionalParameter(String key, String valueIfNull) {
        Object value = parameters.get(key);
        return (value instanceof String) ? (String) value : valueIfNull;
    }

    /**
     * One time invoice note for one time announcement.
     * @param userId
     * @param isReview
     */
    private static String getNotesInInvoice(Integer userId, Integer isReview){
        UserDTO user = new UserBL(userId).getEntity();
        StringBuilder customerNote = new StringBuilder();
        List<CustomerNoteDTO> customerNotes =
                customerNoteDAS.findByCustomer(user.getCustomer().getId(), user.getEntity().getId());
        for (CustomerNoteDTO note : customerNotes) {
            if (note.isNotesInInvoiceChecked()) {
                if (0 == isReview) {
                    customerNoteDAS.excludeNotesInInvoice(user.getCustomer().getId(), note.getNoteId(), false);
                }
                customerNote.append(note.getNoteContent());
            }
        }
        return customerNote.append(Constants.BLANK_STRING).toString();
    }

    private boolean allowLineWithZero(OrderLineDTO orderLine) {
        if(null == orderLine.getItem()) {
            return true;
        }
        return !(getParameter(PARAMETER_SUPPRESS_ASSET_LINES_WITH_ZERO_PRICE.getName(), true)
                && orderLine.getItem().isAssetEnabledItem()
                && (null == orderLine.getItem().getPrice() ||  orderLine.getItem().getPrice().compareTo(BigDecimal.ZERO) == 0));
    }
}
