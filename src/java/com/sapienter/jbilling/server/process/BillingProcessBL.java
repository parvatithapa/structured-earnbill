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

package com.sapienter.jbilling.server.process;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import org.hibernate.ScrollableResults;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.batch.billing.BillingBatchService;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider;
import com.sapienter.jbilling.server.creditnote.CreditNoteBL;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryBL;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.TimePeriod;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.OrderAddedOnInvoiceEvent;
import com.sapienter.jbilling.server.order.event.OrderToInvoiceEvent;
import com.sapienter.jbilling.server.order.event.ProcessTaxLineOnInvoiceEvent;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.InvoiceFilterTask;
import com.sapienter.jbilling.server.pluggableTask.OrderFilterTask;
import com.sapienter.jbilling.server.pluggableTask.OrderPeriodTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessFailedUserDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessInfoDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessInfoDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunStatusDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunTotalDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunUserDAS;
import com.sapienter.jbilling.server.process.event.AboutToGenerateInvoices;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryBL;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

public class BillingProcessBL extends ResultList implements ProcessSQL {

	private static final String EINVOICE_PLUGIN_INTERFACE_NAME = "com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider";

	private BillingProcessDAS billingProcessDas = null;
	private BillingProcessDTO billingProcess = null;
	private ProcessRunDAS processRunHome = null;
	private ProcessRunDTO processRun = null;
	private static final Logger logger = LoggerFactory.getLogger(BillingProcessBL.class);
	private EventLogger eLogger = null;

	public BillingProcessBL(Integer billingProcessId) {
		init();
		set(billingProcessId);
	}

	public BillingProcessBL() {
		init();
	}

	public BillingProcessBL(BillingProcessDTO row) {
		init();
		billingProcess = row;
	}

	private void init() {
		eLogger = EventLogger.getInstance();
		billingProcessDas = new BillingProcessDAS();

		// now create the run info row
		processRunHome = new ProcessRunDAS();
	}

	public BillingProcessDTO getEntity() {
		return billingProcess;
	}

	public ProcessRunDTO getProcessRun() {
		return processRun;
	}

	public BillingProcessDAS getHome() {
		return billingProcessDas;
	}

	public void set(Integer id) {
		billingProcess = billingProcessDas.findNow(id);
	}

	public void set(BillingProcessDTO pEntity) {
		billingProcess = pEntity;
	}

	public Integer findOrCreate(BillingProcessDTO dto) {
		billingProcess = billingProcessDas.isPresent(dto.getEntity().getId(), dto.getIsReview(),
				dto.getBillingDate());
		if (billingProcess == null) {
			create(dto);
		}

		return billingProcess.getId();
	}

	public Integer create(BillingProcessDTO dto) {
		// create the record
		billingProcess = billingProcessDas.create(dto.getEntity(),
				dto.getBillingDate(), dto.getPeriodUnit().getId(),
				dto.getPeriodValue(), dto.getRetriesToDo());
		billingProcess.setIsReview(dto.getIsReview());
		processRun = processRunHome.create(
				billingProcess, dto.getBillingDate(), 0,
				new ProcessRunStatusDAS().find(Constants.PROCESS_RUN_STATUS_RINNING));

		if (dto.getIsReview() == 1) {
			ConfigurationBL config = new ConfigurationBL(dto.getEntity().getId());
			config.getEntity().setReviewStatus(Constants.REVIEW_STATUS_GENERATED);
		}

		return billingProcess.getId();
	}


	public InvoiceDTO generateInvoice(Integer orderId, Integer invoiceId, Integer executorUserId)
			throws PluggableTaskException, SessionInternalError, SQLException {

		return generateInvoice(orderId, invoiceId, null, executorUserId);
	}

	public InvoiceDTO generateInvoice(Integer orderId, Integer invoiceId, NewInvoiceContext template, Integer executorUserId)
			throws PluggableTaskException, SessionInternalError,
			SQLException {
		return generateInvoice(orderId, invoiceId, template, executorUserId, null);
	}

	/**
	 * Generates one single invoice for one single purchase order. This is
	 * meant to be called outside the billing process.
	 * @param orderId
	 * @return
	 * @throws PluggableTaskException
	 * @throws SessionInternalError
	 */
	public InvoiceDTO generateInvoice(Integer orderId, Integer invoiceId, NewInvoiceContext template, Integer executorUserId,
			Date viewLimit) throws PluggableTaskException, SessionInternalError, SQLException {
		InvoiceDTO retValue = null;
		// find the order
		OrderBL order = new OrderBL(orderId);
		// define some data
		Integer entityId = order.getDTO().getUser().getEntity().getId();
		ConfigurationBL config = new ConfigurationBL(entityId);
		int maxPeriods = config.getEntity().getMaximumPeriods();
		boolean paymentApplication = config.getEntity().getAutoPaymentApplication() == 1;
		boolean creditNoteApplication = config.getEntity().getAutoCreditNoteApplication() == 1;
		// The user could be the parent of a sub-account
		Integer userId = findUserId(order.getDTO());
		Date processDate = TimezoneHelper.companyCurrentDate(entityId);
		processDate = Util.truncateDate(processDate);

		// create the my invoice
		NewInvoiceContext newInvoice = new NewInvoiceContext();
		String orderNotes = order.getDTO().getNotes();
		String[] notes = orderNotes!=null && !orderNotes.isEmpty() ? orderNotes.split(",") : new String[0];
		if(notes.length > 0 && notes[0].contains(":")) {
			String referenceNumber = notes[0].split(":")[1];
			if(referenceNumber!=null && !referenceNumber.isEmpty()) {
				newInvoice.setDate(order.getDTO().getActiveSince());
			}
		}else{
			newInvoice.setDate(processDate);
		}

		newInvoice.setIsReview(0);
		// find the due date that applies
		TimePeriod period = order.getDueDate();
		newInvoice.setDueDatePeriod(period);
		// this is an isolated invoice that doesn't care about previous
		// overdue invoices
		newInvoice.setCarriedBalance(BigDecimal.ZERO);
		newInvoice.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_UNPAID));

		// set meta fields
		if (template != null) {
			newInvoice.setMetaFields(template.getMetaFields());
			logger.debug("Set meta fields from template: {}", newInvoice.getMetaFields());
		}

		if(invoiceId!=null){
			// it is an order going into an existing invoice
			InvoiceBL invoice = new InvoiceBL(invoiceId);
			// then new Invoice must be populated with currency before order processing.
			newInvoice.setCurrency(invoice.getEntity().getCurrency());
		}

		try {
			// put the order in the invoice using all the pluggable task stuff
			addOrderToInvoice(entityId, order.getDTO(), newInvoice, processDate, maxPeriods, viewLimit);

			// this means that the user is trying to generate an invoice from
			// an order that the configured tasks have rejected. Therefore
			// either this is the case an generating this invoice doesn't make
			// sense, or some business rules in the tasks have to be changed
			// (probably with a personalized task for this entity)
			if (newInvoice.getOrders().size() == 0) {
				return null;
			}

			// process events before orders added to invoice
			processOrderToInvoiceEvents(newInvoice, entityId);

			// generate the invoice lines
			composeInvoice(entityId, userId, newInvoice);

			// process events after orders added to invoice
			processOrderAddedOnInvoiceEvents(newInvoice, entityId);
			processTaxesAndPenalties(newInvoice, entityId, userId);

			// put the resulting invoice in the database
			if (invoiceId == null) {
				// it is a new invoice from a singe order
				retValue = generateDBInvoice(userId, newInvoice, null,
						Constants.ORDER_PROCESS_ORIGIN_MANUAL, executorUserId);
				this.applyPaymentsAndCreditNotesToInvoice(paymentApplication, creditNoteApplication, retValue);
			} else {
				// it is an order going into an existing invoice
				InvoiceBL invoice = new InvoiceBL(invoiceId);
				boolean isUnpaid = invoice.getEntity().getToProcess() == 1;

				logger.debug("Calling update with new invoice... meta fields = {}", newInvoice.getMetaFields());
				invoice.update(entityId, newInvoice);


				retValue = invoice.getEntity();
				createOrderProcess(newInvoice, retValue, null, Constants.ORDER_PROCESS_ORIGIN_MANUAL, executorUserId);

				if (executorUserId != null) {
					eLogger.audit(executorUserId, userId,
							Constants.TABLE_INVOICE, invoiceId,
							EventLogger.MODULE_INVOICE_MAINTENANCE,
							EventLogger.INVOICE_ORDER_APPLIED, null, null, null);
				}

				// if the invoice is now not payable, take the user
				// out of ageing
				if (isUnpaid && retValue.getToProcess() == 0) {
					AgeingBL ageing = new AgeingBL();
					ageing.out(retValue.getBaseUser(), null, processDate);
				}
			}
		} catch (TaskException e) {
			// this means that the user is trying to generate an invoice from
			// an order that the configurated tasks have rejected. Therefore
			// either this is the case an generating this invoice doesn't make
			// sense, or some business rules in the tasks have to be changed
			// (probably with a personalized task for this entity)
			logger.warn("Exception in generate invoice ", e);
		}

		if (retValue != null) {
			InvoicesGeneratedEvent generatedEvent = new InvoicesGeneratedEvent(entityId, null);
			generatedEvent.getInvoiceIds().add(retValue.getId());
			EventManager.process(generatedEvent);
			// execute eInvoiceProvider plugin.
			triggerEInvoicePlugin(entityId, generatedEvent);
		}

		return retValue;
	}

	private void triggerEInvoicePlugin(Integer entityId, InvoicesGeneratedEvent invoicesGeneratedEvent) throws PluggableTaskException {
		// load eInvoiceProvider plugins.
		PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS = new PluggableTaskTypeCategoryDAS();
		int eInvoiceProviderPluginTypeId = pluggableTaskTypeCategoryDAS.findByInterfaceName(EINVOICE_PLUGIN_INTERFACE_NAME).getId();
		PluggableTaskManager<IEInvoiceProvider> taskManager = new PluggableTaskManager<>(entityId, eInvoiceProviderPluginTypeId);
		IEInvoiceProvider task = taskManager.getNextClass();
		while(null!= task) {
			task.generateEInvoice(invoicesGeneratedEvent);
			task = taskManager.getNextClass(); // fetch next task.
		}
	}

	/**
	 * Generates an invoice for a user using the given billing process details.
	 *
	 * @param process billing process to generate an invoice for
	 * @param dueDatePeriod optional due date period for this invoice. If null, due date will be calculated based
	 *                      on the order and/or customer due date period.
	 * @param user user to generate an invoice for
	 * @param isReview true if generating a review invoice
	 * @param onlyRecurring true if only recurring orders should be invoiced. if false, all orders will be included.
	 * @param executorUserId user to use for event logging.
	 * @return array of generated invoices
	 * @throws SessionInternalError
	 */
	public InvoiceDTO[] generateInvoice(BillingProcessDTO process, TimePeriod dueDatePeriod, UserDTO user,
			boolean isReview, boolean onlyRecurring, Integer executorUserId) throws SessionInternalError {
		Integer userId = user.getUserId();
		Integer entityId = user.getEntity().getId();
		// get the configuration
		boolean useCustomerNextInvoiceDateForInvoice = true;
		int maximumPeriods = 1;
		boolean paymentApplication = false;
		boolean creditNoteApplication = false;
		ConfigurationBL config;
		try {
			config = new ConfigurationBL(process.getEntity().getId());
			useCustomerNextInvoiceDateForInvoice = config.getEntity().getInvoiceDateProcess() == 1;
			maximumPeriods = config.getEntity().getMaximumPeriods();
			paymentApplication = config.getEntity().getAutoPaymentApplication() == 1;
			creditNoteApplication = config.getEntity().getAutoCreditNoteApplication() == 1;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		try {
			if (!isReview) {
				this.handlePaymentsAndCreditNotesApplication(paymentApplication, creditNoteApplication, user);
			}
		} catch (Exception e) {
			logger.error("Error, probably linking payments", e);
			throw new SessionInternalError(e);
		}
		// this contains the generated invoices, one per due date
		// found in the applicable purchase orders.
		// The key is the object TimePeriod
		Hashtable<TimePeriod, NewInvoiceContext> newInvoices = new Hashtable<>();
		InvoiceDTO[] retValue;

		logger.debug("Firing About to Generate Invoices event.");

		AboutToGenerateInvoices event = new AboutToGenerateInvoices(entityId, userId, new Date(process.getBillingDate().getTime()));
		EventManager.process(event);

		logger.debug("In generateInvoice for user {} process date: {}", userId, process.getBillingDate());

		/*
		 * Go through the orders first
		 * This method will recursively call itself to find sub-accounts in any
		 * level
		 */
		boolean includedOrders = processOrdersForUser(user, entityId, process, dueDatePeriod,
				isReview, onlyRecurring, useCustomerNextInvoiceDateForInvoice,
				maximumPeriods, newInvoices);

		if (!includedOrders || newInvoices.size() == 0) {
			// check if invoices without orders are allowed
			int preferenceAllowInvoicesWithoutOrders = 0;
			try {
				preferenceAllowInvoicesWithoutOrders = PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_ALLOW_INVOICES_WITHOUT_ORDERS);
			} catch (EmptyResultDataAccessException fe) {
				// use default
			}

			if (preferenceAllowInvoicesWithoutOrders == 0) {
				logger.debug("No applicable orders. No invoice generated (skipping invoices).");
				return null;
			}
		}

		if (!isReview) {
			for (Map.Entry<TimePeriod, NewInvoiceContext> newInvoiceEntry : newInvoices.entrySet()) {
				// process events before orders added to invoice
				processOrderToInvoiceEvents(newInvoiceEntry.getValue(), entityId);
			}
		}

		/*
		 * Include those invoices that should've been paid
		 * (or have negative balance, as credits)
		 */
		logger.debug("Considering overdue invoices");
		// find the invoice home interface
		InvoiceDAS invoiceDas = new InvoiceDAS();
		// any of the new invoices being created could hold the overdue invoices
		NewInvoiceContext holder = newInvoices.isEmpty() ? null : newInvoices.elements().nextElement();

		Collection dueInvoices = invoiceDas.findWithBalanceByUser(user);
		logger.debug("Processing invoices for user {}", user.getUserId());
		// go through each of them, and update the DTO if it applies

		for (Iterator it = dueInvoices.iterator(); it.hasNext();) {
			InvoiceDTO invoice = (InvoiceDTO) it.next();
			logger.debug("Processing invoice {}", invoice.getId());
			// apply any invoice processing filter pluggable task
			try {
				PluggableTaskManager taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_INVOICE_FILTER);
				InvoiceFilterTask task = (InvoiceFilterTask) taskManager.getNextClass();
				boolean isProcessable = true;
				while (task != null) {
					isProcessable = task.isApplicable(invoice, process);
					if (!isProcessable) {
						break; // no need to keep doing more tests
					}
					task = (InvoiceFilterTask) taskManager.getNextClass();
				}

				// include this invoice only if it complies with all the rules
				if (isProcessable) {
					// check for an invoice
					if (holder == null) {
						// Since there are no new invoices (therefore no orders),
						// don't let invoices with positive balances generate
						// an invoice.
						if (BigDecimal.ZERO.compareTo(invoice.getBalance()) < 0) {
							continue;
						}

						// no invoice/s yet (no applicable orders), so create one
						holder = new NewInvoiceContext();
						holder.setDate(process.getBillingDate());
						holder.setIsReview(isReview ? new Integer(1) : new Integer(0));
						holder.setCarriedBalance(BigDecimal.ZERO);
						holder.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_UNPAID));

						// need to set a due date, so use the order default unless
						// it has been explicitly set for this invoice generation
						if (dueDatePeriod == null) {
							OrderDTO order = new OrderDTO();
							order.setBaseUserByUserId(user);

							OrderBL orderBl = new OrderBL();
							orderBl.set(order);
							dueDatePeriod = orderBl.getDueDate();
						}

						holder.setDueDatePeriod(dueDatePeriod);
						newInvoices.put(dueDatePeriod, holder);
					}

					InvoiceBL ibl = new InvoiceBL(invoice);
					holder.addInvoice(ibl.getDTO());
					// for those invoices wiht only overdue invoices, the
					// currency has to be initialized

					if (holder.getCurrency() == null) {
						holder.setCurrency(invoice.getCurrency());
					} else if (holder.getCurrency().getId() != invoice.getCurrency().getId()) {
						throw new SessionInternalError("An invoice with different currency is not supported. " +
								"Currency = " + holder.getCurrency().getId() + "invoice = " + invoice.getId());
					}
					// update the amount of the new invoice that is due to
					// previously unpaid overdue invoices

					// carry the remaining balance, plus the previously carried balance to the new invoice
					BigDecimal balance = (invoice.getBalance() == null) ? BigDecimal.ZERO : invoice.getBalance();
					BigDecimal carried = balance.add(holder.getCarriedBalance());
					holder.setCarriedBalance(carried);
				}

				logger.debug("invoice {} result {}", invoice.getId(), isProcessable);

			} catch (PluggableTaskException e) {
				logger.debug("Problems handling task invoice filter.", e);
				throw new SessionInternalError("Problems handling task invoice filter.");
			} catch (TaskException e) {
				logger.debug("Problems excecuting task invoice filter.", e);
				throw new SessionInternalError("Problems executing task invoice filter.");
			}

		}

		if (newInvoices.size() == 0) {
			// no orders or invoices for this invoice
			logger.debug("No applicable orders or invoices. No invoice generated (skipping invoices).");
			return null;
		}

		try {
			List<InvoiceDTO> retValueList = new ArrayList<>();
			for (NewInvoiceContext invoice : newInvoices.values()) {
				/*
				 * Apply invoice composition tasks to the new invoices object
				 */
				composeInvoice(entityId, user.getUserId(), invoice);

				if (!isReview) {
					// process events after orders added to invoice
					processOrderAddedOnInvoiceEvents(invoice, entityId);
					processTaxesAndPenalties(invoice, entityId, user.getUserId());

					for (InvoiceDTO oldInvoice : invoice.getInvoices()) {
						// since this invoice is being delegated, mark it as being carried forward
						// so that it is not re-processed later. do not clear the old balance!
						oldInvoice.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_UNPAID_AND_CARRIED));
					}
				}

				/*
				 * apply this object to the DB, generating the actual rows
				 */
				// only if the invoice generated actually has some lines in it
				if (invoice.areLinesGeneratedEmpty()) {
					logger.warn("User {} had orders or invoices but the invoice composition task didn't generate any lines.", user.getUserId());
					continue;
				}

				// If this is a web services API call, the billing
				// process id is 0. Don't link to the billing process
				// object for API calls.
				InvoiceDTO generateDBInvoice = generateDBInvoice(user.getUserId(),
						invoice,
						(process.getId() != 0 ? process : null),
						Constants.ORDER_PROCESS_ORIGIN_PROCESS, executorUserId);
				if (!isReview) {
					applyPaymentsAndCreditNotesToInvoice(paymentApplication, creditNoteApplication, generateDBInvoice);
				}
				retValueList.add(generateDBInvoice);
			}
			retValue = retValueList.toArray(new InvoiceDTO[retValueList.size()]);
		} catch (PluggableTaskException e1) {
			logger.error("Error handling pluggable tasks when composing an invoice");
			throw new SessionInternalError(e1);
		} catch (TaskException e1) {
			logger.error("Task exception when composing an invoice");
			throw new SessionInternalError(e1);
		} catch (Exception e1) {
			logger.error("Error, probably linking payments", e1);
			throw new SessionInternalError(e1);
		}

		InvoicesGeneratedEvent generatedEvent = new InvoicesGeneratedEvent(entityId, process.getId());
		generatedEvent.addInvoices(retValue);
		EventManager.process(generatedEvent);
		try {
			if(!isReview) {
				triggerEInvoicePlugin(entityId, generatedEvent);
			}
		} catch(PluggableTaskException pluggableTaskException) {
			logger.error("Task exception while sending invoice to gst portal");
			throw new SessionInternalError(pluggableTaskException);
		}
		return retValue;
	}

	private void handlePaymentsAndCreditNotesApplication(boolean paymentApplication, boolean creditNoteApplication, UserDTO user) throws SQLException {

		for (InvoiceDTO invoice : new InvoiceDAS().findProccesableByUser(user)) {
			applyPaymentsAndCreditNotesToInvoice(paymentApplication, creditNoteApplication, invoice);
			// if it was not enough to pay off this one
			if (invoice.getBalance().compareTo(BigDecimal.ZERO) > 0) {
				break;
			}
		}
	}

	private void applyPaymentsAndCreditNotesToInvoice(boolean paymentApplication, boolean creditNoteApplication,InvoiceDTO invoice) throws SQLException {

		// try to get this invoice paid by previously generated credit notes
		if (creditNoteApplication) {
			new CreditNoteBL().applyExistingCreditNotesToInvoice(invoice);
		}
		if (invoice.getBalance().compareTo(BigDecimal.ZERO) > 0) {
			// try to get this invoice paid by previously unlinked
			// payments
			if (paymentApplication) {
				PaymentBL pBL = new PaymentBL();
				pBL.automaticPaymentApplication(invoice);
			}
		}
	}

	private boolean processOrdersForUser(UserDTO user, Integer entityId,BillingProcessDTO process, TimePeriod dueDatePeriod,
			boolean isReview, boolean onlyRecurring, boolean useCustomerNextInvoiceDateForInvoice,
			int maximumPeriods, Hashtable<TimePeriod, NewInvoiceContext> newInvoices) {
		boolean includedOrders = false;
		Integer userId = user.getUserId();
		logger.debug("Processing orders for user {}", userId);

		// initialize the subaccounts iterator if this user is a parent
		Iterator subAccountsIt = null;
		if (user.getCustomer().getIsParent() != null && user.getCustomer().getIsParent() == 1) {
			UserBL parent = new UserBL(userId);
			subAccountsIt = parent.getEntity().getCustomer().getChildren().iterator();
		}

		// get the orders that might be processable for this user
		OrderDAS orderDas = new OrderDAS();
		ScrollableResults orders = orderDas.findByUser_Status(userId, OrderStatusFlag.INVOICE, OrderStatusFlag.SUSPENDED_AGEING);

		try {
			// go through each of them, and update the DTO if it applies
			while (orders.next()) {
				OrderDTO order = (OrderDTO) orders.get()[0];

				logger.debug("Processing order :{}", order.getId());
				// apply any order processing filter pluggable task
				try {
					PluggableTaskManager taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_ORDER_FILTER);
					OrderFilterTask task = (OrderFilterTask) taskManager.getNextClass();
					boolean isProcessable = true;
					while (task != null) {
						isProcessable = task.isApplicable(order, process);
						if (!isProcessable) {
							break; // no need to keep doing more tests
						}
						task = (OrderFilterTask) taskManager.getNextClass();
					}

					// include this order only if it complies with all the
					// rules
					if (isProcessable) {

						logger.debug("Order processable");

						if (onlyRecurring) {
							if (order.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE) {
								includedOrders = true;
								logger.debug("It is not one-timer. Generating invoice");
							}
						} else {
							includedOrders = true;
						}
						/*
						 * now find if there is already an invoice being
						 * generated for the given due date period
						 */

						// find the due date that applies to this order
						OrderBL orderBl = new OrderBL();
						orderBl.set(order);
						TimePeriod orderDueDatePeriod = dueDatePeriod != null ? dueDatePeriod : orderBl.getDueDate();

						// look it up in the hashtable
						NewInvoiceContext thisInvoice = newInvoices.get(orderDueDatePeriod);
						if (thisInvoice == null) {
							logger.debug("Adding new invoice for period {} process date: {}", orderDueDatePeriod, process.getBillingDate());
							// we need a new one with this period
							// define the invoice date
							thisInvoice = new NewInvoiceContext();
							if (useCustomerNextInvoiceDateForInvoice) {
								thisInvoice.setDate(user.getCustomer().getNextInvoiceDate());
							} else {
								thisInvoice.setDate(process.getBillingDate());
							}

							thisInvoice.setIsReview(isReview ? new Integer(1) : new Integer(0));
							thisInvoice.setCarriedBalance(BigDecimal.ZERO);
							thisInvoice.setDueDatePeriod(orderDueDatePeriod);
						} else {
							logger.debug("invoice found for period {}", dueDatePeriod);
							if (!useCustomerNextInvoiceDateForInvoice) {
								thisInvoice.setDate(process.getBillingDate());
							}
						}
						/*
						 * The order periods plug-in might not add any period. This should not happen
						 * but if it does, the invoice should not be included
						 */
						boolean oneTimeCheck;
						if (onlyRecurring && order.getOrderPeriod().getId() ==  Constants.ORDER_PERIOD_ONCE) {
							oneTimeCheck= isBillableOneTime(order, thisInvoice, process.getBillingDate());
						} else {
							oneTimeCheck= true;//check not required
						}


						LocalDate processDate = DateConvertUtils.asLocalDate(new UserBL(userId).getCustomerNextInvoiceDate(user));
						processDate = processDate.plusDays(-1);

						if (oneTimeCheck && addOrderToInvoice(entityId, order, thisInvoice,
								DateConvertUtils.asUtilDate(processDate), maximumPeriods)) {
							// add or replace
							newInvoices.put(orderDueDatePeriod, thisInvoice);
						}
						logger.debug("After putting period there are {} periods.", newInvoices.size());

						// here it would be easy to update this order
						// to_process and
						// next_billable_time. I can't do that because these
						// fields
						// will be read by the following tasks, and they
						// will asume
						// they are not modified.
					}

				} catch (PluggableTaskException e) {
					logger.debug("Problems handling order filter task.", e);
					throw new SessionInternalError(
							"Problems handling order filter task.");
				} catch (TaskException e) {
					logger.debug("Problems excecuting order filter task.", e);
					throw new SessionInternalError(
							"Problems executing order filter task.");
				}
			}
		} finally {
			orders.close();
		}

		// see if there is any subaccounts to include in this invoice
		while (subAccountsIt != null) {  // until there are no more subaccounts (subAccountsIt != null) {
			CustomerDTO customer = null;
			while (subAccountsIt.hasNext()) {
				customer = (CustomerDTO) subAccountsIt.next();
				if (customer.getInvoiceChild() == null ||
						customer.getInvoiceChild() == 0) {
					break;
				} else {
					logger.debug("Subaccount not included in parent's invoice {}", customer.getId());
					customer = null;
				}
			}
			if (customer != null) {
				userId = customer.getBaseUser().getUserId();
				// if the child does not have any orders to invoice, this should
				// not affect the current value of includedOrders
				if (processOrdersForUser(customer.getBaseUser(),
						entityId, process, dueDatePeriod, isReview, onlyRecurring,
						useCustomerNextInvoiceDateForInvoice, maximumPeriods,
						newInvoices)) {
					// if ANY child has orders to invoice, it is enough for includedOrders to be true
					includedOrders = true;
				}
				logger.debug("Now processing subaccount {}", userId);

			} else {
				subAccountsIt = null;
				logger.debug("No more subaccounts to process");
			}
		}

		return includedOrders;
	}

	private boolean isBillableOneTime(OrderDTO order, NewInvoiceContext newInvoice, Date processDate) {
		logger.debug("Any one period on this Invoice should incldue the one time order's active since");
		Date start= order.getActiveSince();
		Date end= processDate;
		for (NewInvoiceContext.OrderContext orderCtx : newInvoice.getOrders()) {
			if ( orderCtx.order.getOrderPeriod().getId() !=  Constants.ORDER_PERIOD_ONCE.intValue() ) {
				for (PeriodOfTime period : orderCtx.periods) {
					if (period.getStart().before(start)) {
						start= period.getStart();
					}
					if (period.getEnd().after(end)) {
						end= period.getEnd();
					}
				}
			}
		}

		Interval interval= new Interval(new DateTime(start.getTime()), new DateTime(end.getTime()));
		return interval.contains(order.getActiveSince().getTime());
	}

	private InvoiceDTO generateDBInvoice(Integer userId,
			NewInvoiceContext newInvoice, BillingProcessDTO process,
			Integer origin, Integer executorUserId)
					throws SessionInternalError {
		// The invoice row is created first
		// all that fits in the DTO goes there
		newInvoice.calculateTotal();

		if (newInvoice.getCarriedBalance() != null) {
			newInvoice.setBalance(newInvoice.getTotal().subtract(newInvoice.getCarriedBalance()));
		} else {
			newInvoice.setBalance(newInvoice.getTotal());
		}

		newInvoice.setInProcessPayment(1);
		InvoiceBL invoiceBL = new InvoiceBL();

		try {
			invoiceBL.create(userId, newInvoice, process, executorUserId);
			invoiceBL.createLines(newInvoice);
		} catch (Exception e) {
			logger.debug("CreateException creating invoice record", e);
			throw new SessionInternalError("Couldn't create the invoice record");
		}

		createOrderProcess(newInvoice, invoiceBL.getEntity(), process,
				origin, executorUserId);

		return invoiceBL.getEntity();

	}

	private void createOrderProcess(NewInvoiceContext newInvoice,
			InvoiceDTO invoice, BillingProcessDTO process,
			Integer origin, Integer executorId)
					throws SessionInternalError {

		logger.debug("Generating order process records...");

		// update the orders involved, now that their old data is not needed
		// anymore
		for (NewInvoiceContext.OrderContext orderCtx : newInvoice.getOrders()) {

			OrderDTO order = orderCtx.order;

			logger.debug(" ... order {}", order.getId());

			// this will help later
			List<PeriodOfTime> periodsList = orderCtx.periods;
			Date startOfBillingPeriod = periodsList.get(0).getStart();
			Date endOfBillingPeriod = periodsList.get(periodsList.size() - 1).getEnd();
			Integer periods = orderCtx.periods.size();

			// We don't update orders if this is just a review
			if (newInvoice.getIsReview() == 0) {
				// update the to_process if applicable
				updateStatusFinished(order, startOfBillingPeriod, endOfBillingPeriod, executorId);

				// update this order process field
				updateNextBillableDay(order, endOfBillingPeriod);
			}

			// create the period and update the order-invoice relationship
			try {

				OrderProcessDAS das = new OrderProcessDAS();
				OrderProcessDTO orderProcess = new OrderProcessDTO();
				orderProcess.setPeriodStart(startOfBillingPeriod);
				orderProcess.setPeriodEnd(endOfBillingPeriod);
				orderProcess.setIsReview(newInvoice.getIsReview());
				orderProcess.setPurchaseOrder(order);

				InvoiceDAS invDas = new InvoiceDAS();
				orderProcess.setInvoice(invDas.find(invoice.getId()));

				BillingProcessDAS proDas = new BillingProcessDAS();
				orderProcess.setBillingProcess(process != null ? proDas.find(process.getId()) : null);
				orderProcess.setPeriodsIncluded(periods);
				orderProcess.setOrigin(origin);
				orderProcess = das.save(orderProcess);
				invoice.getOrderProcesses().add(orderProcess);

				logger.debug("created order process id {} for order {}", orderProcess.getId(), order.getId());

			} catch (Exception e) {
				throw new SessionInternalError(e);
			}
		}
	}

	private void composeInvoice(Integer entityId, Integer userId, NewInvoiceContext newInvoice) throws PluggableTaskException,
	TaskException,
	SessionInternalError {
		newInvoice.setEntityId(entityId);
		PluggableTaskManager taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_INVOICE_COMPOSITION);
		InvoiceCompositionTask task = (InvoiceCompositionTask) taskManager.getNextClass();
		while (task != null) {
			task.apply(newInvoice, userId);
			task = (InvoiceCompositionTask) taskManager.getNextClass();
		}

		String validationMessage = newInvoice.validate();
		if (validationMessage != null) {
			logger.error("Composing invoice for entity {} invalid new invoice object: {}", entityId, validationMessage);
			throw new SessionInternalError("NewInvoiceContext:" + validationMessage);
		}
	}
	private boolean addOrderToInvoice(Integer entityId, OrderDTO order,
			NewInvoiceContext newInvoice, Date processDate, int maxPeriods)
					throws SessionInternalError, TaskException,
					PluggableTaskException {
		return addOrderToInvoice(entityId, order, newInvoice, processDate, maxPeriods, null);
	}

	private boolean addOrderToInvoice(Integer entityId, OrderDTO order,
			NewInvoiceContext newInvoice, Date processDate, int maxPeriods, Date viewLimit)
					throws SessionInternalError, TaskException,
					PluggableTaskException {
		// require the calculation of the period dates
		PluggableTaskManager taskManager = new PluggableTaskManager(
				entityId, Constants.PLUGGABLE_TASK_ORDER_PERIODS);
		OrderPeriodTask optask = (OrderPeriodTask) taskManager.getNextClass();

		if (optask == null) {
			throw new SessionInternalError("There has to be " +
					"one order period pluggable task configured");
		}
		Date start = optask.calculateStart(order);
		Date end = optask.calculateEnd(order, processDate, maxPeriods, start, viewLimit);
		List<PeriodOfTime> periods = optask.getPeriods();
		// there isn't anything billable from this order
		if (periods.isEmpty()) {
			return false;
		}

		if (start != null && end != null && start.after(end)) {
			// how come it starts after it ends ???
			throw new SessionInternalError("Calculated for " +
					"order " + order.getId() + " a period that" + " starts after it ends:" + start + " " +
					end);
		}

		// add this order to the invoice being created
		newInvoice.addOrder(order, start, end, periods);


		// prepaid orders shouldn't have to be included
		// past time.
		if (order.getBillingTypeId().compareTo(
				Constants.ORDER_BILLING_PRE_PAID) == 0 &&
				start != null && // it has to be recursive too
				processDate.after(start)) {

			eLogger.warning(entityId, order.getBaseUserByUserId().getId(),
					order.getId(), EventLogger.MODULE_BILLING_PROCESS,
					EventLogger.BILLING_PROCESS_UNBILLED_PERIOD,
					Constants.TABLE_PUCHASE_ORDER);

			logger.warn("Order {} is prepaid but has past time not billed.", order.getId());
		}

		// initialize the currency of the new invoice
		if (newInvoice.getCurrency() == null) {
			newInvoice.setCurrency(order.getCurrency());
		} else {
			// now we are not supporting orders with different
			// currencies in the same invoice. Later this could be done
			if (newInvoice.getCurrency().getId() != order.getCurrency().getId()) {
				throw new SessionInternalError("Orders with different " +
						"currencies not supported in one invoice. " +
						"Currency = " + newInvoice.getCurrency().getId() +
						"order = " + order.getId());
			}
		}
		return true;
	}

	static void updateStatusFinished(OrderDTO order,
			Date startOfBillingPeriod,
			Date endOfBillingPeriod,
			Integer executorId)
					throws SessionInternalError {

		// all one timers are done
		if (order.getOrderPeriod().getId() == Constants.ORDER_PERIOD_ONCE) {
			OrderBL orderBL = new OrderBL(order);
			orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()));
		} else { // recursive orders get more complicated
			// except those that are immortal :)
			if (order.getActiveUntil() == null) {
				return;
			}
			// see until when the incoming process will cover
			// compare if this is after the order exipres
			Logger log = LoggerFactory.getLogger(BillingProcessBL.class);
			log.debug("order {} end of bp {} active until {}", order.getId(), endOfBillingPeriod, order.getActiveUntil());
			if (endOfBillingPeriod.compareTo(Util.truncateDate(order.getActiveUntil())) > 0) {
				OrderBL orderBL = new OrderBL(order);
				orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()));
			}
		}
	}

	static public Date getEndOfProcessPeriod(BillingProcessDTO process)
			throws SessionInternalError {
		GregorianCalendar cal = new GregorianCalendar();

		cal.setTime(process.getBillingDate());

		if (CalendarUtils.isSemiMonthlyPeriod(process.getPeriodUnit())) {
			cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
		} else {
			cal.add(MapPeriodToCalendar.map(process.getPeriodUnit().getId()),
					process.getPeriodValue());
		}

		return cal.getTime();
	}

	static public void updateNextBillableDay(OrderDTO order,
			Date end) throws SessionInternalError {
		// if this order won't be process ever again, the
		// it shouldn't have a next billable day
		if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
			order.setNextBillableDay(null);
		} else {
			order.setNextBillableDay(end);
		}
		updateNextBillableDayInChanges(order);
	}

	static private void updateNextBillableDayInChanges (OrderDTO order) {
		Date nextBillableDate = order.getNextBillableDay();
		for (OrderLineDTO line: order.getLines()) {
			for (OrderChangeDTO change: line.getOrderChanges()) {
				if ((nextBillableDate == null) || (change.getNextBillableDate() == null) || nextBillableDate.after(change.getNextBillableDate())) {
					change.setNextBillableDate(nextBillableDate);
				}
			}
		}
	}

	public BillingProcessDTOEx getDtoEx(Integer language) {
		BillingProcessDTOEx retValue = getSimpleDtoEx();

		// now add the runs and grand total
		BillingProcessRunDTOEx grandTotal =
				new BillingProcessRunDTOEx();
		int totalInvoices = 0;
		int runsCounter = 0;
		List<BillingProcessRunDTOEx> runs = new ArrayList<>();
		// go throuhg every run
		for (Iterator it = billingProcess.getProcessRuns().iterator();
				it.hasNext();) {
			ProcessRunDTO run = (ProcessRunDTO) it.next();
			BillingProcessRunBL runBL = new BillingProcessRunBL(run);
			BillingProcessRunDTOEx runDto = runBL.getDTO(language);
			runs.add(runDto);
			runsCounter++;

			// add statistic for InProgress run proccess in DTO
			if (run.getPaymentFinished() == null) {
				addRuntimeStatistic(run.getBillingProcess().getId(), language, runDto);
			}

			logger.debug("Run: {} has {} total records.", run.getId(), run.getProcessRunTotals().size());
			// go over the totals, since there's one per currency
			for (Iterator it2 = runDto.getTotals().iterator(); it2.hasNext();) {
				// the total to process
				BillingProcessRunTotalDTOEx totalDto =
						(BillingProcessRunTotalDTOEx) it2.next();

				BillingProcessRunTotalDTOEx sum = getTotal(totalDto.getCurrency(), grandTotal.getTotals());

				BigDecimal totalTmp = totalDto.getTotalInvoiced().add(sum.getTotalInvoiced());
				sum.setTotalInvoiced(totalTmp);

				totalTmp = totalDto.getTotalPaid().add(sum.getTotalPaid());
				sum.setTotalPaid(totalTmp);

				// can't add up the not paid, because many runs will try to
				// get the same invoices paid, so the not paid field gets
				// duplicated amounts.
				totalTmp = sum.getTotalInvoiced().subtract(sum.getTotalPaid());
				sum.setTotalNotPaid(totalTmp);

				// make sure this total has the currency name initialized
				if (sum.getCurrencyName() == null) {
					CurrencyBL currency = new CurrencyBL(sum.getCurrency().getId());
					sum.setCurrencyName(currency.getEntity().getDescription(
							language));
				}
				// add the payment method totals
				for (Enumeration en = totalDto.getPmTotals().keys();
						en.hasMoreElements();) {
					String method = (String) en.nextElement();
					BigDecimal methodTotal = new BigDecimal(totalDto.getPmTotals().get(method).toString());

					if (sum.getPmTotals().containsKey(method)) {
						if (sum.getPmTotals().get(method) != null) {
							methodTotal = methodTotal.add(new BigDecimal((sum.getPmTotals().get(method)).toString()));

						}
					}
					sum.getPmTotals().put(method, methodTotal.floatValue());
				}

				logger.debug("Added total to run dto. PMs in total:{} now grandTotal totals: {}", sum.getPmTotals().size(), grandTotal.getTotals().size());
			}
			totalInvoices += runDto.getInvoicesGenerated();
		}

		grandTotal.setInvoicesGenerated(totalInvoices);

		retValue.setRetries(runsCounter);
		retValue.setRuns(runs);
		retValue.setGrandTotal(grandTotal);
		retValue.setBillingDateEnd(getEndOfProcessPeriod(billingProcess));
		retValue.setOrdersProcessed(billingProcess.getOrderProcesses().size());

		return retValue;
	}

	private void addRuntimeStatistic(Integer billingProcessId, Integer language,  BillingProcessRunDTOEx runDto) {
		for (Object invoiceTblObj: new BillingProcessDAS().getCountAndSum(billingProcessId)) {
			Object[] row = (Object[]) invoiceTblObj;

			BillingProcessRunTotalDTOEx totalRowDto =
					new BillingProcessRunTotalDTOEx();
			totalRowDto.setProcessRun(runDto);
			totalRowDto.setCurrency(new CurrencyDAS().find((Integer) row[2]));
			totalRowDto.setCurrencyName(totalRowDto.getCurrency().getDescription(language));
			totalRowDto.setId(-1);
			totalRowDto.setTotalInvoiced((BigDecimal) row[1]);
			totalRowDto.setTotalNotPaid(BigDecimal.ZERO);
			totalRowDto.setTotalPaid(BigDecimal.ZERO);

			// now go over the totals by payment method
			Hashtable<String, BigDecimal> totals = new Hashtable<>();

			for (Object invoicePymObj: new BillingProcessDAS().getSuccessfulProcessCurrencyMethodAndSum(billingProcessId)) {
				Object[] payedRow= (Object[]) invoicePymObj;
				if (payedRow[0].equals(totalRowDto.getCurrency().getId())) {
					PaymentMethodDTO paymentMethod = new PaymentMethodDAS().find((Integer) payedRow[1]);
					BigDecimal payed = (BigDecimal) payedRow[2];
					totals.put(paymentMethod.getDescription(language), payed);
					totalRowDto.setTotalPaid(totalRowDto.getTotalPaid().add(payed));
				}
			}
			totalRowDto.setPmTotals(totals);
			for (Object unpayedObj: new BillingProcessDAS().getFailedProcessCurrencyAndSum(billingProcessId)) {
				Object[] unpayedRow = (Object[]) unpayedObj;
				if (unpayedRow[0].equals(totalRowDto.getCurrency().getId())) {
					totalRowDto.setTotalNotPaid(totalRowDto.getTotalNotPaid().add((BigDecimal) unpayedRow[1]));
				}
			}

			runDto.setInvoicesGenerated(runDto.getInvoicesGenerated() + ((Long) row[0]).intValue());
			runDto.getTotals().add(totalRowDto);
		}
	}

	public CachedRowSet getList(Integer entityId)
			throws SQLException, Exception {
		prepareStatement(ProcessSQL.generalList);
		cachedResults.setInt(1, entityId);
		execute();
		conn.close();
		return cachedResults;

	}

	public int getLast(Integer entityId)
			throws SQLException, Exception {
		int retValue = -1;
		prepareStatement(ProcessSQL.lastId);
		cachedResults.setInt(1, entityId);
		execute();
		conn.close();

		if (cachedResults.next()) {
			retValue = cachedResults.getInt(1);
			if(retValue == 0) {
				retValue = -1;
			}
		}

		return retValue;
	}

	public Integer[] getToRetry(Integer entityId)
			throws SQLException, Exception {
		List<Integer> list = new ArrayList<>();

		prepareStatement(ProcessSQL.findToRetry);
		cachedResults.setInt(1, entityId);
		execute();
		conn.close();

		while (cachedResults.next()) {
			list.add(cachedResults.getInt(1));
		}

		Integer retValue[] = new Integer[list.size()];
		list.toArray(retValue);

		return retValue;
	}

	/**
	 * Tries to get paid the invoice of the parameter.
	 * The processs Id and runId are only to update the run totals.
	 * Only one of them is required. The runId should be passed if
	 * this is a retry, otherwise the processId.
	 * @param processId
	 * @param runId
	 * @param invoiceId
	 * @throws SessionInternalError
	 */
	public void generatePayment(Integer processId, Integer runId, Integer invoiceId) throws SessionInternalError {

		try {
			InvoiceBL invoiceBL = new InvoiceBL(invoiceId);
			InvoiceDTO newInvoice = invoiceBL.getEntity();
			IPaymentSessionBean paymentSess = Context.getBean(Context.Name.PAYMENT_SESSION);
			paymentSess.generatePayment(newInvoice);
		} catch (Exception e) {
			throw new SessionInternalError(e);
		}
	}

	public BillingProcessRunTotalDTOEx getTotal(CurrencyDTO currency, List<BillingProcessRunTotalDTOEx> totals) {
		BillingProcessRunTotalDTOEx retValue = null;
		for (int f = 0; f < totals.size(); f++) {
			BillingProcessRunTotalDTOEx total = totals.get(f);
			if (total.getCurrency().equals(currency)) {
				retValue = total;
				break;
			}
		}

		// it is looking for a total that doesn't exist
		if (retValue == null) {
			CurrencyDAS curDas = new CurrencyDAS();
			CurrencyDTO curDto = curDas.find(currency.getId());
			retValue = new BillingProcessRunTotalDTOEx(null, curDto, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
			totals.add(retValue);
		}

		return retValue;
	}

	public BillingProcessDTOEx getReviewDTO(Integer entityId, Integer languageId) {
		billingProcess = billingProcessDas.findReview(entityId);
		if (billingProcess == null) {
			logger.debug("Don't found the billingProcess");
			return null;
		} else {
			logger.debug("found the billingProcess");
		}
		return getDtoEx(languageId);
	}

	public boolean isReviewPresent(Integer entityId) {
		return billingProcessDas.findReview(entityId) != null;
	}

	public void purgeReview(Integer entityId, boolean isReview) {
		BillingProcessDTO review = billingProcessDas.findReview(
				entityId);
		if (review == null) {
			// no review, nothing to delete then
			return;
		}

		// if we are here, a review exists
		ConfigurationBL configBL = new ConfigurationBL(entityId);
		if (configBL.getEntity().getGenerateReport() == 1 &&
				!new Integer(configBL.getEntity().getReviewStatus()).equals(
						Constants.REVIEW_STATUS_APPROVED) && !isReview) {
			eLogger.warning(entityId, null, configBL.getEntity().getId(),
					EventLogger.MODULE_BILLING_PROCESS,
					EventLogger.BILLING_REVIEW_NOT_APPROVED,
					Constants.TABLE_BILLING_PROCESS_CONFIGURATION);
		}
		// delete the review
		logger.debug("Removing review id = {} from entity {}", review.getId(), entityId);
		// this is needed while the order process is JPA, but the billing process is Entity
		OrderProcessDAS processDas = new OrderProcessDAS();
		com.sapienter.jbilling.server.process.db.BillingProcessDTO processDto = new BillingProcessDAS().find(review.getId());

		// deleting all the references of billing process and process runs from
		// billing_process_failed_user, batch_process_info
		for (BillingProcessInfoDTO batchInfoDto : new BillingProcessInfoDAS().findExecutionsInfoByBillingProcessId(processDto.getId())) {
			new BillingProcessFailedUserDAS().removeFailedUsersForBatchProcess(batchInfoDto.getId());
			new BillingProcessInfoDAS().delete(batchInfoDto);
		}

		for (OrderProcessDTO orderDto : processDto.getOrderProcesses()) {
			processDas.delete(orderDto);
		}
		processDto.getOrderProcesses().clear();
		// delete processRunUsers otherwise will be constraint violation
		for (ProcessRunDTO processRun : review.getProcessRuns()) {
			new ProcessRunUserDAS().removeProcessRunUsersForProcessRun(processRun.getId());
		}

		// delete billing_batch_job_data to avoid constraint violation
		BillingBatchService billingBatchService = Context.getBean(BillingBatchService.class);
		billingBatchService.cleanupBillingProcessData(review.getId());
		// delete service summary generated by billing process
		new ServiceSummaryBL().deleteByBillingProcess(review.getId());
		// delete invoice summary generated by billing process.
		new InvoiceSummaryBL().deleteByBillingProcessId(review.getId());
		// otherwise this line would cascade de delete
		getHome().delete(review);
	}

	private void processOrderToInvoiceEvents(NewInvoiceContext newInvoice, Integer entityId) {
		for (NewInvoiceContext.OrderContext orderCtx : newInvoice.getOrders()) {
			OrderDTO order = orderCtx.order;
			Integer userId = findUserId(order);
			for (PeriodOfTime period : orderCtx.periods) {
				OrderToInvoiceEvent newEvent = new OrderToInvoiceEvent(entityId, userId, order);
				newEvent.setStart(period.getStart());
				newEvent.setEnd(period.getEnd());
				EventManager.process(newEvent);
			}
		}
	}

	private void processOrderAddedOnInvoiceEvents(NewInvoiceContext newInvoice, Integer entityId) {
		List<NewInvoiceContext.OrderContext> orders = newInvoice.getOrders();
		logger.info("Number of orders: {}", orders.size());
		for (NewInvoiceContext.OrderContext orderCtx : orders) {
			OrderDTO order = orderCtx.order;
			Integer userId = findUserId(order);
			OrderAddedOnInvoiceEvent newEvent =
					new OrderAddedOnInvoiceEvent(entityId, userId, order, orderCtx.totalContribution);
			newEvent.setStart(orderCtx.orderPeriodStart);
			newEvent.setEnd(orderCtx.orderPeriodEnd);
			EventManager.process(newEvent);
		}
	}

	/**
	 * Handle tax and penalties
	 * @param newInvoice
	 * @param entityId
	 * @param userId
	 */
	private void processTaxesAndPenalties(NewInvoiceContext newInvoice,
			Integer entityId, Integer userId) {
		logger.debug("Handling taxes and penalties to fire processTaxLineOnInvoiceEvents");
		for (int i = 0; i < newInvoice.getResultLines().size(); i++) {
			InvoiceLineDTO invoiceLine = newInvoice.getResultLines().get(i);
			if (null != invoiceLine.getInvoiceLineType() && invoiceLine.getInvoiceLineType().getId() == Constants.INVOICE_LINE_TYPE_TAX) {
				ProcessTaxLineOnInvoiceEvent taxLineOnInvoice= new ProcessTaxLineOnInvoiceEvent(entityId, userId, invoiceLine.getAmount());
				logger.debug("Firing event {}", taxLineOnInvoice);
				EventManager.process(taxLineOnInvoice);
			}
		}
	}

	private Integer findUserId(OrderDTO order) {
		UserDTO user = order.getUser();

		// while this user has a parent and the flag is off, keep looking
		while(user.getCustomer().getParent() != null &&
				(user.getCustomer().getInvoiceChild() == null ||
				user.getCustomer().getInvoiceChild() == 0)) {
			// go up one level
			logger.debug("finding parent for invoicing. Now {}", user.getUserId());
			user = user.getCustomer().getParent().getBaseUser();
		}

		return user.getUserId();
	}

	/**
	 * Convert a given BillingProcessDTO into a BillingProcessWS web-service object.
	 *
	 * @param dto dto to convert
	 * @return converted web-service object
	 */
	public static BillingProcessWS getWS(BillingProcessDTO dto) {
		if (null == dto) {
			return null;
		}

		BillingProcessWS ws = new BillingProcessWS();

		ws.setId(dto.getId());
		ws.setEntityId(dto.getEntity() != null ? dto.getEntity().getId() : null);
		ws.setPeriodUnitId(dto.getPeriodUnit() != null ? dto.getPeriodUnit()
				.getId() : null);
		ws.setPeriodValue(dto.getPeriodValue());
		ws.setBillingDate(dto.getBillingDate());
		ws.setReview(dto.getIsReview());
		ws.setRetriesToDo(dto.getRetriesToDo());

		// invoice ID's
		ws.getInvoiceIds().addAll(new InvoiceDAS().getInvoiceIdsByBillingProcessId(dto.getId()));

		// order processes
		if (!dto.getOrderProcesses().isEmpty()) {
			ws.setOrderProcesses(new ArrayList<>(dto
					.getOrderProcesses().size()));
			for (OrderProcessDTO process : dto.getOrderProcesses()) {
				ws.getOrderProcesses().add(OrderBL.getOrderProcessWS(process));
			}
		}

		if (!dto.getProcessRuns().isEmpty()) {
			// billing process runs
			ws.setProcessRuns(new ArrayList<>(dto.getProcessRuns().size()));
			for (ProcessRunDTO run : dto.getProcessRuns()) {
				ws.getProcessRuns().add(BillingProcessBL.getProcessRunWS(run));
			}
		}
		return ws;
	}

	/**
	 * Returns billing process ws without any heavy association.
	 * @param dto
	 * @return
	 */
	public static BillingProcessWS getSimpleWS(BillingProcessDTO dto) {
		if (null == dto) {
			return null;
		}
		BillingProcessWS ws = new BillingProcessWS();

		ws.setId(dto.getId());
		ws.setEntityId(dto.getEntity() != null ? dto.getEntity().getId() : null);
		ws.setPeriodUnitId(dto.getPeriodUnit() != null ? dto.getPeriodUnit()
				.getId() : null);
		ws.setPeriodValue(dto.getPeriodValue());
		ws.setBillingDate(dto.getBillingDate());
		ws.setReview(dto.getIsReview());
		ws.setRetriesToDo(dto.getRetriesToDo());
		return ws;
	}


	/**
	 * Convert a given BillingProcessDTOEx into a BillingProcessWS web-service object.
	 *
	 * @param dto extended DTO to convert
	 * @return converted web-service object
	 */
	public static BillingProcessWS getWS(BillingProcessDTOEx dto) {
		if (null == dto) {
			return null;
		}

		BillingProcessWS ws = getWS((BillingProcessDTO) dto);

		ws.setBillingDateEnd(dto.getBillingDateEnd());
		ws.setRetries(dto.getRetries());

		// billing process runs
		if (!dto.getRuns().isEmpty()) {
			ws.setProcessRuns(new ArrayList<>(dto.getRuns().size()));
			for (BillingProcessRunDTOEx run : dto.getRuns()) {
				ws.getProcessRuns().add(BillingProcessBL.getProcessRunWS(run));
			}
		}

		return ws;
	}

	public static ProcessRunTotalWS getProcessRunTotalWS(ProcessRunTotalDTO dto) {

		ProcessRunTotalWS ws = new ProcessRunTotalWS();
		ws.setId(dto.getId());
		ws.setProcessRunId(dto.getProcessRun() != null ? dto.getProcessRun().getId() : null);
		ws.setCurrencyId(dto.getCurrency() != null ? dto.getCurrency().getId() : null);
		ws.setTotalInvoiced(dto.getTotalInvoiced());
		ws.setTotalPaid(dto.getTotalPaid());
		ws.setTotalNotPaid(dto.getTotalNotPaid());
		return ws;
	}

	public static ProcessRunWS getProcessRunWS(ProcessRunDTO dto) {

		ProcessRunWS ws = new ProcessRunWS();
		ws.setId(dto.getId());
		ws.setBillingProcessId(dto.getBillingProcess() != null ? dto.getBillingProcess().getId() : null);
		ws.setRunDate(dto.getRunDate());
		ws.setStarted(dto.getStarted());
		ws.setFinished(dto.getFinished());
		ws.setInvoicesGenerated(dto.getInvoicesGenerated());
		ws.setPaymentFinished(dto.getPaymentFinished());
		ws.setStatusId(dto.getStatus() != null ? dto.getStatus().getId() : null);

		// billing process run totals
		if (!dto.getProcessRunTotals().isEmpty()) {
			ws.setProcessRunTotals(new ArrayList<>(dto.getProcessRunTotals().size()));
			for (ProcessRunTotalDTO runTotal : dto.getProcessRunTotals()) {
				ws.getProcessRunTotals().add(BillingProcessBL.getProcessRunTotalWS(runTotal));
			}
		}
		return ws;
	}

	public static ProcessRunWS getProcessRunWS(BillingProcessRunDTOEx ex) {

		ProcessRunWS ws = getProcessRunWS((ProcessRunDTO) ex);

		ws.setStatusStr(ex.getStatusStr());

		if (!ex.getTotals().isEmpty()) {
			ws.setProcessRunTotals(new ArrayList<>(ex.getTotals().size()));
			for (BillingProcessRunTotalDTOEx runTotal : ex.getTotals()) {
				ws.getProcessRunTotals().add(BillingProcessBL.getProcessRunTotalWS(runTotal));
			}
		}
		return ws;
	}

	public BillingProcessDTOEx getSimpleDtoEx() {
		BillingProcessDTOEx retValue = new BillingProcessDTOEx();

		retValue.setBillingDate(billingProcess.getBillingDate());
		retValue.setEntity(billingProcess.getEntity());
		retValue.setId(billingProcess.getId());
		retValue.setPeriodUnit(billingProcess.getPeriodUnit());
		retValue.setPeriodValue(billingProcess.getPeriodValue());
		retValue.setIsReview(billingProcess.getIsReview());
		return retValue;
	}

	public static List<Integer> getInvoiceIdsByBillingProcessId(Integer processId) {
		return new InvoiceDAS().getInvoiceIdsByBillingProcessId(processId);
	}

	public static List<OrderProcessWS> getOrderPrcoessesByPrcoessId(Integer processId) {
		return new OrderProcessDAS().getOrderPrcoessesByPrcoessId(processId);
	}

	/**
	 * get processed order counts using billing process Id
	 * @param billingProcessId
	 * @return
	 */
	public Long getOrderProcessCount(Integer billingProcessId) {
		return billingProcessId != null ? billingProcessDas.getOrderProcessCount(billingProcessId) : 0L;
	}

	/**
	 * get generated Invoice counts using billing process Id
	 * @param billingProcessId
	 * @return
	 */
	public Long getInvoiceProcessCount(Integer billingProcessId) {
		return billingProcessId != null ? billingProcessDas.getInvoiceProcessCount(billingProcessId) : 0L;
	}
}
