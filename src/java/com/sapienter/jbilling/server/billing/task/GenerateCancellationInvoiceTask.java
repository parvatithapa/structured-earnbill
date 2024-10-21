package com.sapienter.jbilling.server.billing.task;


import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.user.db.CancellationRequestDAS;
import com.sapienter.jbilling.server.user.db.CancellationRequestDTO;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 * 
 * @author krunal bhavsar
 *
 */
public class GenerateCancellationInvoiceTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static ReentrantLock lock = new ReentrantLock();
	
	public GenerateCancellationInvoiceTask() {
		setUseTransaction(true);
	}
	
	@Override
	public String getTaskName() {
        return "Generate CancellationInvoice: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
    		try {
    			lock.lock(); // Acquired lock to avoid concurrency issues.
    			CancellationRequestDAS cancellationRequestDAS = new CancellationRequestDAS();
    			cancellationRequestDAS.findCancellationRequestsToBeProcessedByEntityAndDate(getEntityId(), new Date())
    			.stream()
    			.forEach( request -> {
    				sendJmsMessage(request);
    				request.setStatus(CancellationRequestStatus.PROCESSING);
    				cancellationRequestDAS.save(request);
    			});

                new PlanDAS().findUsersByFreeTrailPlan(new Date())
                 .stream()
                 .forEach(this :: sendFreeTrialPlanJmsMessage);
    		} finally {
    			lock.unlock(); //Releasing lock 
    		}
    }
    
    /**
     * post cancellation request message to jms queue.
     * @param cancellationRequest
     */
    private void sendJmsMessage(CancellationRequestDTO cancellationRequest) {

    	JmsTemplate jmsTemplate = Context.getBean(Context.Name.JMS_TEMPLATE);
    	Destination destination = Context.getBean(Context.Name.CUSTOMER_CANCELLATION_DESTINATION);

    	jmsTemplate.send(destination, session -> {
    		Integer userId = cancellationRequest.getCustomer().getBaseUser().getId();
            logger.debug("Posting user For Invoice Generation {}", userId);
    		ObjectMessage objectMessage = session.createObjectMessage();
    		objectMessage.setObject(userId);
    		objectMessage.setObjectProperty(Constants.CANCELLATION_REQUEST_DATE, cancellationRequest.getCancellationDate().getTime());
    		objectMessage.setObjectProperty(Constants.CANCELLATION_REQUEST_ID, cancellationRequest.getId());
    		return objectMessage;
    	});
    }

    /**
     * post cancellation request message to jms queue.
     * @param cancellationRequest
     */
    private void sendFreeTrialPlanJmsMessage(Integer userId) {

        JmsTemplate jmsTemplate = Context.getBean(Context.Name.JMS_TEMPLATE);
        Destination destination = Context.getBean(Context.Name.CUSTOMER_CANCELLATION_DESTINATION);

        jmsTemplate.send(destination, session -> {
            logger.debug("Posting user For Free Trail Invoice Generation {}", userId);
            ObjectMessage objectMessage = session.createObjectMessage();
            objectMessage.setObject(userId);
            return objectMessage;
        });
    }
}
