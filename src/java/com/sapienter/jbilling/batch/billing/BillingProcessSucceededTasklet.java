package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.payment.event.EndProcessPaymentEvent;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.usagePool.ICustomerUsagePoolEvaluationSessionBean;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;

/**
 *
 * @author Khobab
 *
 */
public class BillingProcessSucceededTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
	private IBillingProcessSessionBean local;
    @Resource
    ICustomerUsagePoolEvaluationSessionBean usagePoolEvaluationBean;
    @Resource
    INotificationSessionBean notificationSession;
    @Resource
    private BillingProcessDAS billingProcessDAS;
    @Resource
    private UserDAS userDAS;

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{jobParameters['entityId']}")
	private Integer entityId;
    @Value("#{jobParameters['billingDate']}")
	private Date billingDate;
    @Value("#{jobParameters['review'] == 1L}")
    private boolean review;

	/**
	 * Set billing process as Successful, marks parallel payment processing finished and sets next billing date.
	 */
	@Override
    public RepeatStatus execute (StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        logger.debug("billingProcessId: {}, totalInvoices: {}", billingProcessId,
                local.getInvoiceCountByBillingProcessId(billingProcessId));

		// only if all got well processed
		// if some of the invoices were paper invoices, a new file with all
		// of them has to be generated
		try {
            // ref #4800. The process entity is this session does not
            // have the changes made with the paper invoice
            // notification about the paper invoice batch
            // so we first evict the old object and reattach
            // new one to get the information about the batch process
            billingProcessDAS.detach(new BillingProcessBL(billingProcessId).getEntity());

            // send message to compile email batch
			MessageDTO messageDTO = PaperInvoiceBatchBL.createCompileMessage(entityId, billingProcessId);
            notificationSession.notify(userDAS.findAdminUserIds(entityId).get(0), messageDTO);

		} catch (Exception e) {
            logger.error("Error generating notiifcation", e);
		}

        Integer processRunId = local.updateProcessRunFinished(billingProcessId, Constants.PROCESS_RUN_STATUS_SUCCESS);

		if (!review) {
			// the payment processing is happening in parallel
			// this event marks the end of it
            EventManager.process(new EndProcessPaymentEvent(processRunId, entityId));
			// and finally the next run date in the config
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(billingDate);

			/**
             * Calculate billing next run date as per billing configuration next run date and billing period. A. In
             * first if calculation of Next Run Date in case Semi Monthly period in calculateNextRunDateForSemiMonthly
             * method. B. In second else if calculation of Next Run Date in case Monthly period and lastDayOfMonth flag
             * is true. Calculation in calculateNextRunDateForEndOfMonth method. C. In last else calculate Next Run Date
             * as per other period and period value 1.
			 */
            ConfigurationBL conf = new ConfigurationBL(entityId);
			BillingProcessConfigurationDTO billingProcesssConfig = conf.getDTO();
			Integer periodUnit = billingProcesssConfig.getPeriodUnit().getId();
			if (CalendarUtils.isSemiMonthlyPeriod(periodUnit)) {
				cal.setTime(CalendarUtils.addSemiMonthyPeriod(billingDate));
			} else if (periodUnit.compareTo(Constants.PERIOD_UNIT_MONTH) == 0
                    && billingProcesssConfig.getLastDayOfMonth()) {
				cal.setTime(calculateNextRunDateForEndOfMonth(billingDate));
			} else {
                cal.add(MapPeriodToCalendar.map(periodUnit), 1);
			}

			conf.getEntity().setNextRunDate(cal.getTime());
            logger.debug("Updated run date to {}", cal.getTime());

			// trigger usage pool evaluation task when final billing run status is successful
            triggerUsagePoolEvaluationAsync();
		}

		return RepeatStatus.FINISHED;
	}

	/**
     * Returns the maximum value that Month if if period unit monthly and lastDayOfMonth flag is true, For example, if
     * the date of this instance is February 1, 2004 the actual maximum value of the DAY_OF_MONTH field is 29 because
     * 2004 is a leap year, and if the date of this instance is February 1, 2005, it's 28.
	  *
	  * @param billingDate
	  * @return
	  */
    private static Date calculateNextRunDateForEndOfMonth (Date billingDate) {

		 GregorianCalendar cal = new GregorianCalendar();
		 cal.setTime(billingDate);
		 Integer dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		 if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= dayOfMonth) {
			 cal.add(Calendar.MONTH, 1);
			 cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
		 } else {
			 cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		 }

		 return cal.getTime();
	 }

    private void triggerUsagePoolEvaluationAsync () {
        new Thread( () -> {
            logger.trace("Entered into triggerUsagePoolEvaluationAsync for entity {} on date {}", entityId, billingDate);
            usagePoolEvaluationBean.trigger(entityId, billingDate);
        }).start();
	 }
}
