package com.sapienter.jbilling.einvoice.plugin;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.einvoice.domain.*;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.pricing.db.DataTableQueryDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDAS;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDTO;
import com.sapienter.jbilling.einvoice.db.Status;
import com.sapienter.jbilling.einvoice.providers.gstzen.client.GstZenClient;
import com.sapienter.jbilling.einvoice.providers.gstzen.client.GstZenClientException;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.commons.lang.StringUtils;
import static com.sapienter.jbilling.einvoice.domain.SupTyp.*;
import com.sapienter.jbilling.server.item.db.ItemDTO;

public class GstZenEInvoiceProviderPlugin extends PluggableTask implements IEInvoiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final int MILLI_TO_HOUR = 1000 * 60 * 60;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final ParameterDescription PARAM_SEND_EINVOICE_ON_PAYMENT =
			new ParameterDescription("send_einvoice_on_successful_payment", true, ParameterDescription.Type.BOOLEAN);

	private static final ParameterDescription PARAM_GST_ZEN_BASE_URL =
			new ParameterDescription("gst_zen_base_url", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_AUTH_TOKEN =
			new ParameterDescription("auth_token", true, ParameterDescription.Type.STR, true);

	private static final ParameterDescription PARAM_TIME_OUT =
			new ParameterDescription("timeout", false, ParameterDescription.Type.INT, "10000");

	private static final ParameterDescription PARAM_GSTIN =
			new ParameterDescription("gstin", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_STCD =
			new ParameterDescription("gst_state_code", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_ORG_NAME =
			new ParameterDescription("company_name", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_ORG_ADDRESS =
			new ParameterDescription("company_address", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_ORG_ADDRESS_2 =
			new ParameterDescription("company_address_2", false, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_ORG_PIN =
			new ParameterDescription("postal_code", true, ParameterDescription.Type.INT);

	private static final ParameterDescription PARAM_ORG_LOCATION =
			new ParameterDescription("company_state", true, ParameterDescription.Type.STR);

	private static final ParameterDescription DISPATCH_PARAM_NAME =
			new ParameterDescription("dispatcher_name", true, ParameterDescription.Type.STR);

	private static final ParameterDescription DISPATCH_PARAM_LOCATION =
			new ParameterDescription("dispatcher_location", true, ParameterDescription.Type.STR);

	private static final ParameterDescription DISPATCH_PARAM_ADDRESS_1 =
			new ParameterDescription("dispatcher_address_1", true, ParameterDescription.Type.STR);

	private static final ParameterDescription DISPATCH_PARAM_ADDRESS_2 =
			new ParameterDescription("dispatcher_address_2", false, ParameterDescription.Type.STR);

	private static final ParameterDescription DISPATCH_PARAM_PIN_CODE =
			new ParameterDescription("dispatcher_pin_code", true, ParameterDescription.Type.INT);

	private static final ParameterDescription DISPATCH_PARAM_STATE_CODE =
			new ParameterDescription("dispatcher_state_code", true, ParameterDescription.Type.STR);


	public GstZenEInvoiceProviderPlugin() {
		descriptions.add(PARAM_GST_ZEN_BASE_URL);
		descriptions.add(PARAM_AUTH_TOKEN);
		descriptions.add(PARAM_TIME_OUT);
		descriptions.add(PARAM_GSTIN);
		descriptions.add(PARAM_STCD);
		descriptions.add(PARAM_ORG_NAME);
		descriptions.add(PARAM_ORG_ADDRESS);
		descriptions.add(PARAM_ORG_ADDRESS_2);
		descriptions.add(PARAM_ORG_PIN);
		descriptions.add(PARAM_ORG_LOCATION);
		descriptions.add(PARAM_SEND_EINVOICE_ON_PAYMENT);
		// Dispatch detail parameters
		descriptions.add(DISPATCH_PARAM_NAME);
		descriptions.add(DISPATCH_PARAM_LOCATION);
		descriptions.add(DISPATCH_PARAM_ADDRESS_1);
		descriptions.add(DISPATCH_PARAM_ADDRESS_2);
		descriptions.add(DISPATCH_PARAM_PIN_CODE);
		descriptions.add(DISPATCH_PARAM_STATE_CODE);
	}

	private boolean sendInvoiceToEInvoicePortalOnSuccessfulPayment() throws PluggableTaskException {
		String flag = getMandatoryStringParameter(PARAM_SEND_EINVOICE_ON_PAYMENT.getName()).toLowerCase();
		if(flag.equals("t") || flag.equals("true")) {
			return true;
		} else if(flag.equals("f") || flag.equals("false")) {
			return false;
		}
		throw new PluggableTaskException(PARAM_SEND_EINVOICE_ON_PAYMENT.getName() +" has invalid paramter, "
				+ "value values will be t, true or f, false");
	}

	@Override
	public void generateEInvoice(InvoicesGeneratedEvent invoicesGeneratedEvent) throws PluggableTaskException {
		List<Integer> invoices = invoicesGeneratedEvent.getInvoiceIds();
		if(CollectionUtils.isEmpty(invoices)) {
			throw new PluggableTaskException("invoicesGeneratedEvent does not have invoices in it");
		}
		if(!findGstGroupId(invoices.get(0)).isPresent()) {
			return;
		}
		if(sendInvoiceToEInvoicePortalOnSuccessfulPayment()) {
			return;
		}
		for(Integer invoiceId : invoices) {
			sendInvoiceToEInvoicePortal(invoiceId);
		}
	}

	@Override
	public void cancelEInvoice(InvoiceDeletedEvent invoiceDeletedEvent) throws PluggableTaskException {
		EInvoiceLogDAS eInvoiceLogDAS = new EInvoiceLogDAS();
		Integer invoiceId = invoiceDeletedEvent.getInvoice().getId();
		InvoiceDTO invoice = invoiceDeletedEvent.getInvoice();
		EInvoiceLogDTO eInvoiceLogDTO = eInvoiceLogDAS.findByInvoiceId(invoiceId);
		Integer userId = invoice.getBaseUser().getId();
		if(null!= eInvoiceLogDTO) {
			try {
				if(hoursDifference(invoice.getCreateDatetime(), new Date()) > 24) {
					logger.error("can not cancel eInvoice which is created before 24 hours, invoice={} created at={}, "
							+ "so can not cancel it on gst e invoice portal", invoiceId, invoice.getCreateDatetime());
					throw new PluggableTaskException("can not delete invoice-"+ invoiceId + "from system, as 24 hours are elapsed and we "
							+ "can not delete eInvoice on gst portal which was created for the invoice");
				}
				// converted and added eInvoiceRequest payload.
				EInvoiceRequest eInvoiceRequest = OBJECT_MAPPER.readValue(eInvoiceLogDTO
						.geteInvoiceRequestpayload(), EInvoiceRequest.class);
				String url = getMandatoryStringParameter(PARAM_GST_ZEN_BASE_URL.getName());
				String authToken = getMandatoryStringParameter(PARAM_AUTH_TOKEN.getName());
				int timeout = getParameter(PARAM_TIME_OUT.getName(), Integer.parseInt(PARAM_TIME_OUT.getDefaultValue()));
				GstZenClient gstZenClient = new GstZenClient(url, authToken, timeout);
				gstZenClient.cancelEInvoice(eInvoiceRequest);
				eInvoiceLogDTO.setStatus(Status.CANCELED_AND_INVOICE_DELETED_ON_SYSTEM);
			} catch (IOException e) {
				logger.error("payload creation failed for user={}", userId, e);
				throw new PluggableTaskException("payload creation failed, for user "+ userId, e);
			} catch(GstZenClientException gstZenClientException) {
				logger.error("eInvoice cancelation failed for user={}, invoiceId={}", userId, invoiceId, gstZenClientException);
				throw new PluggableTaskException("eInvoice cancelation failed for user "+ userId + " ,"+ gstZenClientException.getLocalizedMessage());
			}
		}
	}

	@Override
	public void generateEInvoiceOnSuccessfulPayment(PaymentLinkedToInvoiceEvent paymentLinkedToInvoiceEvent) throws PluggableTaskException {
		Integer invoiceId = paymentLinkedToInvoiceEvent.getInvoice().getId();
		if(!findGstGroupId(invoiceId).isPresent()) {
			return;
		}
		EInvoiceLogDTO eInvoiceLog = new EInvoiceLogDAS().findByInvoiceId(invoiceId);
		if(null!= eInvoiceLog) {
			logger.debug("eInvoice for invoiceId={}, already generated with IRN={}", invoiceId, eInvoiceLog.getIrn());
			return;
		}
		if(paymentLinkedToInvoiceEvent.getInvoice().getBalance().compareTo(BigDecimal.ZERO)!=0) {
			return;
		}
		sendInvoiceToEInvoicePortal(invoiceId);
	}

	@Override
	public String sendInvoiceToEInvoicePortal(Integer invoiceId) throws PluggableTaskException {
		InvoiceDAS invoiceDAS = new InvoiceDAS();
		String url = getMandatoryStringParameter(PARAM_GST_ZEN_BASE_URL.getName());
		String authToken = getMandatoryStringParameter(PARAM_AUTH_TOKEN.getName());
		int timeout = getParameter(PARAM_TIME_OUT.getName(), Integer.parseInt(PARAM_TIME_OUT.getDefaultValue()));
		GstZenClient gstZenClient = new GstZenClient(url, authToken, timeout);
		// creating eInvoice payload.
		EInvoiceRequest eInvoiceRequest = new EInvoiceRequest();
		eInvoiceRequest.setTranDtls(new TranDtls());
		// creating doc info
		DocDtls docDtls = new DocDtls();
		InvoiceDTO invoiceDTO = invoiceDAS.findNow(invoiceId);
		String invoicedDate= (invoiceDTO.getCreateDatetime().toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDate().format(DATE_TIME_FORMATTER));
		docDtls.setDt(invoicedDate);
		if(StringUtils.isNotBlank(invoiceDTO.getPublicNumber())){
			docDtls.setNo(invoiceDTO.getPublicNumber());
		}

		docDtls.setTyp(Typ.INV);
		eInvoiceRequest.setDocDtls(docDtls);

		InvoiceDTO invoice = invoiceDAS.find(invoiceId);
		// creating seller details payload
		SellerDtls sellerDtls = constructSellerDtlsFromPluginParam();
		eInvoiceRequest.setSellerDtls(sellerDtls);
		// creating buyer details payload.
		BuyerDtls buyerDtls = constructBuyerDtlsFromUser(invoiceId, invoice.getBaseUser());
		eInvoiceRequest.setBuyerDtls(buyerDtls);
		// creating dispatcher details payload.
		DispDtls dispDtls = constructDispatchDtlsFromPluginParam();
		eInvoiceRequest.setDispDtls(dispDtls);
		// creating export detail payload.
		ExpDtls expDtls = new ExpDtls();
		expDtls.setCntCode("US");
		eInvoiceRequest.setExpDtls(expDtls);
		// creating e invoice item list from invoice line.
		List<ItemDtls> items = createItemsFromInvoice(invoice, buyerDtls, sellerDtls);
		eInvoiceRequest.setItemList(items);
		// creating value details from item list.
		BigDecimal totalLineAmount = BigDecimal.ZERO;
		BigDecimal totalCgst = BigDecimal.ZERO;
		BigDecimal totalSgst = BigDecimal.ZERO;
		BigDecimal totalIgst = BigDecimal.ZERO;

		for(ItemDtls itemDtls : items) {
			totalLineAmount = totalLineAmount.add(itemDtls.getTotAmt(), MathContext.DECIMAL128);
			totalCgst = totalCgst.add(itemDtls.getCgstAmt(), MathContext.DECIMAL128);
			totalSgst = totalSgst.add(itemDtls.getSgstAmt(), MathContext.DECIMAL128);
			totalIgst = totalIgst.add(itemDtls.getIgstAmt(), MathContext.DECIMAL128);
		}

		BigDecimal totalAmount = totalLineAmount.add(totalCgst, MathContext.DECIMAL128)
				.add(totalSgst, MathContext.DECIMAL128)
				.add(totalIgst, MathContext.DECIMAL128);

		ValDtls valDtls = new ValDtls();

		valDtls.setAssVal(totalLineAmount);
		valDtls.setCgstVal(totalCgst);
		valDtls.setSgstVal(totalSgst);
		valDtls.setIgstVal(totalIgst);
		valDtls.setTotInvVal(totalAmount);
		eInvoiceRequest.setValDtls(valDtls);
		Integer userId = invoice.getBaseUser().getId();
		EInvoiceLogDTO eInvoiceLogDTO = new EInvoiceLogDTO();
		eInvoiceLogDTO.setInvoiceId(invoiceId);

		// creating AddlDocDtls detail payload.
		List<AddlDocDtls> addlDocDtlsList = getARN(invoice.getBaseUser());
		if (addlDocDtlsList != null)
			eInvoiceRequest.setAddlDocDtlsList(addlDocDtlsList);

		try {
			// converted and added eInvoiceRequest payload.
			eInvoiceLogDTO.seteInvoiceRequestpayload(OBJECT_MAPPER.writeValueAsString(eInvoiceRequest));
			logger.debug("Payload before calling the GSTZen api {}", eInvoiceLogDTO.geteInvoiceRequestpayload());
		} catch (JsonProcessingException e) {
			logger.error("payload creation failed for user={}", invoice.getBaseUser().getId(), e);
			throw new PluggableTaskException("payload creation failed, for user "+ userId, e);
		}
		EInvoiceResponse eInvoiceResponse = null;
		try {
			eInvoiceResponse = gstZenClient.createEInvoice(eInvoiceRequest);
			logger.debug("eInvoice created on port for user={}, invoiceId={}, irn={}", userId, invoiceId, eInvoiceResponse.getIrn());
			// converted and added eInvoiceResponse payload.
			eInvoiceLogDTO.setStatus(Status.SUCCESSFUL);
			eInvoiceLogDTO.seteInvoiceResponse(OBJECT_MAPPER.writeValueAsString(eInvoiceResponse));
			eInvoiceLogDTO.setIrn(eInvoiceResponse.getIrn());
		} catch(GstZenClientException gstZenClientException) {
			logger.error("eInvoice upload failed for user={}, invoiceId={}", userId, invoiceId, gstZenClientException);
			throw new PluggableTaskException("eInvoice upload failed for user "+ userId + " ,"+ gstZenClientException.getLocalizedMessage());
		} catch (JsonProcessingException e) {
			// ignore failure as invoice created on portal.
			if(null!= eInvoiceResponse) {
				String[] tokens = splitToken(eInvoiceResponse.getSignedInvoice());
				eInvoiceLogDTO.seteInvoiceResponse(new String(Base64.getUrlDecoder()
						.decode(tokens[1].getBytes())).replaceAll("\\\\",""));
			}
		}
		EInvoiceLogDAS eInvoiceLogDAS = new EInvoiceLogDAS();
		eInvoiceLogDAS.save(eInvoiceLogDTO);
		return eInvoiceLogDTO.geteInvoiceResponse();
	}

	private List<ItemDtls> createItemsFromInvoice(InvoiceDTO invoice, BuyerDtls buyerDtls, SellerDtls sellerDtls) {
		List<ItemDtls> items = new ArrayList<>();
		TranDtls tranDtls = new TranDtls();
		if (tranDtls.getSupTyp() == EXPWOP || tranDtls.getSupTyp() == EXPWP) {
			
			UserDTO user = invoice.getBaseUser();
			Integer entityId = user.getEntity().getId();
			BigDecimal totAmount = invoice.getInvoiceLines().stream()
					.map(InvoiceLineDTO::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			Optional<CurrencyDTO> optional = Optional.ofNullable(new CurrencyDAS().findCurrencyByCode("INR"));
			Integer iNRID = optional.orElseThrow(() -> new SessionInternalError("NullPointerException ",
					new String[]{"Add Indian currency"})).getId();
			totAmount = new CurrencyBL().convert(user.getCurrencyId(), iNRID, totAmount, new Date(), entityId);
			// Round to the nearest whole number
			BigDecimal roundedWholeNumber = totAmount.setScale(0, RoundingMode.HALF_UP);
			// Set the scale to 2 decimal places
			totAmount = roundedWholeNumber.setScale(2, RoundingMode.UNNECESSARY);
			ItemDtls itemDtls = new ItemDtls();
			String tableName = "route_" + entityId + "_sac_code";
			boolean flag = false;
			Set<OrderProcessDTO> orderProcesses = invoice.getOrderProcesses();
			Optional<String> hsnCdOptional = orderProcesses.stream()
					.map(OrderProcessDTO::getPurchaseOrder)
					.filter(orderDTO -> orderDTO != null)
					.flatMap(orderDTO -> orderDTO.getMetaFields().stream())
					.filter(value -> ("SAC 1".equals(value.getFieldName()) || "SAC 2".equals(value.getFieldName()) || "SAC 3".equals(value.getFieldName())) && !StringUtils.isBlank((String) value.getValue()))
					.map(value -> (String) value.getValue())
					.findFirst();

			hsnCdOptional.ifPresent(itemDtls::setHsnCd);
			itemDtls.setPrdDesc(itemDtls.getHsnCd().concat("-"+new DataTableQueryDAS().getColumnValueBySacCode(tableName, itemDtls.getHsnCd(),"product")));
			itemDtls.setQty(BigDecimal.ZERO);
			itemDtls.setUnitPrice(BigDecimal.ZERO);
			itemDtls.setTotAmt(totAmount);
			itemDtls.setAssAmt(totAmount);
			itemDtls.setGstRt(BigDecimal.ZERO);
			if (buyerDtls.getStcd().equals(sellerDtls.getStcd())) {
				itemDtls.setCgstAmt(BigDecimal.ZERO);
				itemDtls.setSgstAmt(BigDecimal.ZERO);
			} else {
				itemDtls.setIgstAmt(BigDecimal.ZERO);
			}
			itemDtls.setTotItemVal(totAmount);
			items.add(itemDtls);

		}else {
			for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {

				if (null == invoiceLine.getItem()) {
					continue;
				}

				if (invoiceLine.getTaxAmount().compareTo(BigDecimal.ZERO) != 1) {
					invoiceLine.setTaxAmount(BigDecimal.ZERO);
				}
				ItemDtls itemDtls = new ItemDtls();
				itemDtls.setPrdDesc(invoiceLine.getDescription());
				itemDtls.setHsnCd(invoiceLine.getItem().getInternalNumber());

				List<MetaFieldValue> values = invoiceLine.getItem().getMetaFields();
				for (MetaFieldValue value : values) {
					if (value.getFieldName().equals(Constants.HSN_SAC_CODE) && !StringUtils.isBlank((String) value.getValue()))
						itemDtls.setHsnCd((String) value.getValue());
				}

				itemDtls.setQty(invoiceLine.getQuantity());
				itemDtls.setUnitPrice(invoiceLine.getPrice());
				itemDtls.setTotAmt(invoiceLine.getGrossAmount());
				itemDtls.setAssAmt(invoiceLine.getGrossAmount());
				itemDtls.setGstRt(invoiceLine.getTaxRate());
				if (buyerDtls.getStcd().equals(sellerDtls.getStcd())) {
					BigDecimal taxAmount = invoiceLine.getTaxAmount().divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);
					itemDtls.setCgstAmt(taxAmount);
					itemDtls.setSgstAmt(taxAmount);
				} else {
					itemDtls.setIgstAmt(invoiceLine.getTaxAmount());
				}
				itemDtls.setTotItemVal(invoiceLine.getAmount());
				items.add(itemDtls);
			}
		}
		return items;
	}

	/**
	 * Creating seller details payload
	 * @return
	 * @throws PluggableTaskException
	 */
	private SellerDtls constructSellerDtlsFromPluginParam() throws PluggableTaskException {
		SellerDtls sellerDtls = new SellerDtls();
		sellerDtls.setGstin(getMandatoryStringParameter(PARAM_GSTIN.getName()));
		sellerDtls.setAddr1(getMandatoryStringParameter(PARAM_ORG_ADDRESS.getName()));
		sellerDtls.setAddr2(getParameter(PARAM_ORG_ADDRESS_2.getName(), PARAM_ORG_ADDRESS_2.getDefaultValue()));
		sellerDtls.setLoc(getMandatoryStringParameter(PARAM_ORG_LOCATION.getName()));
		sellerDtls.setLglNm(getMandatoryStringParameter(PARAM_ORG_NAME.getName()));
		sellerDtls.setStcd(getMandatoryStringParameter(PARAM_STCD.getName()));
		sellerDtls.setPin(Integer.parseInt(getMandatoryStringParameter(PARAM_ORG_PIN.getName())));
		return sellerDtls;
	}

	private BuyerDtls constructBuyerDtlsFromUser(Integer invoiceId, UserDTO user) throws PluggableTaskException {
		Integer gstSectionId = findGstGroupId(invoiceId).orElseThrow(() -> new PluggableTaskException("GST section not found for user "+ user.getId()));
		@SuppressWarnings("rawtypes")
		List<MetaFieldValue> gstSectionDetails = new ArrayList<>(user.getCustomer()
				.getAitTimelineMetaFieldsMap()
				.get(gstSectionId)
				.values())
				.get(0);
		Integer userId = user.getId();
		BuyerDtls buyerDtls = new BuyerDtls();
		buyerDtls.setGstin((String) findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.GSTIN).getValue());
		buyerDtls.setAddr1((String) findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.ADDRESS1).getValue());
		buyerDtls.setAddr2((String) findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.ADDRESS2).getValue());
		buyerDtls.setLoc((String) findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.STATE_PROVINCE).getValue());
		buyerDtls.setLglNm((String) findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.ORGANIZATION).getValue());
		buyerDtls.setStcd((String) findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.GST_STATE_CODE).getValue());
		buyerDtls.setPin(Integer.parseInt((String)findMetaFieldValueByType(userId, gstSectionDetails, MetaFieldType.POSTAL_CODE).getValue()));
		buyerDtls.setPos(buyerDtls.getStcd());
		return buyerDtls;
	}

	@SuppressWarnings("rawtypes")
	private MetaFieldValue findMetaFieldValueByType(Integer userId, List<MetaFieldValue> metaFieldValues, MetaFieldType metaFieldType) throws PluggableTaskException {
		for(MetaFieldValue metaFieldValue :metaFieldValues) {
			if(metaFieldValue.getField().getFieldUsage().equals(metaFieldType)) {
				return metaFieldValue;
			}
		}
		throw new PluggableTaskException(metaFieldType.name() +" not configured in gst section for user "+ userId);
	}

	private Optional<Integer> findGstGroupId(Integer invoiceId) {
		UserDTO user = new InvoiceDAS().find(invoiceId).getBaseUser();
		Set<CustomerAccountInfoTypeMetaField>  customerAitFields = user.getCustomer().getCustomerAccountInfoTypeMetaFields();
		if(CollectionUtils.isEmpty(customerAitFields)) {
			return Optional.empty();
		}
		for(CustomerAccountInfoTypeMetaField aitField : customerAitFields) {
			if(MetaFieldType.GSTIN.equals(aitField.getMetaFieldValue().getField().getFieldUsage())){
				return Optional.of(aitField.getAccountInfoType().getId());
			}
		}
		logger.info("user={} does not has gst enabled account type", user.getId());
		return Optional.empty();
	}

	private static String[] splitToken(String token) throws PluggableTaskException {
		String[] parts = token.split("\\.");
		if (parts.length == 2 && token.endsWith(".")) {
			parts = new String[]{parts[0], parts[1], ""};
		}
		if (parts.length != 3) {
			throw new PluggableTaskException(String.format("The token was expected to have 3 parts, but got %s.", parts.length));
		}
		return parts;
	}

	private static long hoursDifference(Date start, Date end) {
		long differenceInTime = end.getTime() - start.getTime();
		return differenceInTime / MILLI_TO_HOUR;
	}
	@Override
	public String getGSTIn() throws PluggableTaskException {
		return getMandatoryStringParameter(PARAM_GSTIN.getName());
	}

	private List<AddlDocDtls> getARN(UserDTO userDTO) {

		MetaFieldValue metaFieldValue = userDTO.getCompany().getMetaField("ARN No");
		if (metaFieldValue != null && StringUtils.isNotBlank((String) metaFieldValue.getValue())) {
			AddlDocDtls addlDocDtls = new AddlDocDtls();
			addlDocDtls.setInfo("Supply Meant For Exports Against LUT Without Payment Of IGST, Vide ARN No:" + metaFieldValue.getValue());
			return Arrays.asList(addlDocDtls);
		}
		return null;
	}

	/**
	 * Creating diapatcher details payload
	 * @return
	 * @throws PluggableTaskException
	 */
	private DispDtls constructDispatchDtlsFromPluginParam() throws PluggableTaskException {
		DispDtls dispDtls = new DispDtls();
		dispDtls.setDispatcherName(getMandatoryStringParameter(DISPATCH_PARAM_NAME.getName()));
 		dispDtls.setAddr1(getMandatoryStringParameter(DISPATCH_PARAM_ADDRESS_1.getName()));
		dispDtls.setAddr2(getParameter(DISPATCH_PARAM_ADDRESS_2.getName(), DISPATCH_PARAM_ADDRESS_2.getDefaultValue()));
		dispDtls.setLocation(getMandatoryStringParameter(DISPATCH_PARAM_LOCATION.getName()));
		dispDtls.setPinCode(Integer.parseInt(getMandatoryStringParameter(DISPATCH_PARAM_PIN_CODE.getName())));
		dispDtls.setStateCode(getMandatoryStringParameter(DISPATCH_PARAM_STATE_CODE.getName()));
		return dispDtls;
	}
}
