/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2017] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.invoiceSummary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDAS;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteLineDAS;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDAS;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDTO;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;

/**
 * @author Ashok Kale
 */
public class InvoiceSummaryBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(InvoiceSummaryBL.class));

    private InvoiceSummaryDAS invoiceSummaryDas=null;
    private InvoiceSummaryDTO invoiceSummaryDto=null;


    public InvoiceSummaryBL(Integer invoiceSummaryId){
        init();
        set(invoiceSummaryId);
    }

    public InvoiceSummaryBL(){
        init();
    }

    public void set(Integer invoiceSummaryId) {
        invoiceSummaryDto=invoiceSummaryDas.find(invoiceSummaryId);
    }

    private void init() {
        invoiceSummaryDas=new InvoiceSummaryDAS();
        invoiceSummaryDto=new InvoiceSummaryDTO();
    }

    public InvoiceSummaryDTO getInvoiceSummaryDTO() {
        return this.invoiceSummaryDto;
    }

    public InvoiceSummaryDTO getEntity() {
        return this.invoiceSummaryDto;
    }


    public Integer create(InvoiceDTO invoiceDTO) {
        InvoiceDTO lastInvoice = getLastInvoice(invoiceDTO.getUserId(), invoiceDTO.getId());
        Date lastInvoiceDate = null != lastInvoice ? lastInvoice.getCreateDatetime() : null;
        InvoiceSummaryDTO invoiceSummary = new InvoiceSummaryDTO();
        BigDecimal monthlyCharges = getTotalCharges(invoiceDTO.getInvoiceLines(), Constants.INVOICE_LINE_TYPE_ITEM_RECURRING);
        BigDecimal usageCharges = getTotalCharges(invoiceDTO.getInvoiceLines(), Constants.INVOICE_LINE_TYPE_ITEM_ONETIME);
        BigDecimal fees = getTotalCharges(invoiceDTO.getInvoiceLines(), Constants.INVOICE_LINE_TYPE_PENALTY);
        BigDecimal taxes = getTotalCharges(invoiceDTO.getInvoiceLines(), Constants.INVOICE_LINE_TYPE_TAX);
        BigDecimal totalAdjustmentAmount = getTotalAdjustmentCharges(invoiceDTO.getUserId(), invoiceDTO.getInvoiceLines(), lastInvoiceDate, invoiceDTO.getCreateDatetime());
        BigDecimal newCharges = getTotalNewCharges(invoiceDTO.getInvoiceLines(),
                Constants.INVOICE_LINE_TYPE_DUE_INVOICE, Constants.INVOICE_LINE_TYPE_ADJUSTMENT).add(totalAdjustmentAmount);
        BigDecimal amountOfLastStatement = null != lastInvoice ? invoiceSummaryDas.getAmountOfLastStatement(lastInvoice.getId()) : BigDecimal.ZERO;
        BigDecimal totalDue = UserBL.getBalance(invoiceDTO.getUserId());
        invoiceSummary.setCreationInvoiceId(invoiceDTO.getId());
        invoiceSummary.setUserId(invoiceDTO.getUserId());
        invoiceSummary.setMonthlyCharges(monthlyCharges);
        invoiceSummary.setUsageCharges(usageCharges);
        invoiceSummary.setFees(fees);
        invoiceSummary.setTaxes(taxes);
        invoiceSummary.setNewCharges(newCharges);
        invoiceSummary.setTotalDue(totalDue);
        invoiceSummary.setAmountOfLastStatement(amountOfLastStatement);
        invoiceSummary.setAdjustmentCharges(totalAdjustmentAmount);
        invoiceSummary.setPaymentReceived(getTotalPaymentsReceived(invoiceDTO.getUserId(), lastInvoiceDate, invoiceDTO.getCreateDatetime()).negate());
        invoiceSummary.setInvoiceDate(invoiceDTO.getCreateDatetime());
        invoiceSummary.setLastInvoiceDate(lastInvoiceDate);
        invoiceSummary.setCreateDatetime(TimezoneHelper.serverCurrentDate());

        invoiceSummary = save(invoiceSummary);

        BigDecimal calculatedNewCharges = monthlyCharges.add(usageCharges).add(fees).add(taxes);
        if (!newCharges.equals(calculatedNewCharges)) {
            LOG.debug("Calculated new changes"+ calculatedNewCharges +"not matched with invoice line amount sum"+ newCharges );
        }
        LOG.debug("Invoice Summary Created: " + invoiceSummary);

        return invoiceSummary.getId();
    }

    /**
     * Delete Invoice summary by invoice ID
     * @param invoiceId
     */
    public void delete(Integer invoiceId) {
        invoiceSummaryDto = invoiceSummaryDas.findInvoiceSummaryByInvoice(invoiceId);
        if (null != invoiceSummaryDto) {
            invoiceSummaryDas.delete(invoiceSummaryDto);
        }
    }

    public InvoiceSummaryDTO save(InvoiceSummaryDTO invoiceSummaryDTO) {
        return invoiceSummaryDas.save(invoiceSummaryDTO);
    }

    public InvoiceSummaryWS getInvoiceSummaryWS() {
        return getWS(this.invoiceSummaryDto);
    }

    public InvoiceSummaryWS getInvoiceSummaryByInvoiceId(Integer invoiceId) {
        InvoiceSummaryDTO dto = invoiceSummaryDas.findInvoiceSummaryByInvoice(invoiceId);
        return dto != null ? getWS(dto) : null;

    }

    public ItemizedAccountWS getItemizedAccountByInvoiceId(Integer invoiceId, Integer callerLanguageId) {
        InvoiceSummaryDTO dto = invoiceSummaryDas.findInvoiceSummaryByInvoice(invoiceId);
        return dto != null ? InvoiceSummaryBL.getItemizedAccountWS(dto, callerLanguageId) : null;

    }

    public static ItemizedAccountWS getItemizedAccountWS(InvoiceSummaryDTO invoiceSummaryDTO, Integer callerLanguageId) {
        if (invoiceSummaryDTO != null) {
            ItemizedAccountWS itemizedAccountWS = new ItemizedAccountWS();

            itemizedAccountWS.setInvoiceSummary(getWS(invoiceSummaryDTO));
            itemizedAccountWS.setMonthlyCharges(getInvoiceLinesByInvoiceAndType(invoiceSummaryDTO.getCreationInvoiceId(),
                    Constants.INVOICE_LINE_TYPE_ITEM_RECURRING));
            itemizedAccountWS.setUsageCharges(getInvoiceLinesByInvoiceAndType(invoiceSummaryDTO.getCreationInvoiceId(),
                    Constants.INVOICE_LINE_TYPE_ITEM_ONETIME));
            itemizedAccountWS.setFees(getInvoiceLinesByInvoiceAndType(invoiceSummaryDTO.getCreationInvoiceId(),
                    Constants.INVOICE_LINE_TYPE_PENALTY));
            itemizedAccountWS.setTaxes(getInvoiceLinesByInvoiceAndType(invoiceSummaryDTO.getCreationInvoiceId(),
                    Constants.INVOICE_LINE_TYPE_TAX));
            itemizedAccountWS.setPaymentsAndRefunds(getpaymentsAndRefunds(invoiceSummaryDTO.getUserId(),
                    invoiceSummaryDTO.getLastInvoiceDate(), invoiceSummaryDTO.getInvoiceDate(), callerLanguageId));
            itemizedAccountWS.setCreditAdjustments(getCreditAdjustments(invoiceSummaryDTO.getUserId(), invoiceSummaryDTO.getCreationInvoiceId(),
                    invoiceSummaryDTO.getLastInvoiceDate(), invoiceSummaryDTO.getInvoiceDate()));

            return itemizedAccountWS;
        }

        return null;
    }

    public static InvoiceSummaryWS getWS(InvoiceSummaryDTO invoiceSummaryDTO) {
        if (invoiceSummaryDTO != null) {
            InvoiceSummaryWS invoiceSummaryWS = new InvoiceSummaryWS();

            invoiceSummaryWS.setId(invoiceSummaryDTO.getId());
            invoiceSummaryWS.setCreationInvoiceId(invoiceSummaryDTO.getCreationInvoiceId());
            invoiceSummaryWS.setUserId(invoiceSummaryDTO.getUserId());
            invoiceSummaryWS.setMonthlyCharges(invoiceSummaryDTO.getMonthlyCharges());
            invoiceSummaryWS.setUsageCharges(invoiceSummaryDTO.getUsageCharges());
            invoiceSummaryWS.setFees(invoiceSummaryDTO.getFees());
            invoiceSummaryWS.setTaxes(invoiceSummaryDTO.getTaxes());
            invoiceSummaryWS.setAdjustmentCharges(invoiceSummaryDTO.getAdjustmentCharges());
            invoiceSummaryWS.setAmountOfLastStatement(invoiceSummaryDTO.getAmountOfLastStatement());
            invoiceSummaryWS.setPaymentReceived(invoiceSummaryDTO.getPaymentReceived());
            invoiceSummaryWS.setNewCharges(invoiceSummaryDTO.getNewCharges());
            invoiceSummaryWS.setTotalDue(invoiceSummaryDTO.getTotalDue());
            invoiceSummaryWS.setInvoiceDate(invoiceSummaryDTO.getInvoiceDate());
            invoiceSummaryWS.setLastInvoiceDate(invoiceSummaryDTO.getLastInvoiceDate());
            invoiceSummaryWS.setCreateDatetime(invoiceSummaryDTO.getCreateDatetime());

            return invoiceSummaryWS;
        }

        return null;
    }

    /**
     * Get Invoice Lines By Invoice Line Type
     * @param invoiceId
     * @param invoiceLineTypeId
     * @return InvoiceLineDTO
     */
    public static com.sapienter.jbilling.server.entity.InvoiceLineDTO[]
            getInvoiceLinesByInvoiceAndType(Integer invoiceId, Integer invoiceLineTypeId) {
        int m = 0;
        List<InvoiceLineDTO> lines = new InvoiceLineDAS().getInvoiceLinesByType(invoiceId, invoiceLineTypeId);
        com.sapienter.jbilling.server.entity.InvoiceLineDTO[] invoiceLines =
                new com.sapienter.jbilling.server.entity.InvoiceLineDTO[lines.size()];
        for (InvoiceLineDTO line : lines) {
            invoiceLines[m++] = new com.sapienter.jbilling.server.entity.InvoiceLineDTO(line.getId(),
                    line.getDescription(), line.getAmount(), line.getPrice(), line.getQuantity(),
                    line.getDeleted(), line.getItem() == null ? null : line.getItem().getId(),
                            line.getSourceUserId(), line.getIsPercentage(), line.getCallIdentifier(),
                            line.getUsagePlanId(), line.getCallCounter(), line.getTaxRate(), line.getTaxAmount(),
                            line.getGrossAmount());
        }
        return invoiceLines;

    }

    /**
     * Get the payments and Refund in between 2 two invoices
     * @param userId
     * @param lastInvoiceDate
     * @param currentInvoiceDate
     * @param callerLanguageId
     * @return Payments[]
     */
    public static PaymentWS[] getpaymentsAndRefunds(Integer userId, Date lastInvoiceDate, Date currentInvoiceDate, Integer callerLanguageId) {
        List<PaymentDTO> payments = new PaymentDAS().findPaymentsBetweenLastAndCurrentInvoiceDates(userId, lastInvoiceDate, currentInvoiceDate);
        if (payments == null) {
            return null;
        }
        List<PaymentWS> paymentsWS = new ArrayList<>();
        PaymentBL bl = null;
        for (PaymentDTO dto : payments) {
            bl = new PaymentBL(dto.getId());
            PaymentWS wsdto = PaymentBL.getWS(bl.getDTOEx(callerLanguageId));
            paymentsWS.add(wsdto);
        }
        return paymentsWS.toArray(new PaymentWS[paymentsWS.size()]);
    }

    /**
     * Get the Credit Payments, Credit Notes and Invoice lines of type Adjustment in between 2 two invoices
     * @param userId
     * @param lastInvoiceDate
     * @param currentInvoiceDate
     * @return CreditAdjustmentWS[]
     */
    public static CreditAdjustmentWS[] getCreditAdjustments(Integer userId, Integer invoiceId, Date lastInvoiceDate, Date currentInvoiceDate) {
        List <CreditAdjustmentWS> invoiceAdjustmentSectionList =new ArrayList<>();
        List<PaymentDTO> creditPayments = new PaymentDAS().findCreditPaymentsBetweenLastAndCurrentInvoiceDates(userId, lastInvoiceDate, currentInvoiceDate);
        List<CreditNoteLineDTO> creditNoteLines = new CreditNoteLineDAS().findCreditNoteLinesBetweenLastAndCurrentInvoiceDates(userId, lastInvoiceDate, currentInvoiceDate);
        List<InvoiceLineDTO> invoiceLines = new InvoiceLineDAS().getInvoiceLinesByType(invoiceId, Constants.ORDER_LINE_TYPE_ADJUSTMENT);

        creditPayments.stream().forEach(creditPayment -> invoiceAdjustmentSectionList.add(getCreditPaymentAdjustmentWS(creditPayment)));

        List<CreditAdjustmentWS> creditNoteAdjustmentList = getCreditNoteAdjustmentWS(creditNoteLines);
        creditNoteAdjustmentList.stream().forEach(invoiceAdjustmentSectionList::add);

        List<CreditAdjustmentWS> creditInvoiceLineAdjustmentList = getCreditInvoiceLineAdjustmentWS(invoiceLines);
        creditInvoiceLineAdjustmentList.stream().forEach(invoiceAdjustmentSectionList::add);
        return invoiceAdjustmentSectionList.toArray(new CreditAdjustmentWS[invoiceAdjustmentSectionList.size()]);
    }

    /**
     * Get total adjustment (Credit Payments + Credit Notes + Invoice Lines Of type Adjustment) in between two invoice dates
     * @param userId
     * @param lastInvoiceDate
     * @param currentInvoiceDate
     * @return
     */
    public BigDecimal getTotalAdjustmentCharges(Integer userId, Collection<InvoiceLineDTO> invoiceLines, Date lastInvoiceDate, Date currentInvoiceDate) {
        BigDecimal totalAdjustmentCharges;
        List<PaymentDTO> creditPayments = new PaymentDAS().
                findCreditPaymentsBetweenLastAndCurrentInvoiceDates(userId, lastInvoiceDate, currentInvoiceDate);
        List<CreditNoteDTO> creditNotes = new CreditNoteDAS().
                findCreditNotesBetweenLastAndCurrentInvoiceDates(userId, lastInvoiceDate, currentInvoiceDate);
        totalAdjustmentCharges = creditPayments.stream().map(PaymentDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAdjustmentCharges = totalAdjustmentCharges.add(creditNotes.stream().map(CreditNoteDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal creditInvoiceLineCharges = getTotalCharges(invoiceLines, Constants.INVOICE_LINE_TYPE_ADJUSTMENT);
        totalAdjustmentCharges = (totalAdjustmentCharges.negate()).add(creditInvoiceLineCharges);
        return totalAdjustmentCharges;
    }

    /**
     * Get total Payment Received = (Payments(Entered + Successful) - Refunds) in between two invoice dates
     * @param userId
     * @param lastInvoiceDate
     * @param currentInvoiceDate
     * @return
     */
    public BigDecimal getTotalPaymentsReceived(Integer userId, Date lastInvoiceDate, Date currentInvoiceDate) {
        return new PaymentDAS().findTotalRevenueByUserInBetweenTwoInvoices(userId, lastInvoiceDate, currentInvoiceDate);
    }

    private static CreditAdjustmentWS getCreditPaymentAdjustmentWS(PaymentDTO payment) {
        CreditAdjustmentWS creditPaymentAdjustment = new CreditAdjustmentWS();
        creditPaymentAdjustment.setPaymentId(payment.getId());
        creditPaymentAdjustment.setCurrencyId(payment.getCurrency().getId());
        creditPaymentAdjustment.setPaymentResultId(payment.getPaymentResult().getId());
        creditPaymentAdjustment.setPaymentMethodId(payment.getPaymentMethod().getId());
        creditPaymentAdjustment.setAmount(payment.getAmount());
        creditPaymentAdjustment.setBalance(payment.getBalance());
        creditPaymentAdjustment.setDescription("Credit Payment");
        creditPaymentAdjustment.setPaymentCreateDatetime(payment.getCreateDatetime());
        creditPaymentAdjustment.setPaymentDate(payment.getPaymentDate());
        creditPaymentAdjustment.setIsRefund(payment.getIsRefund());
        creditPaymentAdjustment.setPaymentNotes(payment.getPaymentNotes());
        creditPaymentAdjustment.setType(Constants.CREDIT_TYPE_CREDIT_PAYMENT);
        return creditPaymentAdjustment;
    }

    private static List<CreditAdjustmentWS> getCreditNoteAdjustmentWS(List<CreditNoteLineDTO> creditNoteLines) {
        List<CreditAdjustmentWS> creditNoteAdjustments = new ArrayList<>();
        for (CreditNoteLineDTO creditNoteLine: creditNoteLines) {
            CreditAdjustmentWS creditAdjustment = new CreditAdjustmentWS();
            creditAdjustment.setCurrencyId(creditNoteLine.getCreditNoteDTO().getCreationInvoice().getCurrency().getId());
            creditAdjustment.setAmount(creditNoteLine.getAmount());
            creditAdjustment.setBalance(BigDecimal.ZERO);
            creditAdjustment.setDescription(creditNoteLine.getDescription());
            creditAdjustment.setIsRefund(0);
            creditAdjustment.setType(Constants.CREDIT_TYPE_CREDIT_NOTE);
            creditAdjustment.setCreditNoteId(creditNoteLine.getCreditNoteDTO().getId());
            creditAdjustment.setCreditNoteLineId(creditNoteLine.getId());
            creditAdjustment.setCreditNoteInvoiceId(creditNoteLine.getCreditNoteDTO().getCreationInvoice().getId());
            creditAdjustment.setCreditNoteInvoiceLineId(creditNoteLine.getCreationInvoiceLine().getId());
            creditAdjustment.setCreditNoteDate(creditNoteLine.getCreditNoteDTO().getCreateDateTime());
            creditNoteAdjustments.add(creditAdjustment);
        }
        return creditNoteAdjustments;
    }

    private static List<CreditAdjustmentWS> getCreditInvoiceLineAdjustmentWS(List<InvoiceLineDTO> invoiceLines) {
        List<CreditAdjustmentWS> creditInvoiceLineAdjustments = new ArrayList<>();
        for (InvoiceLineDTO invoiceLine: invoiceLines) {
            CreditAdjustmentWS creditInvoiceLineAdjustment = new CreditAdjustmentWS();
            creditInvoiceLineAdjustment.setCurrencyId(invoiceLine.getInvoice().getCurrency().getId());
            creditInvoiceLineAdjustment.setAmount(invoiceLine.getAmount());
            creditInvoiceLineAdjustment.setBalance(BigDecimal.ZERO);
            creditInvoiceLineAdjustment.setDescription(invoiceLine.getDescription());
            creditInvoiceLineAdjustment.setCreditInvoiceLineDate(invoiceLine.getInvoice().getCreateDatetime());
            creditInvoiceLineAdjustment.setType(Constants.CREDIT_TYPE_CREDIT_INVOICE_LINE);
            creditInvoiceLineAdjustments.add(creditInvoiceLineAdjustment);
        }
        return creditInvoiceLineAdjustments;
    }

    /**
     * Get Payments and Refunds in between 2 invoices
     * @param invoiceId
     * @param callerLanguageId
     * @return PaymentWS[]
     */
    public PaymentWS[] getpaymentsAndRefundsByInvoiceId(Integer invoiceId, Integer callerLanguageId) {
        InvoiceSummaryDTO dto = invoiceSummaryDas.findInvoiceSummaryByInvoice(invoiceId);
        return getpaymentsAndRefunds(dto.getUserId(),dto.getLastInvoiceDate(), dto.getInvoiceDate(), callerLanguageId);

    }

    /**
     * Get Credit Payments and Credit Notes in between 2 invoices
     * @param invoiceId
     * @return
     */
    public CreditAdjustmentWS[] getCreditAdjustmentsByInvoiceId(Integer invoiceId) {
        InvoiceSummaryDTO dto = invoiceSummaryDas.findInvoiceSummaryByInvoice(invoiceId);
        return getCreditAdjustments(dto.getUserId(), dto.getCreationInvoiceId(), dto.getLastInvoiceDate(), dto.getInvoiceDate());
    }

    /**
     * Get total charges by invoice line type
     * @param invoiceLines
     * @param invoiceLineTypeId
     * @return BigDecimal
     */
    private static BigDecimal getTotalCharges(Collection<InvoiceLineDTO> invoiceLines, Integer invoiceLineTypeId) {
        return invoiceLines.stream()
                .filter(invoiceLine -> Integer.compare(invoiceLine.getInvoiceLineType().getId(), invoiceLineTypeId) == 0)
                .map(InvoiceLineDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total of new charges by invoice line except invoice lines whose type is due invoice
     * @param invoiceLines
     * @return
     */
    private static BigDecimal getTotalNewCharges(Collection<InvoiceLineDTO> invoiceLines, Integer dueInvoiceTypeId, Integer adjustmentTypeId) {
        return invoiceLines.stream()
                .filter(invoiceLine -> Integer.compare(invoiceLine.getInvoiceLineType().getId(), dueInvoiceTypeId) != 0 &&
                Integer.compare(invoiceLine.getInvoiceLineType().getId(), adjustmentTypeId) != 0)
                .map(InvoiceLineDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get Second Last Invoice Id by user id and invoice id except credit invoice
     * @param userId
     * @return InvoiceDTO
     */
    private static InvoiceDTO getLastInvoice(Integer userId, Integer invoiceId) {
        Integer secondLastInvoiceId = new InvoiceDAS().getLastInvoiceId(userId, invoiceId);
        return new InvoiceDAS().find(secondLastInvoiceId);
    }

    public void deleteByInvoice(Integer invoiceId) {
        invoiceSummaryDas.deleteByInvoice(invoiceId);
    }

    public void deleteByBillingProcessId(Integer billingProcessId) {
        invoiceSummaryDas.deleteByBillingProcessId(billingProcessId);
    }
}
