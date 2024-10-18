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

package com.sapienter.jbilling.server.invoice;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.sql.rowset.CachedRowSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDAS;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDTO;
import com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO;
import com.sapienter.jbilling.server.creditnote.db.CreditNoteInvoiceMapDTO;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentInvoiceMapWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.event.BeforeInvoiceDeleteEvent;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.PreferenceDAS;
import com.sapienter.jbilling.server.util.db.PreferenceDTO;

public class InvoiceBL extends ResultList implements InvoiceSQL {

	private static final String EINVOICE_PLUGIN_INTERFACE_NAME = "com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider";

	private InvoiceDAS invoiceDas = null;
	private InvoiceDTO invoice = null;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private EventLogger eLogger = null;

	public InvoiceBL(Integer invoiceId) {
		init();
		set(invoiceId);
	}

	public InvoiceBL() {
		init();
	}

	public InvoiceBL(InvoiceDTO invoice) {
		init();
		set(invoice.getId());
	}

	private void init() {
		eLogger = EventLogger.getInstance();
		invoiceDas = new InvoiceDAS();
	}

	public InvoiceDTO getEntity() {
		return invoice;
	}

	public InvoiceDAS getHome() {
		return invoiceDas;
	}

	public void set(Integer id) {
		invoice = invoiceDas.find(id);
	}

	public void set(InvoiceDTO invoice) {
		this.invoice = invoice;
	}

	/**
	 * @param userId
	 * @param newInvoice
	 * @param process    It can be null.
	 */
	public void create(Integer userId, NewInvoiceContext newInvoice, BillingProcessDTO process, Integer executorUserId) {
		// find out the entity id
		UserBL user = null;
		Integer entityId;
		if (process != null) {
			entityId = process.getEntity().getId();
		} else {
			// this is a manual invoice, there's no billing process
			user = new UserBL(userId);
			entityId = user.getEntityId(userId);
		}

		// verify if this entity is using the 'continuous invoice date'
		// preference
		try {
			String preferenceContinuousDateValue =
					PreferenceBL.getPreferenceValue(entityId, Constants.PREFERENCE_CONTINUOUS_DATE);

			if (StringUtils.isNotBlank(preferenceContinuousDateValue)) {
				Date lastDate = com.sapienter.jbilling.common.Util.parseDate(preferenceContinuousDateValue);
				logger.debug("Last date invoiced: {}", lastDate);

				if (lastDate.after(newInvoice.getBillingDate())) {
					logger.debug("Due date is before the last recorded date. Moving due date forward for continuous invoice dates.");
					newInvoice.setBillingDate(lastDate);

				} else {
					// update the lastest date only if this is not a review
					if (newInvoice.getIsReview() == null || newInvoice.getIsReview() == 0) {
						new PreferenceBL().createUpdateForEntity(entityId,
								Constants.PREFERENCE_CONTINUOUS_DATE,
								com.sapienter.jbilling.common.Util.parseDate(newInvoice.getBillingDate()));
					}
				}
			}
		} catch (EmptyResultDataAccessException e) {
			// not interested, ignore
		}

		// in any case, ensure that the due date is => that invoice date
		if (newInvoice.getDueDate().before(newInvoice.getBillingDate())) {
			logger.debug("Due date before billing date, moving date up to billing date.");
			newInvoice.setDueDate(newInvoice.getBillingDate());
		}

		// ensure that there are only so many decimals in the invoice
		Integer decimals = null;
		try {
			decimals = PreferenceBL.getPreferenceValueAsInteger(
					entityId, Constants.PREFERENCE_INVOICE_DECIMALS);
			if (decimals == null) {
				decimals = Constants.BIGDECIMAL_SCALE;
			}
		} catch (EmptyResultDataAccessException e) {
			// not interested, ignore
			decimals = Constants.BIGDECIMAL_SCALE;
		}

		logger.debug("Rounding {} to {} decimals.", newInvoice.getTotal(), decimals);
		if (newInvoice.getTotal() != null) {
			newInvoice.setTotal(newInvoice.getTotal().setScale(decimals, Constants.BIGDECIMAL_ROUND));
		}
		if (newInvoice.getBalance() != null) {
			newInvoice.setBalance(newInvoice.getBalance().setScale(decimals, Constants.BIGDECIMAL_ROUND));
		}

		// some API calls only accept ID's and do not pass meta-fields
		// update and validate meta-fields if they've been populated
		if (newInvoice.getMetaFields() != null && !newInvoice.getMetaFields().isEmpty()) {
			newInvoice.updateMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(), entityId, null, newInvoice);
		}

		// create the invoice row
		invoice = invoiceDas.create(userId, newInvoice, process);

		// add delegated/included invoice links
		if (newInvoice.getIsReview() == 0) {
			for (InvoiceDTO dto : newInvoice.getInvoices()) {
				dto.setInvoice(invoice);
			}
		}
		// generating invoice number.
		invoice.setPublicNumber(String.valueOf(generateInvoiceNumber(newInvoice, entityId)));
		logger.debug("invoice number {} generated for invoice id {}", invoice.getPublicNumber(), invoice.getId());

		// set the invoice's contact info with the current user's contact
		ContactBL contactBL = new ContactBL();
		ContactDTOEx contact = ContactBL.buildFromMetaField(userId, newInvoice.getBillingDate());
		if (null != contact) {
			contactBL.createForInvoice(contact, invoice.getId());
		}

		// add a log row for convenience
		if (null != executorUserId) {
			eLogger.audit(executorUserId, userId, Constants.TABLE_INVOICE,
					invoice.getId(), EventLogger.MODULE_INVOICE_MAINTENANCE,
					EventLogger.ROW_CREATED, null, null, null);
		} else {
			eLogger.auditBySystem(entityId, userId, Constants.TABLE_INVOICE,
					invoice.getId(), EventLogger.MODULE_INVOICE_MAINTENANCE,
					EventLogger.ROW_CREATED, null, null, null);
		}

	}

	private String generateInvoiceNumber(NewInvoiceContext newInvoice, Integer entityId) {
		// calculate/compose the number
		String numberStr;
		if (newInvoice.isReviewInvoice()) {
			// invoices for review will be seen by the entity employees
			// so the entity locale will be used
			EntityBL entity = new EntityBL(entityId);
			ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", entity.getLocale());
			numberStr = bundle.getString("invoice.review.number");
		} else if (StringUtils.isEmpty(newInvoice.getPublicNumber())) {
			String prefix = "";
			try {
				prefix = PreferenceBL.getPreferenceValue(entityId, Constants.PREFERENCE_INVOICE_PREFIX);
				if (StringUtils.isEmpty(prefix)) {
					prefix = "";
				}
			} catch (EmptyResultDataAccessException e) {
				//
			}
			//get and update number
			numberStr = prefix + getAndUpdateInvoiceNumberPreference(entityId);

		} else { // for upload of legacy invoices
			numberStr = newInvoice.getPublicNumber();
		}
		return numberStr;
	}

	/**
	 * Returns current invoice number and increment
	 * it by one with write lock in new transaction.
	 * @param entityId
	 * @return
	 */
	// method will use pesimistic locking to synchronize invoice number in multi-node environment,
	//which will make sure same invoice number will not be used by two different invoices.
	private Integer getAndUpdateInvoiceNumberPreference(Integer entityId) {
		IMethodTransactionalWrapper txAction = Context.getBean("methodTransactionalWrapper");
		SessionFactory sessionFactory = Context.getBean(Name.HIBERNATE_SESSION);
		return txAction.executeInNewTransaction(()-> {
			Session session = sessionFactory.getCurrentSession();
			CacheMode defaultCacheMode = session.getCacheMode();
			try {
				session.setCacheMode(CacheMode.IGNORE); // stop fetching PreferenceDTO from 2nd Level Cache.
				PreferenceDTO invoiceNumberPreferenceDTO = new PreferenceDAS().findByTypeWithLock(Constants.PREFERENCE_INVOICE_NUMBER,
						entityId, Constants.TABLE_ENTITY);
				Integer invoiceNumber = invoiceNumberPreferenceDTO.getIntValue();
				Integer returnValue;
				if(null == invoiceNumber) {
					invoiceNumber = 1; // initializing invoice number preference.
				}
				returnValue = invoiceNumber;
				invoiceNumber++;
				invoiceNumberPreferenceDTO.setValue(invoiceNumber); // update invoice number preference.
				return returnValue;
			} catch(Exception ex) {
				throw new SessionInternalError("Error in getAndUpdateInvoiceNumberPreference", ex);
			} finally {
				session.setCacheMode(defaultCacheMode);
			}
		});
	}

	public void createLines(NewInvoiceContext newInvoice) {
		Collection<InvoiceLineDTO> invoiceLines = invoice.getInvoiceLines();

		// Now create all the invoice lines, from the lines in the DTO
		// put there by the invoice composition pluggable tasks
		InvoiceLineDAS invoiceLineDas = new InvoiceLineDAS();

		Map<InvoiceLineDTO, InvoiceLineDTO> lineToAddToNewLineMap = new HashMap<>();

		// go over the DTO lines, creating one invoice line for each
		for(InvoiceLineDTO lineToAdd : newInvoice.getResultLines()) {
			// create the database row
			InvoiceLineDTO newLine = invoiceLineDas.create(lineToAdd.getDescription(), lineToAdd.getAmount(), lineToAdd.getQuantity(), lineToAdd.getPrice(),
					lineToAdd.getTypeId(), lineToAdd.getItem(), lineToAdd.getSourceUserId(), lineToAdd.getIsPercentage(),
					lineToAdd.getCallIdentifier(), lineToAdd.getCallCounter(), lineToAdd.getAssetIdentifier(), lineToAdd.getUsagePlanId(),
					lineToAdd.getGrossAmount(), lineToAdd.getTaxRate(), lineToAdd.getTaxAmount());

			// update the invoice-lines relationship
			newLine.setInvoice(invoice);
			newLine.setOrder(lineToAdd.getOrder());
			newLine.setParentLine(lineToAdd.getParentLine());
			invoiceLines.add(newLine);

			lineToAddToNewLineMap.put(lineToAdd, newLine);

		}
		//parent lines still refer to lines in newInvoice. Update to lines in invoice.
		for (Map.Entry<InvoiceLineDTO, InvoiceLineDTO> entry : lineToAddToNewLineMap.entrySet()) {
			InvoiceLineDTO newLine = entry.getValue();
			if (newLine.getParentLine() != null) {
				newLine.setParentLine(lineToAddToNewLineMap.get(newLine.getParentLine()));
			}
		}
		getHome().save(invoice);
		EventManager.process(new NewInvoiceEvent(invoice));
	}

	/**
	 * This will remove all the records (sql delete, not just flag them). It
	 * will also update the related orders if applicable
	 */
	public void delete(Integer executorId)  {
		if (invoice == null) {
			throw new SessionInternalError("An invoice has to be set before delete");
		}


		InvoiceDeletedEvent invoiceDeletedEvent = new InvoiceDeletedEvent(invoice);
		//delete the reseller invoices and orders of this invoice
		EventManager.process(invoiceDeletedEvent);
		// execute eInvoiceProvider plugin.
		triggerEInvoicePlugin(invoice.getBaseUser().getCompany().getId(), invoiceDeletedEvent);

		//prevent a delegated Invoice from being deleted
		if (invoice.getDelegatedInvoiceId() != null && invoice.getDelegatedInvoiceId() > 0) {
			throw new SessionInternalError("A carried forward Invoice cannot be deleted",
					new String[]{"InvoiceDTO,invoice,invoice.error.fkconstraint," + invoice.getId()});
		}
		// start by updating purchase_order.next_billable_day if applicatble
		// for each of the orders included in this invoice
		for (OrderProcessDTO orderProcess : invoice.getOrderProcesses()) {
			OrderDTO order = orderProcess.getPurchaseOrder();
			if (order.getNextBillableDay() == null) {
				// the next billable day doesn't need updating
				if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
					OrderBL orderBL = new OrderBL(order);
					orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, order.getUser().getCompany().getId()));//Constants.DEFAULT_ORDER_INVOICE_STATUS_ID
				}
				continue;
			}
			// only if this invoice is the responsible for the order's
			// next billable day
			if (order.getNextBillableDay().equals(orderProcess.getPeriodEnd())) {
				order.setNextBillableDay(orderProcess.getPeriodStart());

				for (OrderLineDTO line : order.getLines()) {
					for (OrderChangeDTO change : line.getOrderChanges()) {
						if ((change.getNextBillableDate() != null) && change.getNextBillableDate().equals(orderProcess.getPeriodEnd())) {
							change.setNextBillableDate(orderProcess.getPeriodStart());
						}
					}
				}

				if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
					OrderBL orderBL = new OrderBL(order);
					orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, order.getUser().getCompany().getId()));//Constants.DEFAULT_ORDER_INVOICE_STATUS_ID
				}
			}

		}

		OrderProcessDAS das = new OrderProcessDAS();
		Set<OrderProcessDTO> billingProcessOrderProcesses = null;
		if (null != invoice.getBillingProcess()) {
			billingProcessOrderProcesses = invoice.getBillingProcess().getOrderProcesses();
		}

		// go over the order process records again just to delete them
		// we are done with this order, delete the process row
		for (OrderProcessDTO orderProcess : invoice.getOrderProcesses()) {
			OrderDTO order = orderProcess.getPurchaseOrder();
			order.getOrderProcesses().remove(orderProcess);

			if (billingProcessOrderProcesses != null) {
				billingProcessOrderProcesses.remove(orderProcess);
			}

			das.delete(orderProcess);
		}
		invoice.getOrderProcesses().clear();

		// get rid of the contact associated with this invoice
		try {
			ContactBL contact = new ContactBL();
			if (contact.setInvoice(invoice.getId())) {
				contact.delete();
			}
		} catch (Exception e1) {
			logger.error("Exception deleting the contact of an invoice", e1);
		}

		// remove the payment link/s
		PaymentBL payment = new PaymentBL();
		Iterator<PaymentInvoiceMapDTO> it = invoice.getPaymentMap().iterator();
		while (it.hasNext()) {
			PaymentInvoiceMapDTO map = it.next();
			payment.removeInvoiceLink(map.getId());
			invoice.getPaymentMap().remove(map);
			// needed because the collection has changed
			it = invoice.getPaymentMap().iterator();
		}

		// log that this was deleted, otherwise there will be no trace
		if (executorId != null) {
			eLogger.audit(executorId, invoice.getBaseUser().getId(),
					Constants.TABLE_INVOICE, invoice.getId(),
					EventLogger.MODULE_INVOICE_MAINTENANCE,
					EventLogger.ROW_DELETED, null, null, null);
		}

		// before delete the invoice most delete the reference in table
		// PAYMENT_INVOICE
		new PaymentInvoiceMapDAS().deleteAllWithInvoice(invoice);

		Set<InvoiceDTO> invoices = invoice.getInvoices();
		if (CollectionUtils.isNotEmpty(invoices)) {
			for (InvoiceDTO delegate : invoices) {
				//set status to unpaid as against carried
				delegate.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_UNPAID));
				//remove delegated invoice link
				delegate.setInvoice(null);
				getHome().save(delegate);
			}
		}

		// now delete the invoice itself
		EventManager.process(new BeforeInvoiceDeleteEvent(invoice));

		//prevent a commission invoice to be deleted if CommissionInvoiceDeleteTask isn't configured
		if (new PartnerCommissionDAS().hasInvoiceCommission(invoice.getId())) {
			throw new SessionInternalError("An Invoice referenced to a commission cannot be deleted",
					new String[]{"invoice.error.commission.fkconstraint," + invoice.getId()});
		}

		if (null != invoice.getBillingProcess()) {
			invoice.getBillingProcess().getInvoices().remove(invoice);
		}

		getHome().delete(invoice);
		getHome().flush();

	}

	public void update(Integer entityId, NewInvoiceContext addition) {

		// add the lines to the invoice first
		createLines(addition);
		Integer decimals = Constants.BIGDECIMAL_SCALE;
		try {
			decimals = PreferenceBL.getPreferenceValueAsInteger(
					entityId, Constants.PREFERENCE_INVOICE_DECIMALS);
			if (decimals == null) {
				decimals = Constants.BIGDECIMAL_SCALE;
			}
		} catch (EmptyResultDataAccessException e) {
			// do nothing
		}
		// update the inoice record considering the new lines
		invoice.setTotal(calculateTotal().setScale(decimals, Constants.BIGDECIMAL_ROUND)); // new total
		// adjust the balance
		addition.calculateTotal();
		BigDecimal balance = invoice.getBalance();
		balance = balance.add(addition.getTotal());
		invoice.setBalance(balance.setScale(decimals, Constants.BIGDECIMAL_ROUND));

		//set to process = 0 only if balance is minimum balance to ignore
		if (invoice.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
			invoice.setToProcess(0);
		} else {
			invoice.setToProcess(1);
		}

		if (addition.getMetaFields() != null && !addition.getMetaFields().isEmpty()) {
			invoice.updateMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(), entityId, null, addition);
		}
	}

	private BigDecimal calculateTotal() {
		if(CollectionUtils.isNotEmpty(invoice.getInvoiceLines())) {
			return invoice.getInvoiceLines()
					.stream()
					.map(InvoiceLineDTO::getAmount)
					.reduce((a1, a2) -> a1.add(a2))
					.orElse(BigDecimal.ZERO);

		}
		return BigDecimal.ZERO;
	}

	public static BigDecimal getTotalWithoutCarried(Collection<InvoiceLineDTO> lines) {
		BigDecimal total = BigDecimal.ZERO;
		for (InvoiceLineDTO line : lines) {
			if (!Constants.INVOICE_LINE_TYPE_DUE_INVOICE.equals(line.getInvoiceLineType().getId())) {
				total = total.add(line.getAmount());
			}
		}
		return total;
	}

	public CachedRowSet getPayableInvoicesByUser(Integer userId) throws SQLException {

		prepareStatement(InvoiceSQL.payableByUser);
		cachedResults.setInt(1, userId);

		execute();
		conn.close();
		return cachedResults;
	}

	public BigDecimal getTotalPaidWithCarried() {
		return invoice.getPaymentMap().stream()
				.map(PaymentInvoiceMapDTO::getPayment)
				.map(PaymentDTO::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalPaid() {
		BigDecimal retValue = new BigDecimal(0);
		for (PaymentInvoiceMapDTO paymentMap : invoice.getPaymentMap()) {
			retValue = retValue.add(paymentMap.getAmount());
		}
		return retValue;
	}

	public CachedRowSet getList(Integer orderId) throws SQLException {
		prepareStatement(InvoiceSQL.customerList);

		// find out the user from the order
		Integer userId;
		OrderBL order = new OrderBL(orderId);
		if (order.getDTO().getUser().getCustomer().getParent() == null) {
			userId = order.getDTO().getUser().getUserId();
		} else {
			userId = order.getDTO().getUser().getCustomer().getParent().getBaseUser().getUserId();
		}
		cachedResults.setInt(1, userId);
		execute();
		conn.close();
		return cachedResults;
	}

	public CachedRowSet getList(Integer entityId, Integer userRole, Integer userId) throws SQLException {

		if (userRole.equals(Constants.TYPE_INTERNAL)) {
			prepareStatement(InvoiceSQL.internalList);
		} else if (userRole.equals(Constants.TYPE_ROOT) || userRole.equals(Constants.TYPE_CLERK)) {
			prepareStatement(InvoiceSQL.rootClerkList);
			cachedResults.setInt(1, entityId);
		} else if (userRole.equals(Constants.TYPE_PARTNER)) {
			prepareStatement(InvoiceSQL.partnerList);
			cachedResults.setInt(1, entityId);
			cachedResults.setInt(2, userId);
		} else if (userRole.equals(Constants.TYPE_CUSTOMER)) {
			prepareStatement(InvoiceSQL.customerList);
			cachedResults.setInt(1, userId);
		} else {
			throw new SessionInternalError("The invoice list for the type " + userRole + " is not supported");
		}

		execute();
		conn.close();
		return cachedResults;
	}

	public List<InvoiceDTO> getListInvoicesPaged(Integer entityId, Integer userId, Integer limit, Integer offset) {
		return new InvoiceDAS().findInvoicesByUserPaged(userId, limit, offset);
	}

	public CachedRowSet getInvoicesByProcessId(Integer processId) throws SQLException {

		prepareStatement(InvoiceSQL.processList);
		cachedResults.setInt(1, processId);

		execute();
		conn.close();
		return cachedResults;
	}

	public CachedRowSet getInvoicesToPrintByProcessId(Integer processId) throws SQLException {

		prepareStatement(InvoiceSQL.processPrintableList);
		cachedResults.setInt(1, processId);

		execute();
		conn.close();
		return cachedResults;
	}

	public CachedRowSet getInvoicesByUserId(Integer userId) throws SQLException {
		prepareStatement(InvoiceSQL.custList);
		cachedResults.setInt(1, userId);

		execute();
		conn.close();
		return cachedResults;
	}

	public CachedRowSet getInvoicesByIdRange(Integer from, Integer to, Integer entityId) throws SQLException {

		prepareStatement(InvoiceSQL.rangeList);
		cachedResults.setInt(1, from);
		cachedResults.setInt(2, to);
		cachedResults.setInt(3, entityId);

		execute();
		conn.close();
		return cachedResults;
	}

	public Integer[] getInvoicesByCreateDateArray(Integer entityId, Date since, Date until) throws SQLException {

		cachedResults = getInvoicesByCreateDate(entityId, since, until);

		// get ids for return
		List<Integer> ids = new ArrayList<>();
		while (cachedResults.next()) {
			ids.add(cachedResults.getInt(1));
		}
		Integer[] retValue = new Integer[ids.size()];
		if (retValue.length > 0) {
			ids.toArray(retValue);
		}

		return retValue;
	}

	public CachedRowSet getInvoicesByCreateDate(Integer entityId, Date since, Date until) throws SQLException {

		prepareStatement(InvoiceSQL.getByDate);
		cachedResults.setInt(1, entityId);
		cachedResults.setDate(2, new java.sql.Date(since.getTime()));
		// add a day to include the until date
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(until);
		cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
		cachedResults.setDate(3, new java.sql.Date(cal.getTime().getTime()));

		execute();

		conn.close();
		return cachedResults;
	}

	public Integer convertNumberToID(Integer entityId, String number) throws SQLException {

		prepareStatement(InvoiceSQL.getIDfromNumber);
		cachedResults.setInt(1, entityId);
		cachedResults.setString(2, number);

		execute();

		conn.close();
		if (cachedResults.wasNull()) {
			return null;
		} else {
			cachedResults.next();
			return cachedResults.getInt(1);
		}
	}

	public Integer getLastByUser(Integer userId) throws SQLException {

		Integer retValue = null;
		if (userId == null) {
			return null;
		}
		prepareStatement(InvoiceSQL.lastIdbyUser);
		cachedResults.setInt(1, userId);

		execute();
		if (cachedResults.next()) {
			int value = cachedResults.getInt(1);
			if (!cachedResults.wasNull()) {
				retValue = value;
			}
		}
		conn.close();
		return retValue;
	}

	public Integer getLastByUserAndItemType(Integer userId, Integer itemTypeId) throws SQLException {

		Integer retValue = null;
		if (userId == null) {
			return null;
		}
		prepareStatement(InvoiceSQL.lastIdbyUserAndItemType);
		cachedResults.setInt(1, userId);
		cachedResults.setInt(2, itemTypeId);

		execute();
		if (cachedResults.next()) {
			int value = cachedResults.getInt(1);
			if (!cachedResults.wasNull()) {
				retValue = value;
			}
		}
		cachedResults.close();
		conn.close();
		return retValue;
	}

	public Boolean isUserWithOverdueInvoices(Integer userId, Date today, Integer excludeInvoiceId) throws SQLException {

		Boolean retValue = Boolean.FALSE;
		prepareStatement(InvoiceSQL.getOverdueForAgeing);
		cachedResults.setDate(1, new java.sql.Date(today.getTime()));
		cachedResults.setInt(2, userId);
		if (excludeInvoiceId != null) {
			cachedResults.setInt(3, excludeInvoiceId);
		} else {
			// nothing to exclude, use an imposible ID (zero)
			cachedResults.setInt(3, 0);
		}

		execute();
		InvoiceDAS invoiceDAS = new InvoiceDAS();
		while (cachedResults.next()) {
			int invoiceId = cachedResults.getInt(1);
			if (invoiceDAS.find(invoiceId).getBalance().compareTo(BigDecimal.ZERO) > 0) {
				retValue = Boolean.TRUE;
				logger.debug("user with invoice: {}", cachedResults.getInt(1));
				break;
			}
		}

		conn.close();
		logger.debug("user with overdue: {}", retValue);
		return retValue;
	}

	public Integer[] getUsersOverdueInvoices(Integer userId, Date date) {
		List<Integer> result = new InvoiceDAS().findIdsOverdueForUser(userId, date);
		return result.toArray(new Integer[result.size()]);
	}

	public Integer[] getUserInvoicesByDate(Integer userId, Date since, Date until) {
		// add a day to include the until date
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(until);
		cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
		until = cal.getTime();

		List<Integer> result = new InvoiceDAS().findIdsByUserAndDate(userId, since, until);
		return result.toArray(new Integer[result.size()]);
	}

	public Integer[] getManyWS(Integer userId, Integer number) {
		List<Integer> result = new InvoiceDAS().findIdsByUserLatestFirst(
				userId, number);
		return result.toArray(new Integer[result.size()]);
	}

	public Integer[] getManyByItemTypeWS(Integer userId, Integer itemTypeId, Integer number)  {
		List<Integer> result = new InvoiceDAS().findIdsByUserAndItemTypeLatestFirst(userId, itemTypeId, number);
		return result.toArray(new Integer[result.size()]);
	}


	public InvoiceWS[] DTOtoWS(List<InvoiceDTO> dtos) {
		InvoiceWS[] retValue = new InvoiceWS[dtos.size()];
		for (int f = 0; f < retValue.length; f++) {
			retValue[f] = InvoiceBL.getWS(dtos.get(f));
		}

		logger.debug("converstion {}", retValue.length);
		return retValue;
	}

	/**
	 * Returns Array ofInvoiceWS[] which does not have any associations
	 * from InvoiceDTO
	 *
	 * @param dtos
	 * @return InvoiceWS[]
	 */
	public InvoiceWS[] DTOtoSimpleWS(List<InvoiceDTO> dtos) {
		InvoiceWS[] retValue = new InvoiceWS[dtos.size()];
		for (int f = 0; f < retValue.length; f++) {
			retValue[f] = InvoiceBL.getSimpleInvoiceWS(dtos.get(f));
		}

		logger.debug("converstion {}", retValue.length);
		return retValue;
	}

	public void sendReminders(Date today) {
		for(CompanyDTO entity : new CompanyDAS().findEntities()) {
			Integer entityId = entity.getId();
			int preferenceUseInvoiceReminders =
					PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_USE_INVOICE_REMINDERS);
			if (preferenceUseInvoiceReminders == 1) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(today);
				INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
				int preferenceFirstReminder =
						PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_FIRST_REMINDER);
				cal.add(Calendar.DAY_OF_MONTH, -preferenceFirstReminder);
				Date createDateTime = cal.getTime();
				cal.setTime(today);
				int preferenceNextReminder =
						PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_NEXT_REMINDER);
				cal.add(Calendar.DAY_OF_MONTH, -preferenceNextReminder);
				Date lastReminder = cal.getTime();
				ScrollableResults invoices = null;
				try {
					SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
					NotificationBL notif = new NotificationBL();
					invoices = invoiceDas.getListOfInvoicesToSendReminder(entityId, today, createDateTime, lastReminder);
					int counter = 0;
					while(invoices.next()) {
						InvoiceDTO invoiceRecord = (InvoiceDTO) invoices.get(0);
						sendReminderToInvoice(invoiceRecord, today, notif, notificationSess, entityId);
						if( ++counter % 50 == 0) {
							sf.getCurrentSession().flush();
							sf.getCurrentSession().clear();
						}
					}
				} catch(HibernateException ex) {
					throw new SessionInternalError(ex);
				} finally {
					if(null!= invoices) {
						invoices.close();
					}
				}
			}
		}

	}

	private void sendReminderToInvoice(InvoiceDTO record, Date today, NotificationBL notif,
			INotificationSessionBean notificationSessionBean, Integer entityId) {
		set(record);
		long mils = invoice.getDueDate().getTime() - today.getTime();
		int days = (int) TimeUnit.MILLISECONDS.toDays(mils);
		try {
			MessageDTO message = notif.getInvoiceReminderMessage(
					entityId, invoice.getBaseUser().getUserId(),
					days, invoice.getDueDate(),
					invoice.getPublicNumber(), invoice.getTotal(),
					invoice.getCreateDatetime(), invoice.getCurrency().getId());

			notificationSessionBean.notify(invoice.getBaseUser(), message);
			invoice.setLastReminder(today);
		} catch (NotificationNotFoundException e) {
			logger.warn("There are invoice to send reminders, but the notification message is missing for entity {}", entityId);
		} catch (SessionInternalError e) {
			throw e;
		}
	}

	public InvoiceWS getWS() {
		return getWS(invoice);
	}

	/**
	 * Returns InvoiceWS which does not have any associations
	 * from InvoiceDTO
	 *
	 * @param invoiceDTO
	 * @return InvoiceWS
	 */
	public static InvoiceWS getSimpleInvoiceWS(InvoiceDTO invoiceDTO) {
		if (invoiceDTO == null) {
			return null;
		}
		InvoiceWS retValue = new InvoiceWS();
		retValue.setId(invoiceDTO.getId());
		retValue.setCreateDatetime(invoiceDTO.getCreateDatetime());
		retValue.setCreateTimeStamp(invoiceDTO.getCreateTimestamp());
		retValue.setLastReminder(invoiceDTO.getLastReminder());
		retValue.setDueDate(invoiceDTO.getDueDate());
		retValue.setTotal(invoiceDTO.getTotal());
		retValue.setToProcess(invoiceDTO.getToProcess());
		retValue.setStatusId(invoiceDTO.getInvoiceStatus().getId());
		retValue.setBalance(invoiceDTO.getBalance());
		retValue.setCarriedBalance(invoiceDTO.getCarriedBalance());
		retValue.setInProcessPayment(invoiceDTO.getInProcessPayment());
		retValue.setDeleted(invoiceDTO.getDeleted());
		retValue.setPaymentAttempts(invoiceDTO.getPaymentAttempts());
		retValue.setIsReview(invoiceDTO.getIsReview());
		retValue.setCurrencyId(invoiceDTO.getCurrency().getId());
		retValue.setCustomerNotes(invoiceDTO.getCustomerNotes());
		retValue.setNumber(invoiceDTO.getPublicNumber());
		retValue.setOverdueStep(invoiceDTO.getOverdueStep());
		retValue.setUserId(invoiceDTO.getBaseUser().getId());
		Integer delegatedInvoiceId = invoiceDTO.getInvoice() == null ? null : invoiceDTO.getInvoice().getId();

		retValue.setDelegatedInvoiceId(delegatedInvoiceId);

		retValue.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(
				new UserBL().getEntityId(invoiceDTO.getBaseUser().getId()), invoiceDTO));

		EInvoiceLogDAS eInvoiceLogDAS = new EInvoiceLogDAS();
		EInvoiceLogDTO eInvoiceLog = eInvoiceLogDAS.findByInvoiceId(invoiceDTO.getId());
		if(null!= eInvoiceLog) {
			retValue.setIrn(eInvoiceLog.getIrn());
		}
		PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
		List<PaymentUrlLogDTO> paymentUrlLogDTOS = paymentUrlLogDAS.findAllByInvoiceId(invoiceDTO.getId());
		if (CollectionUtils.isNotEmpty(paymentUrlLogDTOS)) {
			retValue.setPaymentUrlIds(paymentUrlLogDTOS.stream()
				.map(PaymentUrlLogDTO::getId)
				.toArray(Integer[]::new));
		}
		return retValue;
	}

	public static InvoiceWS getWS(InvoiceDTO i) {
		if (i == null) {
			return null;
		}
		InvoiceWS retValue = new InvoiceWS();
		retValue.setId(i.getId());
		retValue.setCreateDatetime(i.getCreateDatetime());
		retValue.setCreateTimeStamp(i.getCreateTimestamp());
		retValue.setLastReminder(i.getLastReminder());
		retValue.setDueDate(i.getDueDate());
		retValue.setTotal(i.getTotal());
		retValue.setToProcess(i.getToProcess());
		retValue.setStatusId(i.getInvoiceStatus().getId());
		retValue.setBalance(i.getBalance());
		retValue.setCarriedBalance(i.getCarriedBalance());
		retValue.setInProcessPayment(i.getInProcessPayment());
		retValue.setDeleted(i.getDeleted());
		retValue.setPaymentAttempts(i.getPaymentAttempts());
		retValue.setIsReview(i.getIsReview());
		retValue.setCurrencyId(i.getCurrency().getId());
		retValue.setCustomerNotes(i.getCustomerNotes());
		retValue.setNumber(i.getPublicNumber());
		retValue.setOverdueStep(i.getOverdueStep());
		retValue.setUserId(i.getBaseUser().getId());

		Integer delegatedInvoiceId = i.getInvoice() == null ? null : i.getInvoice().getId();
		Integer userId = i.getBaseUser().getId();
		Integer[] payments = new Integer[i.getPaymentMap().size()];
		com.sapienter.jbilling.server.entity.InvoiceLineDTO[] invoiceLines =
				new com.sapienter.jbilling.server.entity.InvoiceLineDTO[i.getInvoiceLines().size()];
		Integer[] orders = new Integer[i.getOrderProcesses().size()];

		int f;
		f = 0;
		for (PaymentInvoiceMapDTO p : i.getPaymentMap()) {
			payments[f++] = p.getPayment().getId();
		}
		f = 0;
		for (OrderProcessDTO orderP : i.getOrderProcesses()) {
			orders[f++] = orderP.getPurchaseOrder().getId();
		}
		f = 0;
		List<InvoiceLineDTO> ordInvoiceLines = new ArrayList<>(i.getInvoiceLines());
		Collections.sort(ordInvoiceLines, new InvoiceLineComparator());
		for (InvoiceLineDTO line : ordInvoiceLines) {
			com.sapienter.jbilling.server.entity.InvoiceLineDTO  lineDto
			= new com.sapienter.jbilling.server.entity.InvoiceLineDTO(line.getId(),
					line.getDescription(), line.getAmount(), line.getPrice(), line.getQuantity(),
					line.getDeleted(), line.getItem() == null ? null : line.getItem().getId(),
							line.getSourceUserId(), line.getIsPercentage(), line.getCallIdentifier(),
							line.getUsagePlanId(), line.getCallCounter(), line.getTaxRate(), line.getTaxAmount(),
							line.getGrossAmount());
			lineDto.setTypeId(Integer.valueOf(line.getTypeId()));
			invoiceLines[f++] = lineDto;
		}

		retValue.setDelegatedInvoiceId(delegatedInvoiceId);
		retValue.setUserId(userId);
		retValue.setPayments(payments);
		retValue.setInvoiceLines(invoiceLines);
		retValue.setOrders(orders);
		retValue.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(new UserBL().getEntityId(userId), i));

		if(i.getBillingProcess() != null) {
			retValue.setBillingProcess(BillingProcessBL.getSimpleWS(i.getBillingProcess()));
		}

		Collection<CreditNoteInvoiceMapDTO> creditNotes = i.getCreditNoteMap();
		if(CollectionUtils.isNotEmpty(creditNotes)) {
			retValue.setCreditNoteIds(creditNotes.stream()
					.map(CreditNoteInvoiceMapDTO::getCreditNote)
					.map(CreditNoteDTO::getId)
					.toArray(Integer[]::new));
		}
		Collection<PaymentInvoiceMapDTO> paymentInvoiceMap = i.getPaymentMap();
		if(CollectionUtils.isNotEmpty(paymentInvoiceMap)) {
			retValue.setPaymentInvoiceMap(paymentInvoiceMap
					.stream()
					.map(InvoiceBL::convertToPaymentInvoiceMapWS)
					.toArray(PaymentInvoiceMapWS[]::new));
		}
		retValue.setAccessEntities(getAccessEntities(i.getBaseUser()));
		EInvoiceLogDAS eInvoiceLogDAS = new EInvoiceLogDAS();
		EInvoiceLogDTO eInvoiceLog = eInvoiceLogDAS.findByInvoiceId(i.getId());
		if(null!= eInvoiceLog) {
			retValue.setIrn(eInvoiceLog.getIrn());
		}
		PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
		List<PaymentUrlLogDTO> paymentUrlLogDTOS = paymentUrlLogDAS.findAllByInvoiceId(i.getId());
		if (CollectionUtils.isNotEmpty(paymentUrlLogDTOS)) {
			retValue.setPaymentUrlIds(paymentUrlLogDTOS.stream()
				.map(PaymentUrlLogDTO::getId)
				.toArray(Integer[]::new));
		}
		return retValue;
	}

	/**
	 * Converts {@link PaymentInvoiceMapDTO} to {@link PaymentInvoiceMapWS}
	 * @param dto
	 * @return
	 */
	private static PaymentInvoiceMapWS convertToPaymentInvoiceMapWS(PaymentInvoiceMapDTO dto) {
		CompanyDTO entity = dto.getPayment().getBaseUser().getEntity();
		Date createDateTime = TimezoneHelper.convertToTimezone(dto.getCreateDatetime(),
				TimezoneHelper.getCompanyLevelTimeZone(entity.getId()));
		PaymentInvoiceMapWS paymentInvoiceMap = new PaymentInvoiceMapWS(dto.getId(), dto.getInvoiceEntity().getId(),
				dto.getPayment().getId(), createDateTime, dto.getAmount());
		paymentInvoiceMap.setPaymentType(dto.getPayment().getIsRefund());
		paymentInvoiceMap.setPaymentStatus(dto.getPayment().getPaymentResult().getDescription(entity.getLanguageId()));
		return paymentInvoiceMap;
	}

	public InvoiceDTO getDTOEx(Integer languageId, boolean forDisplay) {

		if (!forDisplay) {
			return invoice;
		}

		InvoiceDTO invoiceDTO = new InvoiceDTO(invoice);
		// make sure that the lines are properly ordered
		List<InvoiceLineDTO> orderdLines = new ArrayList<>(invoiceDTO.getInvoiceLines());
		Collections.sort(orderdLines, new InvoiceLineComparator());
		invoiceDTO.setInvoiceLines(new HashSet<>(orderdLines));

		UserBL userBl = new UserBL(invoice.getBaseUser());
		ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", userBl.getLocale());

		// now add headers and footers if this invoices has sub-account
		// lines
		if (invoiceDTO.hasSubAccounts()) {
			addHeadersFooters(orderdLines, bundle, invoice.getBaseUser().getCustomer().getId());
		}
		// add a grand total final line
		InvoiceLineDTO total = new InvoiceLineDTO();
		total.setDescription(bundle.getString("invoice.line.total"));
		total.setAmount(invoice.getTotal());
		total.setIsPercentage(0);
		invoiceDTO.getInvoiceLines().add(total);

		// add some currency info for the human
		CurrencyBL currency = new CurrencyBL(invoice.getCurrency().getId());
		if (languageId != null) {
			invoiceDTO.setCurrencyName(currency.getEntity().getDescription(languageId));
		}

		invoiceDTO.setCurrencySymbol(currency.getEntity().getSymbol());
		return invoiceDTO;
	}

	/**
	 * Will add lines with headers and footers to make an invoice with
	 * sub-accounts more readable. The lines have to be already sorted.
	 *
	 * @param lines
	 * @param parentId
	 * @return
	 */
	private void addHeadersFooters(List<InvoiceLineDTO> lines, ResourceBundle bundle, Integer parentId) {
		Integer nowProcessing = -1;
		BigDecimal total = null;
		int totalLines = lines.size();
		int subaccountNumber = 0;
		CustomerDTO subAccountCustomer;
		logger.debug("adding headers & footers.{}", totalLines);

		for (int idx = 0; idx < totalLines; idx++) {
			InvoiceLineDTO line = lines.get(idx);

			if (null != line.getSourceUserId()) {
				// to check an invoiceLine belongs to a sub-account user
				// compare invoiceLine.customer.parent with invoice.customer
				subAccountCustomer = UserBL.getUserEntity(line.getSourceUserId()).getCustomer();
				if (null != subAccountCustomer.getParent() && (subAccountCustomer.getParent().getId() == parentId) && (!line.getSourceUserId().equals(nowProcessing))) {
					// line break
					nowProcessing = line.getSourceUserId();
					subaccountNumber++;
					// put the total first
					if (total != null) { // it could be the first sub-account
						InvoiceLineDTO totalLine = new InvoiceLineDTO();
						totalLine.setDescription(bundle.getString("invoice.line.subAccount.footer"));
						totalLine.setAmount(total);
						lines.add(idx, totalLine);
						idx++;
						totalLines++;
					}
					total = BigDecimal.ZERO;

					// now the header announcing a new sub-accout
					InvoiceLineDTO headerLine = new InvoiceLineDTO();
					try {
						ContactBL contact = new ContactBL();
						contact.set(nowProcessing);
						StringBuilder text = new StringBuilder();
						text.append(subaccountNumber + " - ");
						text.append(bundle.getString("invoice.line.subAccount.header1"));
						text.append(" " + bundle.getString("invoice.line.subAccount.header2") + " " + nowProcessing);
						if (null != contact.getEntity()) {
							if (contact.getEntity().getFirstName() != null) {
								text.append(" " + contact.getEntity().getFirstName());
							}
							if (contact.getEntity().getLastName() != null) {
								text.append(" " + contact.getEntity().getLastName());
							}
						}
						headerLine.setDescription(text.toString());
						lines.add(idx, headerLine);
						idx++;
						totalLines++;
					} catch (Exception e) {
						logger.error("Exception", e);
						return;
					}
				}

				// update the total
				if (total != null) {
					// there had been at least one sub-account processed
					if (null != subAccountCustomer.getParent() && (subAccountCustomer.getParent().getId() == parentId)) {
						total = total.add(line.getAmount());
					} else {
						// this is the last total to display, from now on the
						// lines are not of sub-accounts
						InvoiceLineDTO totalLine = new InvoiceLineDTO();
						totalLine.setDescription(bundle.getString("invoice.line.subAccount.footer"));
						totalLine.setAmount(total);
						lines.add(idx, totalLine);
						total = null; // to avoid repeating
					}
				}
			}
		}
		// if there are no lines after the last sub-account, we need
		// a total for it
		if (total != null) { // only if it wasn't added before
			InvoiceLineDTO totalLine = new InvoiceLineDTO();
			totalLine.setDescription(bundle.getString("invoice.line.subAccount.footer"));
			totalLine.setAmount(total);
			lines.add(totalLine);
		}

		logger.debug("done {}", lines.size());
	}

	/***
	 * Will return InvoiceDTO with headers
	 * if invoice for sub-account users
	 *
	 * @return
	 */
	public InvoiceDTO getInvoiceDTOWithHeaderLines() {
		InvoiceDTO invoiceDTO = new InvoiceDTO(invoice);
		List<InvoiceLineDTO> invoiceLines = new ArrayList<>(invoiceDTO.getInvoiceLines());

		// now add headers if this invoices has sub-account lines
		if (invoiceDTO.hasSubAccounts()) {
			invoiceDTO.setInvoiceLines(new HashSet<>(addHeaders(invoiceLines, invoice.getBaseUser().getId())));
		} else {
			//if there are no sub account than just sort the lines
			Collections.sort(invoiceLines, new InvoiceLineComparator());
			invoiceDTO.setInvoiceLines(new HashSet<>(invoiceLines));
		}

		return invoiceDTO;
	}

	/**
	 * Will add lines with headers to make an invoice with
	 * subaccounts more readable.
	 *
	 * @param lines
	 * @param parentUserId
	 * @return
	 */
	public static List<InvoiceLineDTO> addHeaders(List<InvoiceLineDTO> lines, Integer parentUserId) {
		Integer nowProcessing;
		Integer parentCustomerId = UserBL.getUserEntity(parentUserId).getCustomer().getId();

		logger.debug("adding headers for sub-account users, total invoice lines : {}", lines.size());

		Map<Integer, List<InvoiceLineDTO>> accountLineGroups = new HashMap<>();
		Map<Integer, InvoiceLineDTO> accountLineHeaders = new HashMap<>();

		for (InvoiceLineDTO line : lines) {
			// to check an invoiceLine belongs to a sub-account user
			// compare invoiceLine.customer.parent with invoice.customer
			if (null != line.getSourceUserId()) {

				nowProcessing = line.getSourceUserId();

				UserDTO subAccount = UserBL.getUserEntity(nowProcessing);
				if (subAccount != null) {
					CustomerDTO subAccountCustomer = subAccount.getCustomer();
					while (subAccountCustomer != null) {
						if (null != subAccountCustomer.getParent() &&
								subAccountCustomer.getParent().getId() == parentCustomerId &&
								!accountLineHeaders.containsKey(nowProcessing)) {
							InvoiceLineDTO headerLine = createHeaderLine(nowProcessing);
							if (null == headerLine) {
								logger.debug("Could not create a header line for invoice line {} source user id {}", line.getId(), nowProcessing);
							} else {
								accountLineHeaders.put(nowProcessing, headerLine);
							}

							subAccountCustomer = null;
						} else {
							subAccountCustomer = subAccountCustomer.getParent();
						}
					}
				}

				//groups the lines based on a account
				List<InvoiceLineDTO> group = accountLineGroups.computeIfAbsent(nowProcessing, key -> new ArrayList<>());
				group.add(line);
			} else {
				nowProcessing = line.getInvoice().getBaseUser().getId();
				List<InvoiceLineDTO> group = accountLineGroups.computeIfAbsent(nowProcessing, key -> new ArrayList<>());
				group.add(line);
			}
		}

		//first add the parent invoice lines
		List<InvoiceLineDTO> result = new ArrayList<>();
		List<InvoiceLineDTO> parentLines = accountLineGroups.get(parentUserId);
		if (null != parentLines && !parentLines.isEmpty()) {
			accountLineGroups.remove(parentUserId);
			Collections.sort(parentLines, new InvoiceLineComparator());
			result.addAll(parentLines);
		}

		//now add subaccount invoice lines
		for (Map.Entry<Integer, List<InvoiceLineDTO>> entry : accountLineGroups.entrySet()) {
			Integer userId = entry.getKey();
			InvoiceLineDTO headerLine = accountLineHeaders.get(userId);
			if (null != headerLine) {
				result.add(headerLine);
			}

			List<InvoiceLineDTO> subAccountLines = entry.getValue();
			if (null != subAccountLines && !subAccountLines.isEmpty()) {
				Collections.sort(subAccountLines, new InvoiceLineChildComparator());
				result.addAll(subAccountLines);
			}
		}

		logger.debug("Now, total line size : {}", result.size());
		return result;
	}

	private static InvoiceLineDTO createHeaderLine(Integer sourceUserId) {
		// now the header announcing a new sub-account
		InvoiceLineDTO headerLine = new InvoiceLineDTO();
		try {
			ContactDTOEx contact = ContactBL.buildFromMetaField(sourceUserId, TimezoneHelper.companyCurrentDateByUserId(sourceUserId));
			//get user's name
			StringBuilder name = new StringBuilder("");
			if (null != contact) {
				if (!StringUtils.isEmpty(contact.getFirstName()) || !StringUtils.isEmpty(contact.getLastName())) {
					if (contact.getFirstName() != null) {
						name.append(contact.getFirstName());
					}
					if (contact.getLastName() != null) {
						name.append(" ").append(contact.getLastName());
					}
				} else if (!StringUtils.isEmpty(contact.getOrganizationName())) {
					name.append(contact.getOrganizationName());
				}
			}
			if (name.toString().equals("")) {
				name.append(new UserDAS().find(sourceUserId).getUserName());
			}
			headerLine.setDescription(name.toString());
			headerLine.setSourceUserId(sourceUserId);
		} catch (Exception e) {
			logger.error("Exception", e);
			return null;
		}
		return headerLine;
	}

	public InvoiceDTO getDTO() {
		return invoice;

	}

	// given the current invoice, it will 'rewind' to the previous one
	public void setPrevious() throws SQLException {
		prepareStatement(InvoiceSQL.previous);
		cachedResults.setInt(1, invoice.getBaseUser().getUserId());
		cachedResults.setInt(2, invoice.getId());
		boolean found = false;

		execute();
		if (cachedResults.next()) {
			int value = cachedResults.getInt(1);
			if (!cachedResults.wasNull()) {
				set(value);
				found = true;
			}
		}
		conn.close();

		if (!found) {
			throw new EmptyResultDataAccessException("No previous invoice found", 1);
		}
	}

	// given the current invoice, it will 'rewind' to the previous one
	public void setPreviousByInvoiceDate() throws SQLException {
		prepareStatement(InvoiceSQL.previousByCreateDateTime);
		cachedResults.setInt(1, invoice.getBaseUser().getUserId());
		cachedResults.setDate(2, new java.sql.Date(invoice.getCreateDatetime().getTime()));
		cachedResults.setInt(3, invoice.getBaseUser().getUserId());
		boolean found = false;

		execute();
		if (cachedResults.next()) {
			int value = cachedResults.getInt(1);
			if (!cachedResults.wasNull()) {
				set(value);
				found = true;
			}
		}
		conn.close();

		if (!found) {
			throw new EmptyResultDataAccessException("No previous invoice found", 1);
		}
	}

	public List<InvoiceWS> findInvoicesByUserPagedSortedByAttribute(Integer userId, Integer limit, int offset, String sort, ListField.Order order, Integer callerLanguageId) {
		List<InvoiceDTO> invoices = invoiceDas.findInvoicesByUserPagedSortedByAttribute(userId, limit, offset, sort, order);
		if (CollectionUtils.isEmpty(invoices)) {
			return Collections.emptyList();
		}
		List<InvoiceWS> invoicesWS = new ArrayList<>();
		for (InvoiceDTO invoiceDTO : invoices) {
			InvoiceWS wsdto = InvoiceBL.getWS(invoiceDTO);
			if (null != invoiceDTO.getInvoiceStatus()) {
				wsdto.setStatusDescr(invoiceDTO.getInvoiceStatus().getDescription(callerLanguageId));
			}
			invoicesWS.add(wsdto);
		}
		return invoicesWS;
	}

	public CachedRowSet getPayableInvoicesByUserOldestFirst(Integer userId) throws SQLException {
		prepareStatement(InvoiceSQL.payableByUserOldestFirst);
		cachedResults.setInt(1, userId);

		execute();
		conn.close();
		return cachedResults;
	}

	public Integer[] getUnpaidInvoicesByUserId(Integer userId) {
		List<Integer> result = new InvoiceDAS().findInvoicesByUserIdOldestFirst(userId);
		return result.toArray(new Integer[result.size()]);
	}

	private static List<Integer> getAccessEntities(UserDTO dto) {
		List<Integer> entityIds = new ArrayList<>();
		CompanyDTO company = dto.getEntity();
		while (company != null) {
			entityIds.add(company.getId());
			company = company.getParent();
		}
		return entityIds;
	}

	public BigDecimal getTotalBalanceByInvoiceIds(Integer[] invoiceIds) {
		return invoiceDas.getTotalBalanceByInvoiceIds(invoiceIds);
	}

	/**
	 * Returns List of InvoiceId where billing process id is null.
	 *
	 * @param entityId
	 * @param linkingStartDate
	 * @return
	 */
	public Integer[] getBillingProcessUnlinkedInvoices(Integer entityId, Date linkingStartDate) {
		return invoiceDas.getBillingProcessUnlinkedInvoices(entityId, linkingStartDate).toArray(new Integer[0]);
	}

	public BigDecimal getTotalAmountOwed(Integer userId,Date ageingDate) {
		CurrencyBL currencyBL = new CurrencyBL();
		UserDTO user = new UserDAS().find(userId);

		return new InvoiceDAS().findTotalBalanceByUser(userId,ageingDate)
				.stream()
				.map(totalBalance -> currencyBL.convert(totalBalance.getCurrency(),
						user.getCurrencyId(),
						totalBalance.getBalance(),
						TimezoneHelper.serverCurrentDate(),
						user.getEntity().getId()))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public InvoiceWS[] getAllInvoicesForUser(Integer userId, Integer languageId) {
		ScrollableResults result = null;
		try {
			List<InvoiceWS> invoices = new ArrayList<>();
			result = invoiceDas.getAllInvoicesForUser(userId);
			int count = 0;
			while(result.next()) {
				InvoiceDTO invoiceDbRecord  = (InvoiceDTO) result.get()[0];
				InvoiceWS invoiceWS = InvoiceBL.getWS(invoiceDbRecord);
				if (null != invoiceDbRecord.getInvoiceStatus()) {
					invoiceWS.setStatusDescr(invoiceDbRecord.getInvoiceStatus().getDescription(languageId));
				}
				invoices.add(invoiceWS);
				if(++count % 10 == 0) {
					SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
					Session session = sf.getCurrentSession();
					session.clear(); // only clearing session since it is read only api.
				}
			}
			return invoices.toArray(new InvoiceWS[0]);
		} catch(Exception ex) {
			String msg = "Exception in web service: getAllInvoicesForUser";
			throw new SessionInternalError(msg, ex);
		} finally {
			if(result!=null) {
				result.close();
			}
		}
	}

	public InvoiceWS[] getAllInvoices(Integer languageId) {
		List<InvoiceDTO> invoices = invoiceDas.findAll();
		List<InvoiceWS> ids = new ArrayList<>(invoices.size());
		for (InvoiceDTO invoiceDbRecord : invoices) {
			InvoiceWS wsdto = InvoiceBL.getWS(invoiceDbRecord);
			if (null != invoiceDbRecord.getInvoiceStatus()) {
				wsdto.setStatusDescr(invoiceDbRecord.getInvoiceStatus().getDescription(languageId));
			}
			ids.add(wsdto);
		}
		return ids.toArray(new InvoiceWS[ids.size()]);
	}

	public void updateInvoiceForFailedPayments(PaymentWS payment){
		for (PaymentInvoiceMapDTO paymentInvoiceMapDTO: invoice.getPaymentMap()){
			if (paymentInvoiceMapDTO.getPayment().getId() == payment.getId()){
				paymentInvoiceMapDTO.setAmount(BigDecimal.ZERO);
			}
		}

		invoice.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_UNPAID));
		invoice.setBalance(invoice.getBalance().add(payment.getAmountAsDecimal()));
	}

	private void triggerEInvoicePlugin(Integer entityId, InvoiceDeletedEvent invoiceDeletedEvent) {
		try {
			// load eInvoiceProvider plugins.
			PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS = new PluggableTaskTypeCategoryDAS();
			int eInvoiceProviderPluginTypeId = pluggableTaskTypeCategoryDAS.findByInterfaceName(EINVOICE_PLUGIN_INTERFACE_NAME).getId();
			PluggableTaskManager<IEInvoiceProvider> taskManager = new PluggableTaskManager<>(entityId, eInvoiceProviderPluginTypeId);
			IEInvoiceProvider task = taskManager.getNextClass();
			while(null!= task) {
				task.cancelEInvoice(invoiceDeletedEvent);
				task = taskManager.getNextClass(); // fetch next task.
			}
		} catch(PluggableTaskException pluggableTaskException) {
			throw new SessionInternalError("eInvoice cancel failed for invoice"+ invoiceDeletedEvent
					.getInvoice().getId(), pluggableTaskException);
		}
	}
}
