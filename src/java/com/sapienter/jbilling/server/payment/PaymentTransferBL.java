package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.common.FormatLogger;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.payment.event.PaymentDeletedEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.log4j.Logger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentTransferDAS;
import com.sapienter.jbilling.server.payment.db.PaymentTransferDTO;

/**
 * @author Javier Rivero
 * @since 13/01/16.
 */
public class PaymentTransferBL extends ResultList {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaymentTransferBL.class));

    private PaymentTransferDAS paymentTransferDas = null;
    private PaymentTransferDTO paymentTransfer = null;

    public PaymentTransferBL() {
        init();
    }

    public PaymentTransferBL(int paymentTransferId) {
        init();
        this.paymentTransfer = paymentTransferDas.find(paymentTransferId);
    }

    private void init() {
        try {
            paymentTransferDas = new PaymentTransferDAS();
            paymentTransfer = new PaymentTransferDTO();

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Integer createPaymentTransfer(PaymentDTOEx dto, PaymentTransferWS paymentTransferWS) {
        LOG.debug("paymentTransfer from user "+ dto.getBaseUser().getUserId() +" to user "+ paymentTransferWS.getToUserId());
        paymentTransfer.setPayment(dto);
        paymentTransfer.setAmount(dto.getAmount());
        paymentTransfer.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        paymentTransfer.setCreatedBy(paymentTransferWS.getCreatedBy());
        paymentTransfer.setFromUserId(dto.getBaseUser().getUserId());
        paymentTransfer.setToUserId(paymentTransferWS.getToUserId());
        paymentTransfer.setPaymentTransferNotes((null != paymentTransferWS.getPaymentTransferNotes() &&
                !paymentTransferWS.getPaymentTransferNotes().isEmpty()) ?
                paymentTransferWS.getPaymentTransferNotes() : "Payment Transfer from user "+  dto.getBaseUser().getUserId() +" To user "+paymentTransferWS.getToUserId());

        LOG.debug("Creating a new payment transfer for payment %s", dto.getId());
        paymentTransfer = paymentTransferDas.save(paymentTransfer);

        LOG.debug("Created a new payment transfer %s", paymentTransfer.getId());

        // let know about this payment with an event
        EventManager.process(new PaymentDeletedEvent(dto.getBaseUser().getEntity().getId(), dto));

        return paymentTransfer == null ? null : paymentTransfer.getId();
    }

    public PaymentTransferDTO getLastPaymentTransferByPaymentId(Integer paymentId) {
        return paymentTransferDas.getLastPaymentTransferByPaymentId(paymentId);
    }

    public List<Integer> getAllPaymentTransfersByDateRange(Integer entityId, Date fromDate, Date toDate) {
        return paymentTransferDas.getAllPaymentTransfersByDateRange(entityId, fromDate, toDate);
    }

    public List<PaymentTransferWS> getAllPaymentTransfersByUserId(Integer userId) {
        return paymentTransferDas.getAllPaymentTransfersByUserId(userId);
    }

    /**
     * get latest payment transfer of user
     * @param userId
     * @return
     * @throws SQLException
     */
    public Integer getLatestPaymentTransfer(Integer userId)
            throws SessionInternalError {
        Integer retValue = null;
        try {
            prepareStatement(PaymentSQL.getLatestPaymentTransfer);
            cachedResults.setInt(1, userId.intValue());
            execute();
            if (cachedResults.next()) {
                int value = cachedResults.getInt(1);
                if (!cachedResults.wasNull()) {
                    retValue = new Integer(value);
                }
            }
            cachedResults.close();
            conn.close();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    /**
     * get WS of payment transfer
     * @param paymentTransfer
     * @return
     */
    public PaymentTransferWS getWS(PaymentTransferDTO paymentTransfer) {
        if (paymentTransfer == null) {
            return null;
        }
        PaymentTransferWS retValue = new PaymentTransferWS();
        retValue.setId(paymentTransfer.getId());
        retValue.setCreateDatetime(paymentTransfer.getCreateDatetime());
        retValue.setFromUserId(paymentTransfer.getFromUserId());
        retValue.setFromUserId(paymentTransfer.getToUserId());
        retValue.setPaymentId(paymentTransfer.getPayment().getId());
        retValue.setCreatedBy(paymentTransfer.getCreatedBy());
        retValue.setDeleted(paymentTransfer.getDeleted());

        return retValue;
    }

    public PaymentTransferDTO getPaymentTransferDTO() {
        return paymentTransfer;
    }

    public void setPaymentTransferDTO(PaymentTransferDTO paymentTransfer) {
        this.paymentTransfer = paymentTransfer;
    }

    public void setPaymentTransferDTO(int paymentTransferId) {
        this.paymentTransfer = paymentTransferDas.find(paymentTransferId);
    }

}

