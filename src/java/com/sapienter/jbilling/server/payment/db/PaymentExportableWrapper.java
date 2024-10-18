package com.sapienter.jbilling.server.payment.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.csv.DynamicExport;
import com.sapienter.jbilling.server.util.csv.ExportableWrapper;

/**
 *
 * @author Harshad Pathan
 * @since  25/07/2017
 */
public class PaymentExportableWrapper implements ExportableWrapper<PaymentDTO> {

	private static final Object[] FIX_EMPTY_ARRAY = new Object[10];

	private Integer paymentId;
	private DynamicExport dynamicExport = DynamicExport.NO;

	private void init(Integer paymentId) {
		this.paymentId = paymentId;
	}

	public PaymentExportableWrapper(Integer paymentId, DynamicExport dynamicExport) {
		this.paymentId = paymentId;
		setDynamicExport(dynamicExport);

	}

	public PaymentExportableWrapper(Integer paymentId) {
		init(paymentId);
	}

	public PaymentExportableWrapper() { }

	@Override
    @Transient
    public String[] getFieldNames() {
        return new String[] {
                "id",
                "userId",
                "userName",
                "linkedInvoices",
                "paymentMethod",
                "currency",
                "amount",
                "balance",
                "isRefund",
                "isPreauth",
                "createdDate",
                "paymentDate",
                "paymentNotes",

                // payment auth
                "paymentProcessor",
                "code1",
                "code2",
                "code3",
                "approvalCode",
                "avs",
                "transactionId",
                "md5",
                "cardCode",
                "responseMessage",

                // credit card
                "cardName",
                "cardNumber",
                "cardType",
                "cardExpiry",

                // ach
                "achAccountName",
                "achBankName",
                "achAccountType",

                // cheque
                "chequeBankName",
                "chequeNumber",
                "chequeDate",
        };
    }

    @Override
    @Transient
    public Object[][] getFieldValues() {
        PaymentDTO payment = getWrappedInstance();
        String invoiceIds = null;
        Set<PaymentInvoiceMapDTO> invoiceMap = payment.getInvoicesMap();
        if(CollectionUtils.isNotEmpty(invoiceMap)) {
            invoiceIds = invoiceMap.stream()
                    .map(PaymentInvoiceMapDTO::getInvoiceEntity)
                    .map(InvoiceDTO::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        PaymentAuthorizationDTO latestAuthorization = null;
        if(CollectionUtils.isNotEmpty(payment.getPaymentAuthorizations())) {
            latestAuthorization = payment.getPaymentAuthorizations().iterator().next();
        }

		List<Object[]> payments = new ArrayList<>();
		Object[] objects = new Object[33];
		objects[0] = payment.getId();
		objects[1] = payment.getBaseUser() != null ? payment.getBaseUser().getId() : null;
		objects[2] = payment.getBaseUser() != null ? payment.getBaseUser().getUserName() : null;
		objects[3] = invoiceIds;
		objects[4] = payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDescription(1) : null;
		objects[5] = payment.getCurrency() != null ? payment.getCurrency().getDescription(1) : null;
		objects[6] = payment.getAmount();
		objects[7] = payment.getBalance();
		objects[8] = payment.getIsRefund();
		objects[9] = payment.getIsPreauth();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date createDateTime = TimezoneHelper.convertToTimezone(payment.getCreateDatetime(), payment.getBaseUser().getCompany().getTimezone());
        objects[10] = simpleDateFormat.format(createDateTime);
        Date paymentDate = TimezoneHelper.convertToTimezone(payment.getPaymentDate(), payment.getBaseUser().getCompany().getTimezone());
        if (null != paymentDate) {
            objects[11] = simpleDateFormat.format(paymentDate);
        }
		objects[12] = payment.getPaymentNotes();
		if( latestAuthorization!=null ) {
		    objects[13] = latestAuthorization.getProcessor();
	        objects[14] = latestAuthorization.getCode1();
	        objects[15] = latestAuthorization.getCode2();
	        objects[16] = latestAuthorization.getCode3();
	        objects[17] = latestAuthorization.getApprovalCode();
	        objects[18] = latestAuthorization.getAvs();
	        objects[19] = latestAuthorization.getTransactionId();
	        objects[20] = latestAuthorization.getMd5();
	        objects[21] = latestAuthorization.getCardCode();
	        objects[22] = latestAuthorization.getResponseMessage();
		}
		int pivot = 23;
		for (Object object: getPaymentInformation()) {
			objects[pivot++] = object;
		}

		payments.add(objects);
		return payments.toArray(new Object[payments.size()][]);

    }

	@Override
    public void setDynamicExport(DynamicExport dynamicExport) {
		this.dynamicExport = dynamicExport;
	}

	@Override
	public PaymentDTO getWrappedInstance() {
		return new PaymentDAS().find(paymentId);
	}

	private Object[] getPaymentInformation() {
	    PaymentDTO payment = getWrappedInstance();
		try (PaymentInformationBL piBL = new PaymentInformationBL()) {
		    if (!CollectionUtils.isEmpty(payment.getPaymentInstrumentsInfo())) {
	            PaymentInformationDTO pi = payment.getPaymentInstrumentsInfo().get(0).getPaymentInformation();
	            if (piBL.isCreditCard(pi)) {
	                PaymentMethodDTO paymentMethodDTO = new PaymentMethodDAS().find(pi.getPaymentMethodId());
	                return new Object[]{
	                        String.valueOf((char[]) getNonNullValue(piBL.getMetaField(pi, MetaFieldType.TITLE))),
	                        String.valueOf((char[]) getNonNullValue(piBL.getMetaField(pi, MetaFieldType.PAYMENT_CARD_NUMBER))),
	                        null == paymentMethodDTO ? StringUtils.EMPTY : paymentMethodDTO.getDescription(),
	                        String.valueOf((char[]) getNonNullValue(piBL.getMetaField(pi, MetaFieldType.DATE))),
	                        StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
	                        StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
	                };
	            }

	            if (piBL.isACH(pi)) {
	                return new Object[]{
	                        StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
	                        String.valueOf(getNonNullValue(piBL.getMetaField(pi, MetaFieldType.INITIAL))),
	                        getNonNullValue(piBL.getMetaField(pi, MetaFieldType.BANK_NAME)),
	                        getNonNullValue(piBL.getMetaField(pi, MetaFieldType.BANK_ACCOUNT_TYPE)),
	                        StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
	                };
	            }

	            if (piBL.isCheque(pi)) {
	                return new Object[]{
	                        StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
	                        StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
	                        getNonNullValue(piBL.getMetaField(pi, MetaFieldType.BANK_NAME)),
	                        getNonNullValue(piBL.getMetaField(pi, MetaFieldType.CHEQUE_NUMBER)),
	                        getNonNullValue(piBL.getMetaField(pi, MetaFieldType.DATE)),
	                };
	            }
	        }
		    return FIX_EMPTY_ARRAY;
		} catch (Exception e) {
		    throw new SessionInternalError(e);
        }
	}

    private Object getNonNullValue(MetaFieldValue value) {
        return null == value ? StringUtils.EMPTY :  value.getValue();
    }
}
