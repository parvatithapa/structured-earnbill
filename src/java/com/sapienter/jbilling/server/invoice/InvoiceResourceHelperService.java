package com.sapienter.jbilling.server.invoice;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDAS;
import com.sapienter.jbilling.einvoice.db.EInvoiceLogDTO;
import com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
public class InvoiceResourceHelperService {

	private static final String EINVOICE_PLUGIN_INTERFACE_NAME = "com.sapienter.jbilling.einvoice.plugin.IEInvoiceProvider";
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String INVOICE_FILE_NAME_PATTERN = "invoice-%s.pdf";
	private static final String APPLICATION_PDF_TYPE = "application/pdf";

	@Resource(name = "webServicesSession")
	private IWebServicesSessionBean api;
	@Resource
	private InvoiceDAS invoiceDAS;
	@Resource
	private EInvoiceLogDAS eInvoiceLogDAS;


	@Transactional(readOnly = true)
	public Response generatePdfFileForInvoice(Integer invoiceId) {
		try {
			logger.debug("generating pdf file for invoice {}", invoiceId);
			byte[] data = api.getPaperInvoicePDF(invoiceId);
			if(ArrayUtils.isEmpty(data)) {
				return Response.noContent()
						.build();
			}
			InvoiceDTO invoice = new InvoiceDAS().findNow(invoiceId);
			return Response.ok(data, APPLICATION_PDF_TYPE)
					.header("Content-Disposition" ,"attachment; filename = "+ String.format(INVOICE_FILE_NAME_PATTERN, invoice.getNumber()))
					.build();
		} catch(SessionInternalError error) {
			logger.error("error in generatePdfFileForInvoice", error);
			throw error;
		}
	}

	public String createEInvoice(Integer invoiceId) {
		try {
			if(!invoiceDAS.isIdPersisted(invoiceId)) {
				throw new SessionInternalError("invoice id not found",
						new String[] { "invalid invoice id passed" }, HttpStatus.SC_NOT_FOUND);
			}
			EInvoiceLogDTO eInvoiceLog = eInvoiceLogDAS.findByInvoiceId(invoiceId);
			if(null!= eInvoiceLog) {
				return eInvoiceLog.geteInvoiceResponse();
			}
			return createEInvoice(api.getCallerCompanyId(), invoiceId);
		} catch(SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch (Exception exception) {
			logger.error("createEInvoice failed for invoiceId={}", invoiceId, exception);
			throw new SessionInternalError("createEInvoice failed for invoice Id"+ invoiceId, exception);
		}
	}

	public String findEInvoiceDetailsByInvoiceId(Integer invoiceId) {
		try {
			if(!invoiceDAS.isIdPersisted(invoiceId)) {
				throw new SessionInternalError("invoice id not found",
						new String[] { "invalid invoice id passed" }, HttpStatus.SC_NOT_FOUND);
			}
			EInvoiceLogDTO eInvoiceLog = eInvoiceLogDAS.findByInvoiceId(invoiceId);
			if(null!= eInvoiceLog) {
				return eInvoiceLog.geteInvoiceResponse();
			}
			logger.error("eInvoice details not found for invoiceId={}", invoiceId);
			throw new SessionInternalError("eInvoice details not found", new String[] { "eInvoice details "
					+ "not found for invoiceId:"+ invoiceId }, HttpStatus.SC_NOT_FOUND);
		} catch(SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch (Exception  exception) {
			logger.error("findEInvoiceDetailsByInvoiceId failed for invoiceId={}", invoiceId, exception);
			throw new SessionInternalError("findEInvoiceDetailsByInvoiceId failed for invoice Id"+ invoiceId, exception);
		}
	}

	private String createEInvoice(Integer entityId, Integer invoiceId) {
		try {
			// load eInvoiceProvider plugins.
			PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS = new PluggableTaskTypeCategoryDAS();
			int eInvoiceProviderPluginTypeId = pluggableTaskTypeCategoryDAS.findByInterfaceName(EINVOICE_PLUGIN_INTERFACE_NAME).getId();
			PluggableTaskManager<IEInvoiceProvider> taskManager = new PluggableTaskManager<>(entityId, eInvoiceProviderPluginTypeId);
			IEInvoiceProvider task = taskManager.getNextClass();
			if(null == task) {
				logger.error("no plugin configured to createEInvoice on gst portal for entityId={}", entityId);
				throw new SessionInternalError("no plugin configured to createEInvoice on gst portal for entity "+ entityId);
			}
			return task.sendInvoiceToEInvoicePortal(invoiceId);
		} catch(PluggableTaskException pluggableTaskException) {
			throw new SessionInternalError("eInvoice create failed for invoice"+ invoiceId, pluggableTaskException);
		}
	}
}
