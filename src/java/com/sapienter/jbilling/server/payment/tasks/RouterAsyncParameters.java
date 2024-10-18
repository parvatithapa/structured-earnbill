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
package com.sapienter.jbilling.server.payment.tasks;

import java.util.Map;

import javax.jms.MapMessage;

import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;

public class RouterAsyncParameters extends PluggableTask implements IAsyncPaymentParameters  {

	public void addParameters(MapMessage message) throws TaskException {
		try {
			int invoiceId = message.getInt("invoiceId");
			if(invoiceId > 0) {
				InvoiceBL invoiceBl = new InvoiceBL(invoiceId);
				Integer entityId = invoiceBl.getEntity().getBaseUser().getEntity().getId();
				InvoiceDTO invoice = invoiceBl.getDTO();
				AbstractPaymentRouterTask router = getRouter(entityId);
				Map<String, String> parameters = router.getAsyncParameters(invoice);
				for(Map.Entry<String, String> parameter : parameters.entrySet()) {
					message.setStringProperty(parameter.getKey(), 
							parameter.getValue());
				}
			} else {
				/*
				 * Commenting out the following lines (53,54 & 55) of code in order to address one of the comments
				 * from "Sonarqube static source code analysis". The comment was regarding "dead store to userId/router".
				 * Reviewed the code and found that local variables are not being used hence code becoming dead.
				 */
				/*int userId = message.getInt("userId");
				int entityId = message.getInt("entityId");
				AbstractPaymentRouterTask router = getRouter(entityId);*/

				// TODO: commenting out following code as it is throw no session exception inside getAsyncParameters method
//				Map<String, String> parameters = router.getAsyncParameters(userId);
//				for(Map.Entry<String, String> parameter : parameters.entrySet()) {
//					message.setStringProperty(parameter.getKey(), 
//							parameter.getValue());
//				}
			}
		} catch (Exception e) {
			throw new TaskException(e);
		} 

	}

	private AbstractPaymentRouterTask getRouter(Integer entityId) throws PluggableTaskException, TaskException{
		PluggableTaskManager taskManager = new PluggableTaskManager(entityId, 
				Constants.PLUGGABLE_TASK_PAYMENT);
		// search for PaymentRouterTask in the payment chain
		AbstractPaymentRouterTask router = null;
		Object task = taskManager.getNextClass();
		while (task != null) {
			if (task instanceof AbstractPaymentRouterTask) {
				router = (AbstractPaymentRouterTask) task;
				break;
			}
			task = taskManager.getNextClass();
		}

		if (router == null) {
			throw new TaskException("Can not find router task");
		}
		return router;
	}
}
