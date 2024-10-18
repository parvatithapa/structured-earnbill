package com.sapienter.jbilling.server.billing.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.db.PaymentProcessRunDTO;
import com.sapienter.jbilling.server.payment.event.ProcessAutoPaymentEvent;
import com.sapienter.jbilling.server.process.BillingProcessDTOEx;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Set;

public class PaymentProcessTask extends AbstractCronTask {

	private static final FormatLogger LOG = new FormatLogger(
			Logger.getLogger(PaymentProcessTask.class));

	public PaymentProcessTask() {
		setUseTransaction(true);
	}
	
	@Override
	public String getTaskName() {
		return "payment message: , entity id " + getEntityId() + ", taskId "
				+ getTaskId();
	}

	public void doExecute(JobExecutionContext context)
			throws JobExecutionException {

		LOG.debug("executing PaymentProcessTask");
		ConfigurationBL configurationBL = new ConfigurationBL(getEntityId());

		BillingProcessConfigurationDTO configDTO = configurationBL.getDTO();

		if (configDTO.getAutoPayment() == 1) {

			IBillingProcessSessionBean billing = (IBillingProcessSessionBean) Context
					.getBean(Context.Name.BILLING_PROCESS_SESSION);
			if(billing.isBillingRunning(getEntityId()) || billing.isAgeingProcessRunning(getEntityId()) || billing.isPaymentProcessRunning(getEntityId())){
				return;
			}
			Integer billingProcessId = billing.getLast(getEntityId());
			PaymentProcessRunDTO paymentProcess = billing.findPaymentProcessRun(billingProcessId);
			if(paymentProcess == null){
				BillingProcessDTOEx dtoEx = billing.getSimpleDto(billingProcessId);
				if (dtoEx.getIsReview() == 1) {
					LOG.info("This is review run, no need to invoke payment process.");
					return;
				}
				paymentProcess = new PaymentProcessRunDTO(billingProcessId, dtoEx.getBillingDate(), 0);
				paymentProcess = billing.saveOrUpdatePaymentProcessRun(paymentProcess);
			}else if(configDTO.getRetryCount() <= paymentProcess.getRunCount()){
				LOG.info("Retries exhausted for billing process id: " + billingProcessId);
				return;
			}else{
				paymentProcess.setRunCount(paymentProcess.getRunCount()+1);
				paymentProcess = billing.saveOrUpdatePaymentProcessRun(paymentProcess);
			}
			Set<Integer> users = billing.findInvoicesForAutoPayments(getEntityId());
			LOG.debug("processing users: "+ (null != users ? users.size() : 0));
			if (users != null) {
				for(Integer userId: users){
					LOG.debug("invoking ProcessAutoPaymentEvent for user id: "+userId);
					EventManager.process(new ProcessAutoPaymentEvent(userId, getEntityId()));
				}
			}
		}
	}
}
