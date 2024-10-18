/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.crm.task;

import static com.sapienter.jbilling.client.crm.CRMConstants.*;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.crm.model.CRMInvoiceTerm;
import com.sapienter.jbilling.server.crm.model.CRMPaymentMethod;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import com.sapienter.jbilling.client.crm.CRMAPIClient;
import com.sapienter.jbilling.client.crm.CRMHelperService;
import com.sapienter.jbilling.server.crm.model.CRMDeleteInvoice;
import com.sapienter.jbilling.server.crm.model.CRMInvoice;
import com.sapienter.jbilling.server.crm.model.CRMInvoiceItem;
import com.sapienter.jbilling.server.crm.model.CRMPayment;
import com.sapienter.jbilling.server.crm.model.CRMResponse;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.payment.event.PaymentDeletedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUnlinkedFromInvoiceEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import io.jsonwebtoken.lang.Collections;
import com.sapienter.jbilling.server.crm.model.CRMPaymentApplication;

public class CRMIntegrationTask extends PluggableTask implements IInternalEventsTask {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String CRM_CLIENT_NAME = "crmApiClient";
	private static final String CRM_HELPER_SERVICE_NAME = "crmHelperService";

	public static final ParameterDescription PARAMETER_BASE_URL =
		new ParameterDescription(BASE_URL, true, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_CRM_CLIENT_ID =
		new ParameterDescription(CRM_CLIENT_ID, true, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_CRM_CLIENT_SECRET =
		new ParameterDescription(CRM_CLIENT_SECRET, false, ParameterDescription.Type.STR, true);
	public static final ParameterDescription PARAMETER_CRM_PLAN_PREFIX =
		new ParameterDescription(CRM_PLAN_PREFIX, false, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_CRM_ORGANIZATION_NAME =
		new ParameterDescription(CRM_ORG_NAME, false, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_CRM_ADD_TAX_TO_INVOICE =
		new ParameterDescription(CRM_ADD_TAX_TO_INVOICE, false, ParameterDescription.Type.BOOLEAN);
	{
		descriptions.add(PARAMETER_BASE_URL);
		descriptions.add(PARAMETER_CRM_CLIENT_ID);
		descriptions.add(PARAMETER_CRM_CLIENT_SECRET);
		descriptions.add(PARAMETER_CRM_PLAN_PREFIX);
		descriptions.add(PARAMETER_CRM_ORGANIZATION_NAME);
		descriptions.add(PARAMETER_CRM_ADD_TAX_TO_INVOICE);
	}

    private static final String GET_ORDER_PATH = "/services/findOrders";
	private static final String SERVICES_CREATE_INVOICE = "/services/createInvoice";
	private static final String SERVICES_CREATE_INVOICE_ITEM = "/services/createInvoiceItem";
	private static final String SERVICES_GET_INVOICE = "/services/getInvoice";
	private static final String SERVICES_REMOVE_INVOICE_ITEM = "/services/removeInvoiceItem";
	private static final String SERVICES_CREATE_PAYMENT = "/services/createPayment";
	private static final String SERVICES_CREATE_PAYMENT_AND_APPLICATION = "/services/createPaymentAndApplication";
	private static final String SERVICES_CREATE_PAYMENT_APPLICATION = "/services/createPaymentApplication";
	private static final String SERVICES_ADD_TAX_TO_INVOICE = "/services/addtax";
	private static final String SERVICES_CREATE_INVOICE_TERM = "/services/createInvoiceTerm";

	private static final String IN_PARAMS = "inParams";

	@SuppressWarnings("unchecked")
	private static final Class<Event> events[] = new Class[]{
		InvoicesGeneratedEvent.class,
		InvoiceDeletedEvent.class,
		PaymentSuccessfulEvent.class,
		PaymentFailedEvent.class,
		PaymentDeletedEvent.class,
		PaymentLinkedToInvoiceEvent.class,
		PaymentUnlinkedFromInvoiceEvent.class
    };

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

	/**
	 * This method is responsible for providing the Base URL stored in the Configured Plugin
	 * @return CRM base url
	 */
	public String getBaseUrl() {
        return parameters.get(PARAMETER_BASE_URL.getName());
	}
	public String getPlanPrefix() {
		String pluginPlanPrefix = parameters.get(PARAMETER_CRM_PLAN_PREFIX.getName());
        return StringUtils.isBlank(pluginPlanPrefix) ? DEFAULT_PLAN_PREFIX : pluginPlanPrefix;
	}
	public String getCrmOrgName() {
		String pluginOrgName = parameters.get(PARAMETER_CRM_ORGANIZATION_NAME.getName());
        return StringUtils.isBlank(pluginOrgName) ? DEFAULT_COMPANY_NAME : pluginOrgName;
	}

	@Override
	public void process(Event event) throws PluggableTaskException {
		logger.debug("Entering CRMIntegrationTask process - event: {}", event);
		if (event instanceof InvoiceDeletedEvent) {
			InvoiceDeletedEvent instantiatedEvent = (InvoiceDeletedEvent) event;
			Integer entityId = instantiatedEvent.getEntityId();
			InvoiceDTO invoice = instantiatedEvent.getInvoice();
			deleteInvoiceInCRM(invoice);
			logger.debug("CRMIntegrationTask.process-- Invoice Deleted with invoiceId: {} entityId: {}", invoice.getId(), entityId);
		} else if (event instanceof InvoicesGeneratedEvent) {
			InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;
			Integer entityId = instantiatedEvent.getEntityId();
			if(null!= instantiatedEvent.getBillingProcessId() &&
				isReviewRun(instantiatedEvent.getBillingProcessId())) {
				logger.debug("skipping invoice push to crm for process {} review for entity {}", instantiatedEvent.getBillingProcessId(),
					entityId);
				return ;
			}
			List<Integer> invoiceIds = instantiatedEvent.getInvoiceIds();
			if(!Collections.isEmpty(invoiceIds)) {
				invoiceIds.stream().forEach(invoiceId -> {
					InvoiceDTO invoiceDTO = new InvoiceDAS().findNow(invoiceId);
					createInvoiceInCRM(invoiceDTO, entityId);
				});
			}
		} else if( event instanceof PaymentSuccessfulEvent ) {
			PaymentSuccessfulEvent instantiatedEvent = (PaymentSuccessfulEvent) event;
            createAndLinkCRMPayment(instantiatedEvent.getPayment().getId(), null, instantiatedEvent.getEntityId());
		} else if( event instanceof PaymentLinkedToInvoiceEvent ) {
			// TODO needs to recheck and enable it
			logger.debug("doing nothing for -- {}", event.getName());
//			PaymentLinkedToInvoiceEvent instantiatedEvent = (PaymentLinkedToInvoiceEvent) event;
//          createAndLinkCRMPayment(instantiatedEvent.getPayment().getId(), instantiatedEvent.getInvoice(), instantiatedEvent.getEntityId());
		} else {
			logger.debug("no process is configured for event -- {}", event.getName());
		}
	}

	/**
	 * create invoice in EarnDesk CRM
	 *
	 * @param invoiceDTO	: Invoice DTO object
	 * @param entityId : entity Id for which the invoice has to be created
	 */
	private void createInvoiceInCRM(InvoiceDTO invoiceDTO, Integer entityId) {
		String invoiceNumber = invoiceDTO.getNumber();
		Date invoiceDate = invoiceDTO.getCreateDatetime();
		Date invoiceDueDate = invoiceDTO.getDueDate();
		String invoicePayload = CRMInvoice.builder().partyId(getPartyId(invoiceDTO.getBaseUser()))
													.partyIdFrom(getCrmOrgName())
													.invoiceId(invoiceNumber)
													.invoiceDate(getFormattedDate(invoiceDate))
													.referenceNumber(String.valueOf(invoiceDTO.getId()))
													.currencyUomId(invoiceDTO.getCurrency().getCode())
													.dueDate(getFormattedDate(invoiceDueDate))
													.build().toJson();
        String createInvoiceUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_CREATE_INVOICE).toUriString();
        String getInvoiceUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_GET_INVOICE)
            .queryParam(IN_PARAMS, invoicePayload).toUriString();
		logger.debug("createInvoiceUrl called =============== {}",createInvoiceUrl);
		logger.debug("invoicePayload called =============== {}",invoicePayload);
		CRMResponse crmInvoiceResponse = crmapiClient().exchangeForObject(getInvoiceUrl, HttpMethod.GET, null, CRMResponse.class);
		// Call CRM API
		if(crmInvoiceResponse != null) {
			if(crmInvoiceResponse.getStatusCode() != HttpStatus.OK.value()) {
				crmInvoiceResponse = crmapiClient().exchangeForObject(createInvoiceUrl, HttpMethod.POST, invoicePayload, CRMResponse.class);
			}
			Map<String, Object> data = crmInvoiceResponse.getData();
			logger.debug("response=========================={}", data);
			Set<InvoiceLineDTO> invoiceLines = invoiceDTO.getInvoiceLines();
			for (InvoiceLineDTO invoiceLineDTO : invoiceLines) {
				ItemDTO item = invoiceLineDTO.getItem();
				createInvoiceLineItem(invoiceLineDTO, item, invoiceNumber);
			}
			if(null!= data) {
				addTaxToCRMInvoice(invoiceNumber);
			}
			int dueInDays = getDaysBetween(invoiceDate, invoiceDueDate);
			if(dueInDays > 0) {
				addInvoiceTerms(invoiceNumber, dueInDays, FIN_PAYMENT_TERM);
			}
		}
	}

	/**
	 * Helper method to get date in 'yyyy-MM-dd' format
	 *
	 * @param dateToFormat date to format
	 * @return formatted date
	 */
	private String getFormattedDate(Date dateToFormat) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(dateToFormat.toInstant(), java.util.TimeZone.getDefault().toZoneId());
		return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * Helper method to find days in between the provided dates
	 * @param startDate from date
	 * @param endDate to date
	 * @return days in between
	 */
	private int getDaysBetween(Date startDate, Date endDate) {
		LocalDateTime localStartDate = LocalDateTime.ofInstant(startDate.toInstant(), java.util.TimeZone.getDefault().toZoneId());
		LocalDateTime localEndDate = LocalDateTime.ofInstant(endDate.toInstant(), java.util.TimeZone.getDefault().toZoneId());
		Period period = Period.between(localStartDate.toLocalDate(), localEndDate.toLocalDate());
		return period.getDays();
	}

	/**
	 * Helper method to addTax to CRM Invoice if Parameter ADD TAX to Invoice is True
	 * @param invoiceNumber Add Tax to this invoice number
	 */
	private void addTaxToCRMInvoice(String invoiceNumber) {
		String param = parameters.get(PARAMETER_CRM_ADD_TAX_TO_INVOICE.getName());
		if(Boolean.parseBoolean(param)) {
			String addTaxUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_ADD_TAX_TO_INVOICE).toUriString();
			String addTaxPayload = CRMInvoice.builder().invoiceId(invoiceNumber)
				.build().toJson();
			CRMResponse addTaxResponse = crmapiClient().exchangeForObject(addTaxUrl, HttpMethod.POST, addTaxPayload, CRMResponse.class);
			if(addTaxResponse != null) {
				Map<String, Object> addTaxResponseData = addTaxResponse.getData();
				if(addTaxResponseData != null) {
					logger.debug("received invoice add tax response {}", addTaxResponseData);
				}
			}
		}
	}
	/**
	 * Helper method to add invoice terms to CRM Invoice
	 * @param invoiceNumber Add invoice terms to this invoice number
	 * @param termDays number of days in terms
	 * @param termTypeId Type of Term to be created
	 */
	private void addInvoiceTerms(String invoiceNumber, int termDays, String termTypeId) {
		String invoiceTermUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_CREATE_INVOICE_TERM).toUriString();
		String invoiceTermPayload = CRMInvoiceTerm.builder().invoiceId(invoiceNumber)
			.termDays(termDays)
			.termTypeId(termTypeId)
			.build().toJson();
		CRMResponse invoiceTermResponse = crmapiClient().exchangeForObject(invoiceTermUrl, HttpMethod.POST, invoiceTermPayload, CRMResponse.class);
		if(invoiceTermResponse != null) {
			Map<String, Object> invoiceTermResponseData = invoiceTermResponse.getData();
			if(invoiceTermResponseData != null) {
				logger.debug("created invoice term {}", invoiceTermResponseData);
			}
		}
	}

	/**
	 * Helper method to create invoice line item in CRM
	 * @param invoiceLineDTO invoice line item
	 * @param item product
	 * @param invoiceNumber CRM invoice number
	 */
	private void createInvoiceLineItem(InvoiceLineDTO invoiceLineDTO, ItemDTO item, String invoiceNumber) {
		String createInvoiceItemUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_CREATE_INVOICE_ITEM).toUriString();
		if(null != item) {
			String invoiceItemPayload = CRMInvoiceItem.builder().invoiceId(invoiceNumber)
																.productId(item.isPlan() ? getPlanPrefix()+ item.getInternalNumber() : item.getInternalNumber())
																.amount(invoiceLineDTO.getPrice())
																.quantity(invoiceLineDTO.getQuantity())
																.invoiceItemTypeId(DIGITAL_PRODUCT_ITEM_TYPE)
																.description(item.getDescription(1))
																.build().toJson();
			CRMResponse invoiceItemResponse = crmapiClient().exchangeForObject(createInvoiceItemUrl, HttpMethod.POST, invoiceItemPayload, CRMResponse.class);
			if(invoiceItemResponse != null) {
				Map<String, Object> invoiceItemResponseData = invoiceItemResponse.getData();
				if(invoiceItemResponseData != null) {
					logger.debug("received invoice item creation response {}", invoiceItemResponseData);
				}
			}
		}
	}

	/**
	 * Helper method to fetch the CRM Account ID
	 * @param baseUser billing user object
	 * @return CRM Account ID
	 */
	private static String getPartyId(UserDTO baseUser) {
		CustomerDTO customer = baseUser.getCustomer();
		MetaFieldValue<String> crmAccountId = customer.getMetaField(CRM_ACCOUNT_ID);
        return (crmAccountId != null ? crmAccountId.getValue() : DEFAULT_CUSTOMER_NAME);
	}

	private CRMAPIClient crmapiClient() {
		CRMAPIClient crmapiClient = Context.getBean(CRM_CLIENT_NAME);
		crmapiClient.setEntityId(getEntityId());
		return crmapiClient;
	}
	private CRMHelperService crmHelperService() {
		return Context.getBean(CRM_HELPER_SERVICE_NAME);
	}

	/**
	 * delete invoice in EarnDesk CRM
	 *
	 * @param invoiceDTO	: Invoice object to delete the crm invoice object
	 */
	private void deleteInvoiceInCRM(InvoiceDTO invoiceDTO) {
		logger.debug("CRMIntegrationTask.deleteInvoiceInCRM=================");
		String invoiceNumber = invoiceDTO.getNumber();
		String invoicePayload = CRMInvoice.builder().invoiceId(invoiceNumber).build().toJson();
		String getInvoiceUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_GET_INVOICE)
			.queryParam(IN_PARAMS, invoicePayload).toUriString();
		String removeInvoiceItemUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_REMOVE_INVOICE_ITEM).toUriString();

		CRMResponse crmInvoiceResponse = crmapiClient().exchangeForObject(getInvoiceUrl, HttpMethod.GET, null, CRMResponse.class);
		if(crmInvoiceResponse != null) {
			Map<String, Object> data = crmInvoiceResponse.getData();
			if(data != null) {
				List<Map<String, String>> invoiceItems = (List<Map<String, String>>) data.get("invoiceItems");
				if(!Collections.isEmpty(invoiceItems)) {
					for (Map<String, String> invoiceItem : invoiceItems) {
						String invoiceItemSeqId = invoiceItem.get("invoiceItemSeqId");
						String payload = CRMDeleteInvoice.builder().invoiceId(invoiceNumber).invoiceItemSeqId(invoiceItemSeqId).build().toJson();
						CRMResponse crmRemoveResponse = crmapiClient().exchangeForObject(removeInvoiceItemUrl, HttpMethod.DELETE, payload, CRMResponse.class);
						if (crmRemoveResponse != null) {
							logger.debug("CRMIntegrationTask.crmRemoveResponse.getData================={}", crmRemoveResponse.getData());
						}
					}
				}
			}
		}
	}

	/**
	 * Helper method to create a Payment in CRM and if invoice object is not null then link the payment and invoice in CRM
	 * @param paymentId payment id of billing payment object
	 * @param invoice billing invoice object
	 * @param entityId
	 * @return CRM Payment Id
	 */
	private String createAndLinkCRMPayment(int paymentId, InvoiceDTO invoice, Integer entityId) {
		PaymentDTO paymentDTO = new PaymentBL(paymentId).getEntity();
		Set<PaymentInvoiceMapDTO> invoicesMap = paymentDTO.getInvoicesMap();
		if (invoicesMap != null && !invoicesMap.isEmpty()) {
            invoice = invoicesMap.stream().findFirst().get().getInvoiceEntity();
		}
		String paymentInCRM = getCrmPaymentId(paymentDTO);
		boolean createPaymentInCRM = paymentInCRM == null || StringUtils.isBlank(paymentInCRM);
		if( createPaymentInCRM ) {
			paymentInCRM = createPaymentInCRM(paymentDTO, invoice);
			MetaField metaField = new MetaFieldDAS().getFieldByName(entityId, new EntityType[] { EntityType.PAYMENT }, CRM_PAYMENT_ID);
			if( metaField != null ) {
				paymentDTO.setMetaField(metaField, paymentInCRM);
			}
		}
		if(null != invoice && StringUtils.isNotBlank(paymentInCRM)) {
			linkPaymentInCRM(paymentInCRM, invoice.getNumber());
		}
		return paymentInCRM;
	}

	/**
	 * Helper method to create Payment in CRM
	 * @param payment billing payment object
	 * @param invoice billing invoice object
	 * @return CRM Payment Id
	 */
	private String createPaymentInCRM(PaymentDTO payment,InvoiceDTO invoice) {
		CRMResponse crmPaymentResponse = getCreateCrmPaymentResponse(payment, invoice);
		String crmPaymentId = null;
		if( crmPaymentResponse != null ) {
			Map<String, Object> data = crmPaymentResponse.getData();
			if( data != null ) {
				logger.debug("CRMIntegrationTask.createPaymentInCRM =================== {}", data);
				crmPaymentId = (String) data.get("paymentId");
			}
		}
		return crmPaymentId;
	}

	/**
	 * Helper method to create Payment in CRM and if Invoice object is not null then link the invoice and payment objects in CRM
	 * @param payment Billing Payment Object
	 * @param invoice Billing invoice object
	 * @return CRM Response object
	 */
	private CRMResponse getCreateCrmPaymentResponse(PaymentDTO payment, InvoiceDTO invoice) {
		logger.debug("CRMIntegrationTask.createPaymentInCRM===============");
		UserDTO baseUser = payment.getBaseUser();
		String paymentMethod = payment.getPaymentMethod().getDescription(1);
		String invoiceNumber = invoice != null ? invoice.getNumber() : null;
		String notes = payment.getPaymentNotes();
		CRMPayment.CRMPaymentBuilder crmPaymentBuilder = CRMPayment.builder().partyIdFrom(getPartyId(baseUser))
				.partyIdTo(getCrmOrgName())
				.paymentMethodId(findPaymentMethod(paymentMethod).name())
				.amount(payment.getAmount())
				.statusId(PMNT_RECEIVED);
		if (StringUtils.isNotBlank(invoiceNumber)) {
			crmPaymentBuilder.invoiceId(invoiceNumber);
		}
		if (StringUtils.isNotBlank(notes)) {
			crmPaymentBuilder.comments(StringUtils.left(notes, LENGTH_OF_NOTES));
		}
		String paymentPayload = crmPaymentBuilder.build().toJson();
		logger.debug("paymentPayload===================={}", paymentPayload);
		String createPaymentUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + (StringUtils.isBlank(invoiceNumber) ? SERVICES_CREATE_PAYMENT : SERVICES_CREATE_PAYMENT_AND_APPLICATION)).toUriString();
        return crmapiClient().exchangeForObject(createPaymentUrl, HttpMethod.POST, paymentPayload, CRMResponse.class);
	}

	/**
	 * Helper method to find the CRM Payment Method value
	 * @param methodName
	 * @return
	 */
	private CRMPaymentMethod findPaymentMethod(String methodName) {
		for(CRMPaymentMethod method : CRMPaymentMethod.values()) {
			if (method.getMethodName().equalsIgnoreCase(methodName) ) {
				return method;
			}
		}
		return CRMPaymentMethod.CASH;
	}

	/**
	 * Helper method to link the created payment and invoice in CRM
	 * @param paymentInCrm CRM Payment ID
	 * @param invoiceNumber CRM Invoice Number
	 */
	private void linkPaymentInCRM(String paymentInCrm, String invoiceNumber) {
		CRMPaymentApplication.CRMPaymentApplicationBuilder crmPaymentApplicationBuilder = CRMPaymentApplication.builder()
				.invoiceId(invoiceNumber)
				.paymentId(paymentInCrm);
		String paymentLinkPayload = crmPaymentApplicationBuilder.build().toJson();
		logger.debug("paymentPayload===================={}", paymentLinkPayload);
		String createPaymentUrl = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + SERVICES_CREATE_PAYMENT_APPLICATION).toUriString();
		CRMResponse crmPaymentResponse = crmapiClient().exchangeForObject(createPaymentUrl, HttpMethod.POST, paymentLinkPayload, CRMResponse.class);
		if( crmPaymentResponse != null ) {
			Map<String, Object> data = crmPaymentResponse.getData();
			if( data != null ) {
				logger.debug("CRMIntegrationTask.createPaymentInCRM ==================={}", data);
			}
		}
	}

	/**
	 * Helper method to fetch the CRM Payment Id metafield from billing payment object
	 * @param payment billing payment object
	 * @return CRM Payment ID from the billing Payment Object
	 */
	private String getCrmPaymentId(PaymentDTO payment) {
		MetaFieldValue<String> metaField = payment.getMetaField(CRM_PAYMENT_ID);
		return metaField != null ? metaField.getValue() : null;
	}

	/**
	 * Helper method to check  if billing process is Review Run
	 * @param billingprocessId
	 * @return
	 */
	private boolean isReviewRun(Integer billingprocessId) {
		BillingProcessDTO processDTO = new BillingProcessDAS().findNow(billingprocessId);
		return null != processDTO && processDTO.getIsReview() == 1;
	}

    @Override
    public boolean isSingleton() {
        return true;
    }

}



