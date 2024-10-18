package com.sapienter.jbilling.server.billing.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

/**
 * @author Harhsad Pathan
 * @since 02-01-2016
 * This schedule task is for linking invoices to 
 * billing process based on invoice charged period.
 */
public class InvoiceBillingProcessLinkingTask extends AbstractCronTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(InvoiceBillingProcessLinkingTask.class));

	protected static final ParameterDescription LINKING_START_DATE =
			new ParameterDescription("LINKING_START_DATE", false, ParameterDescription.Type.DATE);

	public InvoiceBillingProcessLinkingTask () {
		descriptions.add(LINKING_START_DATE);
	}

	@Override
	public String getTaskName() {
		return this.getClass().getName() + "-" + getEntityId();
	}

	public void doExecute(JobExecutionContext context) throws JobExecutionException {
		_init(context);
		IBillingProcessSessionBean billing = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

		LOG.info("Starting link of billing process id on manual/Cancellation request invoices "
				+ "at " + TimezoneHelper.serverCurrentDate() + " for " + getEntityId());
		try {
			billing.linkInvoicesToBillingProcess(getEntityId(),getParameter(LINKING_START_DATE.getName()));
		} catch (Exception  e) {
			LOG.debug("Exception from InvoiceBillingProcessLinkingTask", e);
			throw new JobExecutionException(e);
		}
		LOG.info("Ended link of billing process id on manual/Cancellation request Invoices "
				+ "at " + TimezoneHelper.serverCurrentDate() + "for : " + getEntityId() );

	}

	public Date getParameter(String key) throws JobExecutionException, ParseException {
		String value = (String) parameters.get(key);
		if (value == null || value.trim().equals(""))
			return null;
		Date date = new SimpleDateFormat("yyyyMMdd").parse(value);
		LOG.info("In getParameter with key=" + key + " and value=" + value);
		return date;
	}
}
