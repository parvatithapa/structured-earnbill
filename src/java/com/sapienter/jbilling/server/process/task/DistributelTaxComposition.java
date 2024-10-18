package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineTypeDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.spa.DistributelTaxHelper;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.TaxDistributel;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;


import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by pablo_galera on 18/01/17.
 */
public class DistributelTaxComposition extends PluggableTask implements InvoiceCompositionTask {

    @Override
    public void apply(NewInvoiceContext invoice, Integer userId) throws TaskException {
        UserDTO user = new UserBL(userId).getEntity();
        DistributelTaxHelper distributelTaxHelper = new DistributelTaxHelper();

        MetaField provinceMF = MetaFieldBL.getFieldByName(getEntityId(), new EntityType[]{EntityType.INVOICE}, SpaConstants.TAX_PROVINCE);
        if (provinceMF != null) {
            invoice.setMetaField(provinceMF, distributelTaxHelper.getProvince(user));
        }

        MetaField previousBalanceMF = MetaFieldBL.getFieldByName(getEntityId(), new EntityType[]{EntityType.INVOICE}, SpaConstants.PREVIOUS_BALANCE);
        if (previousBalanceMF != null) {
            invoice.setMetaField(previousBalanceMF, UserBL.getBalance(userId));
        }

        ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
        String message;
        BigDecimal totalWithoutTaxes = InvoiceBL.getTotalWithoutCarried(invoice.getResultLines()).add(UserBL.getBalance(userId));
        if (BigDecimal.ZERO.compareTo(totalWithoutTaxes) >= 0) {
            message = getStringFromBundleByKeyWithParams(bundle, "invoiceTemplate.payment.message.paid");
            MetaField paymentMessageMF = MetaFieldBL.getFieldByName(getEntityId(), new EntityType[]{EntityType.INVOICE}, SpaConstants.PAYMENT_MESSAGE);
            if (paymentMessageMF != null) {
                invoice.setMetaField(paymentMessageMF, message);
            }
        }

        BigDecimal invoiceTotalForTaxes = calculateTotal(invoice);
        if (BigDecimal.ZERO.compareTo(invoiceTotalForTaxes) != 0 && !DistributelTaxHelper.isCustomerTaxExcempt(user.getCustomer())) {
            List<TaxDistributel> taxList = distributelTaxHelper.getTaxList(user, invoice.getBillingDate(), invoiceTotalForTaxes);

            InvoiceLineTypeDTO lineType = new InvoiceLineTypeDTO(Constants.INVOICE_LINE_TYPE_TAX);

            for (TaxDistributel tax : taxList) {
                InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
                invoiceLineDTO.setAmount(tax.getTotal());
                invoiceLineDTO.setDescription(tax.getDescription());
                invoiceLineDTO.setInvoiceLineType(lineType);
                invoiceLineDTO.setIsPercentage(1);
                invoiceLineDTO.setPrice(tax.getPercentage());
                invoiceLineDTO.setQuantity(1);
                invoiceLineDTO.setInvoice(invoice);
                invoice.addResultLine(invoiceLineDTO);
            }
        }

        if (BigDecimal.ZERO.compareTo(totalWithoutTaxes) < 0) {

            //recalculate with taxes
            String totalWithTaxes = Util.decimal2string(InvoiceBL.getTotalWithoutCarried(invoice.getResultLines()).add(UserBL.getBalance(userId)), new UserBL(user.getId()).getLocale(), Util.AMOUNT_FORMAT_PATTERN);

            boolean isAutoPayment = user.getPaymentInstruments().stream().anyMatch(
                    pi -> null != pi.getMetaFields().stream().filter(
                            mf -> SpaConstants.AUTOPAYMENT_AUTHORIZATION.equals(mf.getField().getName()) &&
                                    Boolean.TRUE.equals(mf.getValue())
                    ).findAny().orElse(null)
            );

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(bundle.getString("format.date"));
            LocalDate localDate = DateConvertUtils.asLocalDate(new Date());
            String billingDate = localDate.format(formatter);

            if (isAutoPayment) {
                message = getStringFromBundleByKeyWithParams(bundle, "invoiceTemplate.payment.message.with.creditCard", totalWithTaxes, billingDate);
            } else {
                billingDate = localDate.plusDays(7).format(formatter);
                message = getStringFromBundleByKeyWithParams(bundle, "invoiceTemplate.payment.message.without.creditCard", totalWithTaxes, billingDate);
            }

            MetaField paymentMessageMF = MetaFieldBL.getFieldByName(getEntityId(), new EntityType[]{EntityType.INVOICE}, SpaConstants.PAYMENT_MESSAGE);
            if (paymentMessageMF != null) {
                invoice.setMetaField(paymentMessageMF, message);
            }
        }
    }
    
     

    private String getStringFromBundleByKeyWithParams(ResourceBundle bundle, String key, Object... params) {
        return MessageFormat.format(bundle.getString(key), params);
    }

    private BigDecimal calculateTotal(NewInvoiceContext invoice) {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLineDTO line : invoice.getResultLines()) {
            if (!Constants.INVOICE_LINE_TYPE_DUE_INVOICE.equals(line.getInvoiceLineType().getId()) && !isTaxExempt(line)) {
                total = total.add(line.getAmount());
            }
        }
        return total;
    }

    private boolean isTaxExempt(InvoiceLineDTO line) {
        if (line.getItem() != null) {
            ItemDTO item = new ItemDAS().find(line.getItem().getId());
            if (item.getMetaField(SpaConstants.TAX_EXEMPT) != null) {
                return Boolean.valueOf(item.getMetaField(SpaConstants.TAX_EXEMPT).getValue().toString());
            }
        }
        return false;
    }

}

