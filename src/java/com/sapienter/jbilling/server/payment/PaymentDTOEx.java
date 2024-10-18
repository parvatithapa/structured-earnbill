/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.payment;

import java.io.Closeable;
import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

import java.util.ArrayList;

public class PaymentDTOEx extends PaymentDTO implements AutoCloseable {

    private Integer userId = null;
    private String method = null;
    private List<Integer> invoiceIds = null;
    private List paymentMaps = null;
    private PaymentDTOEx payment = null; // for refunds
    private String resultStr = null;
    private Integer payoutId = null;
    
    //Changes for Strong Customer Authentication (SCA), 3DS auth
    private boolean authenticationRequired;
    // now we only support one of these
    private PaymentAuthorizationDTO authorization = null; // useful in refuds
    // instruments using which user specifc, not linked to payments
    private List<PaymentInformationDTO> paymentInstruments = new ArrayList<PaymentInformationDTO>(0);
    
    // current instrument with which this payment will be process
    private PaymentInformationDTO instrument = null;
    
    private Integer autoPayment;

    private boolean isBankPaymentApproved = false;
    private boolean sendNotification = true;

    public PaymentDTOEx(PaymentDTO dto) {
        if (dto.getBaseUser() != null)
            userId = dto.getBaseUser().getId();

        setId(dto.getId());
        setCurrency(dto.getCurrency());
        setAmount(dto.getAmount());
        setBalance(dto.getBalance());
        setAttempt(dto.getAttempt());
        setCreditCard(dto.getCreditCard());
        setInstrument(dto.getCreditCard());
        
        setDeleted(dto.getDeleted());
        setIsPreauth(dto.getIsPreauth());
        setIsRefund(dto.getIsRefund());

        setPaymentDate(dto.getPaymentDate());
        setCreateDatetime(dto.getCreateDatetime());
        setUpdateDatetime(dto.getUpdateDatetime());

        if (dto.getPaymentMethod() != null) {
            setPaymentMethod(dto.getPaymentMethod());
        }

        if (dto.getPaymentResult() != null) {
            setPaymentResult(dto.getPaymentResult());
        }
        setPaymentPeriod(dto.getPaymentPeriod());
        setPaymentNotes(dto.getPaymentNotes());
        setMetaFields(dto.getMetaFields());
        
        //for refunds
        setPayment(dto.getPayment());
        
        // payment instruments
        if(dto.getPaymentInstrumentsInfo() != null) {
        	setPaymentInstrumentsInfo(dto.getPaymentInstrumentsInfo());
        }
        
        invoiceIds = new ArrayList<Integer>();
        paymentMaps = new ArrayList();
    }

    public PaymentDTOEx(PaymentWS dto) {

        setId(dto.getId());
        setAmount(dto.getAmountAsDecimal());
        setAttempt(dto.getAttempt());
        setBalance(dto.getBalanceAsDecimal());
        setCreateDatetime(dto.getCreateDatetime());
        setCurrency(new CurrencyDTO(dto.getCurrencyId()));
        setDeleted(dto.getDeleted());
        setIsPreauth(dto.getIsPreauth());
        setIsRefund(dto.getIsRefund());
        setPaymentDate(dto.getPaymentDate());
        setUpdateDatetime(dto.getUpdateDatetime());
        setPaymentPeriod(dto.getPaymentPeriod());
        setPaymentNotes(dto.getPaymentNotes());
        setSendNotification(dto.isSendNotification());

        if (dto.getMethodId() != null)
            setPaymentMethod(new PaymentMethodDTO(dto.getMethodId()));

        if (dto.getResultId() != null)
            setPaymentResult(new PaymentResultDAS().find(dto.getResultId()));

        userId = dto.getUserId();

        method = dto.getMethod();

        Integer entityId = new UserBL().getEntityId(userId);
        // set payment instruments
        try {
            if(null!=dto.getPaymentInstruments() && !dto.getPaymentInstruments().isEmpty()) {
            	for(PaymentInformationWS paymentInstrument : dto.getPaymentInstruments()) {
            		this.getPaymentInstruments().add(new PaymentInformationDTO(paymentInstrument, entityId));
            	}
            }
        }catch (Exception exception){
            new FormatLogger(Logger.getLogger(PaymentDTOEx.class)).debug("Exception: "+exception);
        }
        
        invoiceIds = new ArrayList<Integer>();
        paymentMaps = new ArrayList();

        if (dto.getInvoiceIds() != null) {
            for (int f = 0; f < dto.getInvoiceIds().length; f++) {
                invoiceIds.add(dto.getInvoiceIds()[f]);
            }
        }

        if (dto.getPaymentId() != null) {
            payment = new PaymentDTOEx();
            payment.setId(dto.getPaymentId());
        } else {
            payment = null;
        }
        autoPayment = dto.getAutoPayment();
        authorization = new PaymentAuthorizationDAS().find(dto.getAuthorizationId());
        MetaFieldBL.fillMetaFieldsFromWS(entityId,
        		this, dto.getMetaFields());
    }
    
    /**
     *
     */
    public PaymentDTOEx() {
        super();
        invoiceIds = new ArrayList<Integer>();
        paymentMaps = new ArrayList();
    }

//    /**
//     * @param id
//     * @param amount
//     * @param createDateTime
//     * @param attempt
//     * @param deleted
//     * @param methodId
//     */
//    public PaymentDTOEx(Integer id, BigDecimal amount, Date createDateTime,
//            Date updateDateTime,
//            Date paymentDate, Integer attempt, Integer deleted,
//            Integer methodId, Integer resultId, Integer isRefund,
//            Integer isPreauth, Integer currencyId, BigDecimal balance) {
//        super(id, amount, balance, createDateTime, updateDateTime,
//                paymentDate, attempt, deleted, methodId, resultId, isRefund,
//                isPreauth, currencyId, null, null);
//        invoiceIds = new ArrayList<Integer>();
//        paymentMaps = new ArrayList();
//    }

//    /**
//     * @param otherValue
//     */
//    public PaymentDTOEx(PaymentDTO otherValue) {
//        super(otherValue);
//        invoiceIds = new ArrayList<Integer>();
//        paymentMaps = new ArrayList();
//    }

    public boolean validate() {
        boolean retValue = true;

        // check some mandatory fields
        if (getPaymentMethod() == null || getPaymentResult() == null) {
            retValue = false;
        }

        return retValue;
    }
    
    public String toString() {

        StringBuffer maps = new StringBuffer();
        if (paymentMaps != null) {
            for (int f = 0; f < paymentMaps.size(); f++) {
                maps.append(paymentMaps.get(f).toString());
                maps.append(" - ");
            }
        }

        return super.toString() + " payment maps:" + maps.toString() + "payment for refund "+ payment;
    }
    /**
     * @return
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * @param integer
     */
    public void setUserId(Integer integer) {
        userId = integer;
    }

    /**
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param string
     */
    public void setMethod(String string) {
        method = string;
    }


    /**
     * @return
     */
    public List<Integer> getInvoiceIds() {
        return invoiceIds;
    }

    /**
     * @param vector
     */
    public void setInvoiceIds(List vector) {
        invoiceIds = vector;
    }

    /**
     * @return
     */
    public PaymentDTOEx getPayment() {
        return payment;
    }

    /**
     * @param ex
     */
    public void setPayment(PaymentDTOEx ex) {
        payment = ex;
    }

    /**
     * @return
     */
    public PaymentAuthorizationDTO getAuthorization() {
        new FormatLogger(Logger.getLogger(PaymentDTOEx.class)).debug("Returning " +
                authorization + " for payemnt " + getId());
        return authorization;
    }

    /**
     * @param authorizationDTO
     */
    public void setAuthorization(PaymentAuthorizationDTO authorizationDTO) {
        authorization = authorizationDTO;
    }

    /**
     * @return
     */
    public String getResultStr() {
        return resultStr;
    }

    /**
     * @param resultStr
     */
    public void setResultStr(String resultStr) {
        this.resultStr = resultStr;
    }

    /**
     * @return
     */
    public Integer getPayoutId() {
        return payoutId;
    }

    /**
     * @param payoutId
     */
    public void setPayoutId(Integer payoutId) {
        this.payoutId = payoutId;
    }

    public List getPaymentMaps() {
        new FormatLogger(Logger.getLogger(PaymentDTOEx.class)).debug("Returning " +
                paymentMaps.size() + " elements in the map");
        return paymentMaps;
    }

    public void addPaymentMap(PaymentInvoiceMapDTOEx map) {
        new FormatLogger(Logger.getLogger(PaymentDTOEx.class)).debug("Adding map to the vector ");
        paymentMaps.add(map);
    }

	public List<PaymentInformationDTO> getPaymentInstruments() {
		return paymentInstruments;
	}

	public void setPaymentInstruments(List<PaymentInformationDTO> paymentInstruments) {
		this.paymentInstruments = paymentInstruments;
	}

	public PaymentInformationDTO getInstrument() {
		return instrument;
	}

	public void setInstrument(PaymentInformationDTO instrument) {
		this.instrument = instrument;
	}

	public Integer getAutoPayment() {
		return autoPayment;
	}

	public void setAutoPayment(Integer autoPayment) {
		this.autoPayment = autoPayment;
	}

	public boolean getIsBankPaymentApproved() {
        return isBankPaymentApproved;
    }

    public void setIsBankPaymentApproved(boolean isBankPaymentApproved) {
        this.isBankPaymentApproved = isBankPaymentApproved;
    }

    public boolean isSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }
	
	public boolean isAuthenticationRequired() {
		return authenticationRequired;
	}

	public void setAuthenticationRequired(boolean authenticationRequired) {
		this.authenticationRequired = authenticationRequired;
	}

	/**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {
        // Close PaymentMethodDTO object
        if (null != instrument) {
            instrument.close();
        }

        // Close objects from the list
        for(PaymentInformationDTO paymentInformationDTO: paymentInstruments){
            paymentInformationDTO.close();
        }
    }
}
