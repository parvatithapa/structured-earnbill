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

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.batch.BatchConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.payment.db.PaymentProcessRunDAS;
import com.sapienter.jbilling.server.payment.db.PaymentProcessRunDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunDTO;
import com.sapienter.jbilling.server.process.event.NoNewInvoiceEvent;
import com.sapienter.jbilling.server.process.task.BasicUserFilterTask;
import com.sapienter.jbilling.server.process.task.IBillableUserFilterTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;

/**
 *
 * This is the session facade for the all the billing process and its
 * related services.
 */
@Transactional( propagation = Propagation.REQUIRED )
public class BillingProcessSessionBean implements IBillingProcessSessionBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ConcurrentMap<Integer, Boolean> ageingRunning = new ConcurrentHashMap<>();
    private static final String BILLING_PROCESS_CACHE_KEY = "Billing Process Running for [%d]";

    @Resource
    private BasicUserFilterTask basicUserFilterTask;

    /**
     * Gets the invoices for the specified process id. The returned collection
     * is of extended dtos (InvoiceDTO).
     * @param processId
     * @return A collection of InvoiceDTO objects
     * @throws SessionInternalError
     */
    @Override
    public Collection<InvoiceDTO> getGeneratedInvoices(Integer processId) {
        // find the billing_process home interface
        BillingProcessDAS processHome = new BillingProcessDAS();
        Collection<InvoiceDTO> invoices =  new InvoiceDAS().findByProcess(processHome.find(processId));
        for (InvoiceDTO invoice : invoices) {
            invoice.getOrderProcesses().iterator().next().getId(); // it is a touch
        }
        return invoices;
    }

    @Override
    public List<Integer> getPagedGeneratedInvoices(Integer processId, Integer limit, Integer offset) {
        return new InvoiceDAS().findByProcess(processId, limit, offset);
    }

    @Override
    public List<Integer> findAllInvoiceIdsForByProcessId(Integer processId) {
        return new InvoiceDAS().findAllInvoiceIdsForByProcessId(processId);
    }

    @Override
    public List<Integer> findAllInvoiceIdsForByProcessIdAndInvoiceDesign(Integer billingProcessId, String invoiceDesign) {
        return new InvoiceDAS().findAllInvoiceIdsForByProcessIdAndInvoiceDesign(billingProcessId, invoiceDesign);
    }

    /**
     * @param entityId
     * @param languageId
     * @param collectionType
     * @return
     * @throws SessionInternalError
     */
    @Override
    public AgeingDTOEx[] getAgeingSteps(Integer entityId,
            Integer executorLanguageId, Integer languageId, CollectionType collectionType) {
        try {
            AgeingBL ageing = new AgeingBL();
            return ageing.getSteps(entityId, executorLanguageId, languageId, collectionType);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * @param entityId
     * @param languageId
     * @param steps
     * @param collectionType
     * @throws SessionInternalError
     */
    @Override
    @Transactional( propagation = Propagation.REQUIRES_NEW)
    public void setAgeingSteps(Integer entityId, Integer languageId, AgeingDTOEx[] steps, CollectionType collectionType) {
        try {
            new AgeingBL().setSteps(entityId, languageId, steps, collectionType);
        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public void generateReview(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue) {
        logger.debug("Generating review entity {}", entityId);
        IBillingProcessSessionBean local = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
        local.processEntity(entityId, billingDate, periodType,
                periodValue, true);
        // let know this entity that a new reivew is now pending approval
        try {
            String[] params = new String[1];
            params[0] = entityId.toString();
            NotificationBL.sendSapienterEmail(entityId, "process.new_review",
                    null, params);
        } catch (Exception e) {
            logger.warn("Exception sending email to entity", e);
        }
    }

    /**
     * Creates the billing process record. This has to be done in its own
     * transaction (thus, in its own method), so new invoices can link to
     * an existing process record in the db.
     */
    @Override
    public BillingProcessDTO createProcessRecord(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue, boolean isReview,
            Integer retries) {
        BillingProcessBL bpBL = new BillingProcessBL();
        BillingProcessDTO dto = new BillingProcessDTO();

        // process can't leave reviews behind, and a review has to
        // delete the previous one too
        bpBL.purgeReview(entityId, isReview);

        //I need to find the entity
        CompanyDAS comDas = new CompanyDAS();
        CompanyDTO company = comDas.find(entityId);
        //I need to find the PeriodUnit
        PeriodUnitDAS periodDas = new PeriodUnitDAS();
        PeriodUnitDTO period = periodDas.find(periodType);

        dto.setEntity(company);
        dto.setBillingDate(Util.truncateDate(billingDate));
        dto.setPeriodUnit(period);
        dto.setPeriodValue(periodValue);
        dto.setIsReview(isReview ? 1 : 0);
        dto.setRetriesToDo(retries);

        bpBL.findOrCreate(dto);
        return bpBL.getEntity();
    }

    @Override
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public Integer createRetryRun(Integer processId) {
        BillingProcessBL process = new BillingProcessBL(processId);
        // create a new run record
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.create(process.getEntity(), process.getEntity().getBillingDate());
        logger.debug("created process run {}", runBL.getEntity().getId());

        return runBL.getEntity().getId();
    }

    @Override
    @Transactional( propagation = Propagation.NOT_SUPPORTED )
    public void processEntity(Integer entityId, Date billingDate, Integer periodType, Integer periodValue,
            boolean isReview) {
        logger.debug("Entering processEntity(entityId: {}, billingDate: {}, periodType: {}, periodValue: {})",
                entityId, billingDate, periodValue, periodValue);
        if (entityId == null || billingDate == null) {
            throw new SessionInternalError("entityId and billingDate can't be null");
        }

        JobLauncher launcher = Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);
        logger.debug("Loaded job launcher bean # {}", launcher);

        Job job = Context.getBean(Context.Name.BATCH_JOB_GENERATE_INVOICES);
        logger.debug("Loaded job bean # {}", job);

        JobParametersBuilder paramBuilder = new JobParametersBuilder()
        .addLong(Constants.BATCH_JOB_PARAM_ENTITY_ID, entityId.longValue())
        .addDate(BatchConstants.PARAM_BILLING_DATE, billingDate)
        .addLong(BatchConstants.PARAM_PERIOD_VALUE, periodValue.longValue())
        .addLong(BatchConstants.PARAM_PERIOD_TYPE, periodType.longValue())
        .addLong(BatchConstants.PARAM_REVIEW, (isReview ? 1L : 0L));
        if(isReview) {
            paramBuilder.addDate(Constants.BATCH_JOB_PARAM_UNIQUE, TimezoneHelper.serverCurrentDate());
        }
        JobParameters jobParameters = paramBuilder.toJobParameters();

        try {
            launcher.run(job, jobParameters);
        } catch (Exception e) {
            logger.error("Job # {} with parameters # {} colud not be launched: {}",
                    job.getName(), jobParameters.toString(), e);
        }

        logger.debug("Job for entity id # {} has finished successfully", entityId);
    }

    /**
     * This method process a payment synchronously. It is a wrapper to the payment processing
     * so it runs in its own transaction
     */
    @Override
    public void processPayment(Integer processId, Integer runId, Integer invoiceId) {
        logger.debug("Entering processPayment()");
        try {
            BillingProcessBL bl = new BillingProcessBL();
            bl.generatePayment(processId, runId, invoiceId);
        } catch (Exception e) {
            logger.error("Exception processing a payment ", e);
        }
    }

    /**
     * This method marks the end of payment processing. It is a wrapper
     * so it runs in its own transaction
     */
    @Override
    public void endPayments(Integer runId) {
        logger.debug("Entering endPayment()");
        BillingProcessRunBL run = new BillingProcessRunBL(runId);
        run.updatePaymentsFinished();
        // update the totals
        run.updateTotals(run.getEntity().getBillingProcess().getId());
        run.updatePaymentsStatistic(run.getEntity().getId());
        logger.debug("Leaving endPayment()");
    }

    @Override
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public boolean verifyIsRetry(Integer processId, int retryDays, Date today) {
        GregorianCalendar cal = new GregorianCalendar();
        // find the last run date
        BillingProcessBL process = new BillingProcessBL(processId);
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        ProcessRunDTO lastRun = Collections.max(process.getEntity().getProcessRuns(), runBL.new DateComparator());
        cal.setTime(Util.truncateDate(lastRun.getStarted()));
        logger.debug("Retry evaluation lastrun = {}", cal.getTime());
        cal.add(GregorianCalendar.DAY_OF_MONTH, retryDays);
        logger.debug("Added days = {} today = {}", cal.getTime(), today);
        return !cal.getTime().after(today);
    }


    @Override
    @Transactional( propagation = Propagation.REQUIRED )
    public void email(Integer entityId, Integer invoiceId, Integer processId) {
        InvoiceBL invoice = new InvoiceBL(invoiceId);
        Integer userId = invoice.getEntity().getBaseUser().getUserId();

        logger.debug("email and payment for user {} invoice {}", userId, invoiceId);

        // last but not least, let this user know about his/her new
        // invoice.
        NotificationBL notif = new NotificationBL();

        try {
            MessageDTO[] invoiceMessage = notif.getInvoiceMessages(entityId,
                    processId,
                    invoice.getEntity().getBaseUser().getLanguageIdField(),
                    invoice.getEntity());

            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);

            for (MessageDTO messageDTO : invoiceMessage) {
                String fileName = messageDTO.getAttachmentFile();
                messageDTO.setInvoiceId(invoiceId);
                if(StringUtils.isNotEmpty(fileName)) {
                    logger.debug("attached email file {} found for invoice {} for user{}", fileName, invoiceId, userId);
                    if(!fileName.contains(invoiceId.toString())) {
                        logger.debug("attached file {} not for invoice {} for user {}", fileName, invoiceId, userId);
                    }
                }
                notificationSess.notify(userId, messageDTO);
            }
        } catch (NotificationNotFoundException e) {
            logger.warn("Invoice message not defined for entity {} Invoice email not sent", entityId);
            throw new SessionInternalError(e);
        }
    }

    /**
     * return true when user is not billable.
     * @param userId
     * @param billingDate
     * @return
     */
    private boolean skipUser(Integer userId, Date billingDate) {
        UserBL user = new UserBL(userId);
        if (!user.canInvoice()) {
            logger.debug("Skipping non-customer / subaccount user {}", userId);
            return Boolean.TRUE;
        }
        IBillableUserFilterTask userFilterTask;
        try {
            PluggableTaskTypeCategoryDTO category = new PluggableTaskTypeCategoryDAS().findByInterfaceName(IBillableUserFilterTask.class.getName());
            Integer entityId = user.getEntity().getCompany().getId();
            PluggableTaskManager<IBillableUserFilterTask> taskManager = new PluggableTaskManager<>(entityId, category.getId());
            userFilterTask = taskManager.getNextClass(); // fetch configured task for entity.
            if(null == userFilterTask) {
                logger.debug("no {} configure for entity {}", IBillableUserFilterTask.class.getSimpleName(), entityId);
                userFilterTask = basicUserFilterTask; // using default one.
            }
        } catch(PluggableTaskException taskException) {
            throw new SessionInternalError("error during loading IBillableUserFilterTask plugin ", taskException);
        }
        if(userFilterTask.isNotBillable(userId, billingDate)) {
            logger.debug("Skipping non billable user {}", userId);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Process a user, generating the invoice/s
     * @param userId
     */
    @Override
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public Integer[] processUser(Integer processId, Date billingDate, Integer userId, boolean isReview, boolean onlyRecurring) {
        UserBL user = new UserBL(userId);

        if(skipUser(userId, billingDate)) {
            return new Integer[0];
        }

        BillingProcessBL processBL = new BillingProcessBL(processId);
        BillingProcessDTO process = processBL.getEntity();

        // payment and notification only needed if this user gets a
        // new invoice.
        InvoiceDTO[] newInvoices = processBL.generateInvoice(process, null, user.getEntity(), isReview, onlyRecurring, null);

        //Update Next Invoice Date of Customer.
        if (!isReview) {
            //Update parent next invoice date.
            updateNextInvoiceDate(user, user.getDto());

            //Update childern next invoice date.
            updateChildrenNextInvoiceDate(user, user.getDto());
        }

        if (newInvoices == null) {
            if (!isReview) {
                NoNewInvoiceEvent event = new NoNewInvoiceEvent(
                        user.getEntityId(userId), userId,
                        process.getBillingDate(),
                        user.getEntity().getSubscriberStatus().getId());
                EventManager.process(event);
            }
            return new Integer[0];
        }

        Integer[] retValue = new Integer[newInvoices.length];
        for (int f = 0; f < newInvoices.length; f++) {
            retValue[f] = newInvoices[f].getId();
        }
        logger.info("The user {} has been processed. {} invoice generated", userId, retValue.length);
        return retValue;
    }

    public void updateNextInvoiceDate(UserBL userBl, UserDTO user) {
        userBl.setCustomerNextInvoiceDate(user,null);
    }

    @Override
    public BillingProcessDTOEx getDto(Integer processId, Integer languageId) {
        BillingProcessDTOEx retValue;

        BillingProcessBL process = new BillingProcessBL(processId);
        if (null == process.getEntity()) {
            throw new SessionInternalError("Billing process with id:" + processId + " does not exist.",
                    HttpStatus.SC_NOT_FOUND);
        }
        retValue = process.getDtoEx(languageId);
        if (retValue != null)
        {
            retValue.toString(); // as a form of touch
        }

        return retValue;
    }

    @Override
    public BillingProcessConfigurationDTO getConfigurationDto(Integer entityId) {
        BillingProcessConfigurationDTO retValue;

        try {
            ConfigurationBL config = new ConfigurationBL(entityId);
            retValue = config.getDTO();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    @Override
    public Integer createUpdateConfiguration(Integer executorId,
            BillingProcessConfigurationDTO dto) {
        Integer retValue;

        try {
            logger.debug("Updating configuration {}", dto);
            ConfigurationBL config = new ConfigurationBL();
            retValue = config.createUpdate(executorId, dto);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    @Override
    public Integer getLast(Integer entityId){
        try {
            BillingProcessBL process = new BillingProcessBL();
            int retValue = process.getLast(entityId);
            return retValue > 0 ? retValue : null;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public BillingProcessDTOEx getReviewDto(Integer entityId, Integer languageId) {
        BillingProcessDTOEx dto = null;
        BillingProcessBL process = new BillingProcessBL();
        dto = process.getReviewDTO(entityId, languageId);
        if (dto != null)
        {
            dto.toString(); // as a touch
        }

        return dto;
    }

    @Override
    public BillingProcessConfigurationDTO setReviewApproval(Integer executorId, Integer entityId, Boolean flag) {
        try {
            logger.debug("Setting review approval : {}", flag);
            ConfigurationBL config = new ConfigurationBL(entityId);
            config.setReviewApproval(executorId, flag);
            return getConfigurationDto(entityId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public boolean trigger (Date pToday, Integer entityId) {
        if (getMarkingElementForBillingProceessForEntity(entityId) != null) {
            logger.warn("Failed to trigger billing process at {}, another process is already running.", pToday.getTime());
            return false;
        } else {
            setMarkingElementForBillingProceessForEntity(entityId);
        }
        logger.debug("Billing trigger for {} entity {}", pToday, entityId);

        try {
            Date today = Util.truncateDate(pToday);
            processEntity(entityId, today);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        } finally {
            removeMarkingElementForBillingProceessForEntity(entityId);
        }
        return true;
    }

    @Override
    public boolean triggerAsync (Date runDate, Integer entityId) {
        new Thread(() -> trigger(runDate, entityId)).start();
        return true;
    }

    private void processEntity (Integer entityId, Date currentRunDate) {

        BillingProcessBL processBL = new BillingProcessBL();

        BillingProcessConfigurationDTO config = new ConfigurationBL(entityId).getDTO();
        Integer periodUnitId = config.getPeriodUnit().getId();
        Integer periodValue = 1;

        Date nextRunDate = config.getNextRunDate();
        boolean isReviewRequired = config.getGenerateReport() == 1;

        logger.debug("NextRunDate for entity {} is {}", entityId, nextRunDate);

        if (! nextRunDate.after(currentRunDate)) {
            // there should be a run today
            boolean doRun = true;
            EventLogger eLogger = EventLogger.getInstance();

            logger.debug("A process has to be done for entity {}", entityId);

            // check that: the configuration requires a review
            // AND, there is no partial run already there (failed)
            if (isReviewRequired && new BillingProcessDAS().isPresent(entityId, 0, nextRunDate) == null) {

                // a review had to be done for the run to go ahead
                if (! processBL.isReviewPresent(entityId)) {  // review wasn't generated
                    logger.warn("Review is required but not present for entity {}", entityId);
                    eLogger.warning(entityId, null, config.getId(),
                            EventLogger.MODULE_BILLING_PROCESS,
                            EventLogger.BILLING_REVIEW_NOT_GENERATED,
                            Constants.TABLE_BILLING_PROCESS_CONFIGURATION);

                    generateReview(entityId, nextRunDate, periodUnitId, periodValue);

                    doRun = false;

                } else if (Constants.REVIEW_STATUS_GENERATED.equals(config.getReviewStatus())) {
                    // the review has to be reviewed yet
                    int hourOfDay = new GregorianCalendar().get(GregorianCalendar.HOUR_OF_DAY);
                    logger.warn("Review is required but is not approved. Entity {} hour is {}", entityId, hourOfDay);

                    eLogger.warning(entityId, null, config.getId(),
                            EventLogger.MODULE_BILLING_PROCESS,
                            EventLogger.BILLING_REVIEW_NOT_APPROVED,
                            Constants.TABLE_BILLING_PROCESS_CONFIGURATION);
                    try {
                        // only once per day please
                        if (hourOfDay < 1) {
                            String[] params = new String[]{entityId.toString()};
                            NotificationBL.sendSapienterEmail(entityId, "process.review_waiting", null, params);
                        }
                    } catch (Exception e) {
                        logger.warn("Exception sending an entity email", e);
                    }
                    doRun = false;

                } else if (Constants.REVIEW_STATUS_DISAPPROVED.equals(config.getReviewStatus())) {
                    // is has been disapproved, let's regenerate
                    logger.debug("The process should run, but the review has been disapproved");
                    generateReview(entityId, nextRunDate, periodUnitId, periodValue);

                    doRun = false;
                }
            }
            if (doRun) {
                IBillingProcessSessionBean local = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
                local.processEntity(entityId, nextRunDate, periodUnitId, periodValue, false);
            }
        } else {
            // no run, may be then a review generation
            logger.debug("No run was scheduled. Next run on {}", nextRunDate);

            if (isReviewRequired) {
                Date reviewDate = new DateTime(nextRunDate).minusDays(config.getDaysForReport()).toDate();
                if (! reviewDate.after(currentRunDate)) {
                    if (!processBL.isReviewPresent(entityId)
                            || Constants.REVIEW_STATUS_DISAPPROVED.equals(config.getReviewStatus())) {
                        logger.debug("Review is absent or disapproved. Regenerating.");
                        generateReview(entityId, nextRunDate, periodUnitId, periodValue);
                    }
                }
            }
        } // else (no run)
    }

    /**
     * @return the id of the invoice generated
     */
    @Override
    public InvoiceDTO generateInvoice(Integer orderId, Integer invoiceId, Integer languageId, Integer executorUserId){
        try {
            BillingProcessBL process = new BillingProcessBL();
            InvoiceDTO invoice = process.generateInvoice(orderId, invoiceId, null, executorUserId, TimezoneHelper.companyCurrentDateByUserId(new OrderDAS().find(orderId).getUserId()));

            if (null != invoice) {
                invoice.touch();
            }

            return invoice;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Transactional( propagation = Propagation.NOT_SUPPORTED )
    @Override
    public void reviewUsersStatus(Integer entityId, Date today) {

        ageingRunning.putIfAbsent(entityId, Boolean.FALSE);

        if (ageingRunning.get(entityId)) {
            logger.warn("Failed to trigger ageing review process at {} , another process is already running.", today);
            return;

        } else {
            ageingRunning.put(entityId, Boolean.TRUE);
        }

        JobLauncher launcher = Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);
        logger.debug("Loaded job launcher bean # {}", launcher);

        Job job = Context.getBean(Context.Name.BATCH_JOB_AGEING_PROCESS);
        logger.debug("Loaded job bean # {}", job);

        JobParameters jobParameters = new JobParametersBuilder().addLong(Constants.BATCH_JOB_PARAM_ENTITY_ID, entityId.longValue())
                .addDate(BatchConstants.PARAM_AGEING_DATE, today)
                // following parameter was added to make ageing job unique each time
                .addDate(Constants.BATCH_JOB_PARAM_UNIQUE, TimezoneHelper.serverCurrentDate())
                .toJobParameters();
        try {
            launcher.run(job, jobParameters);
        } catch (Exception e) {
            logger.error("Job # {} with parameters # {} colud not be launched: {}",
                    job.getName(), jobParameters.toString(), e);
        }

        ageingRunning.put(entityId, Boolean.FALSE);
        logger.debug("Job for entity id # {} has finished successfully", entityId);
    }

    @Override
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public List<InvoiceDTO> reviewUserStatus(Integer entityId, Integer userId, Date today) {
        logger.debug("Trying to review user {} for date {}", userId, today);
        AgeingBL age = new AgeingBL();
        return age.reviewUserForAgeing(entityId, userId, today);
    }

    /**
     * Update status of BillingProcessRun in new transaction
     * for accessing updated entity from other thread
     * @param billingProcessId id of billing process for searching ProcessRun
     * @return id of updated ProcessRunDTO
     */
    @Override
    public Integer updateProcessRunFinished(Integer billingProcessId, Integer processRunStatusId) {
        logger.debug("Entering updateRunFinished()");
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.setProcess(billingProcessId);
        runBL.updateFinished(processRunStatusId);
        logger.debug("Leaving updateRunFinished()");
        return runBL.getEntity().getId();
    }

    @Override
    public Integer addProcessRunUser(Integer billingProcessId, Integer userId, Integer status) {
        logger.debug("Entering addProcessRunUser()");
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.setProcess(billingProcessId);
        return runBL.addProcessRunUser(userId, status).getId();
    }

    /**
     * Returns true if the Billing Process is running.
     * @param entityId
     */
    @Override
    public boolean isBillingRunning(Integer entityId) {
        if(entityId==null) {
            return false;
        }

        return getMarkingElementForBillingProceessForEntity(entityId) != null;
    }

    private void setMarkingElementForBillingProceessForEntity(Integer entityId) {
        Cache billingProcessRunning = CacheManager.getInstance().getCache("BillingProcessRunning");
        String cacheKey = String.format(BILLING_PROCESS_CACHE_KEY, entityId);
        billingProcessRunning.acquireWriteLockOnKey(cacheKey);
        billingProcessRunning.put(new Element(cacheKey, true));
        billingProcessRunning.releaseWriteLockOnKey(cacheKey);
    }

    private void removeMarkingElementForBillingProceessForEntity(Integer entityId) {
        Cache billingProcessRunning = CacheManager.getInstance().getCache("BillingProcessRunning");
        String cacheKey = String.format(BILLING_PROCESS_CACHE_KEY, entityId);
        billingProcessRunning.acquireWriteLockOnKey(cacheKey);
        billingProcessRunning.remove(cacheKey);
        billingProcessRunning.releaseWriteLockOnKey(cacheKey);
    }

    private Element getMarkingElementForBillingProceessForEntity(Integer entityId) {
        Cache billingProcessRunning = CacheManager.getInstance().getCache("BillingProcessRunning");
        String cacheKey = String.format(BILLING_PROCESS_CACHE_KEY, entityId);
        billingProcessRunning.acquireReadLockOnKey(cacheKey);
        Element element = billingProcessRunning.get(cacheKey);
        billingProcessRunning.releaseReadLockOnKey(cacheKey);
        return element;
    }

    @Override
    public ProcessStatusWS getBillingProcessStatus(Integer entityId) {
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        return runBL.getBillingProcessStatus(entityId);
    }

    @Override
    public boolean isAgeingProcessRunning(Integer entityId) {
        Boolean isRunning = ageingRunning.get(entityId);
        return isRunning != null && isRunning;
    }

    @Override
    public ProcessStatusWS getAgeingProcessStatus(Integer entityId) {
        ProcessStatusWS result = new ProcessStatusWS();
        if (isAgeingProcessRunning(entityId)) {
            result.setState(ProcessStatusWS.State.RUNNING);
        } else {
            result.setState(ProcessStatusWS.State.FINISHED);
        }
        return result;
    }

    /**
     * Returns the maximum value that Month if if period unit monthly and lastDayOfMonth flag is true,
     * For example, if the date of this instance is February 1, 2004 the actual maximum value of the DAY_OF_MONTH field
     * is 29 because 2004 is a leap year, and if the date of this instance is February 1, 2005, it's 28.
     *
     * @param billingDate
     * @return
     */
    public static Date calculateNextRunDateForEndOfMonth(Date billingDate) {

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

    /**
     * To Update Child Accounts Next Invoice Date
     * @param user
     * @param userDto
     */
    public void updateChildrenNextInvoiceDate(UserBL user, UserDTO userDto) {
        Iterator<CustomerDTO> subAccountsIt = null;
        if (userDto.getCustomer().getIsParent() != null && userDto.getCustomer().getIsParent() == 1) {
            UserBL parent = new UserBL(userDto.getUserId());
            subAccountsIt = parent.getEntity().getCustomer().getChildren().
                    iterator();
            //update child next invoice date
            updateChildNextInvoiceDate(subAccountsIt, parent);
        }
    }

    /**
     * This function updates the next invoice date of all customers in a hierarchy of parent - child and further children relationship,
     * in a recursive manner till there is no customer left out from the hierarchy." Also, sub-accounts next invoice date update
     * happen for every sub account that does not have invoice if child check box checked and parent and child  billing cycle are same.
     * @param subAccountsIt
     * @param user
     */
    public void  updateChildNextInvoiceDate(Iterator<CustomerDTO> subAccountsIt, UserBL user) {

        CustomerDTO customer = null;

        MainSubscriptionDTO parentMainSubscription = user.getDto().getCustomer().getMainSubscription();
        Integer parentBillingCycleUnit = parentMainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
        Integer parentBillingCycleValue = parentMainSubscription.getSubscriptionPeriod().getValue();

        if (subAccountsIt != null) {
            while (subAccountsIt.hasNext()) {
                customer = subAccountsIt.next();
                if (customer.isInvoiceable()) {
                    continue;
                }

                MainSubscriptionDTO childMainSubscription = customer.getBaseUser().getCustomer().getMainSubscription();
                Integer childBillingCycleUnit = childMainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
                Integer childBillingCycleValue = childMainSubscription.getSubscriptionPeriod().getValue();

                if (!customer.isInvoiceable() && (parentBillingCycleUnit.equals(childBillingCycleUnit) &&
                        parentBillingCycleValue.equals(childBillingCycleValue))) {
                    //update user next invoice date
                    updateNextInvoiceDate(user, customer.getBaseUser());
                }
                if (customer.getIsParent() != null && customer.getIsParent() == 1) {
                    UserBL parent = new UserBL(customer.getBaseUser().getUserId());
                    if (parent.getEntity() != null && parent.getEntity().getCustomer() != null && checkIfUserhasAnychildren(parent)) {
                        Iterator<CustomerDTO> subAccounts = parent.getEntity().getCustomer().getChildren().iterator();
                        updateChildNextInvoiceDate(subAccounts, parent); // Recursive function
                    }
                }
            }
        }
    }

    public boolean checkIfUserhasAnychildren(UserBL parent) {
        return (parent.getEntity().getCustomer().getChildren() != null && !parent.getEntity().getCustomer().getChildren().isEmpty());
    }

    @Override
    public Set<Integer> findInvoicesForAutoPayments(Integer entityId){
        return new InvoiceDAS().findInvoicesForAutoPayments(entityId);
    }

    @Override
    public PaymentProcessRunDTO findPaymentProcessRun(Integer billingProcessId) {

        return new PaymentProcessRunDAS().findByProcessId(billingProcessId);
    }
    @Override
    public PaymentProcessRunDTO saveOrUpdatePaymentProcessRun(PaymentProcessRunDTO paymentProcessRunDTO){
        return new PaymentProcessRunDAS().save(paymentProcessRunDTO);
    }

    /**
     * Returns true if the Payment Process is running.
     * @param entityId
     */
    @Override
    public boolean isPaymentProcessRunning(Integer entityId) {
        if(entityId==null){
            return false;
        }
        int count = getMessagesInQueue("autoPaymentProcessorsDestination");
        logger.debug("Payment messages found {}", count);
        return count > 0;
    }

    protected int getMessagesInQueue(String queueName) {
        JmsTemplate jmsTemplate = Context.getBean(Context.Name.JMS_TEMPLATE);
        return jmsTemplate.browse(queueName, (session, browser) -> {
            Enumeration<?> messages = browser.getEnumeration();
            int total = 0;
            while (messages.hasMoreElements()) {
                messages.nextElement();
                total++;
            }
            return total;
        });
    }
    @Override
    public BillingProcessDTOEx getSimpleDto(Integer processId) {
        BillingProcessDTOEx retValue;

        BillingProcessBL process = new BillingProcessBL(processId);
        retValue = process.getSimpleDtoEx();

        return retValue;
    }

    /**
     * @param entityId
     * @param linkingStartDate
     * Method links the invoices to respective billing process based on its invoiced period.
     */
    @Override
    public void linkInvoicesToBillingProcess(Integer entityId,Date linkingStartDate) {

        logger.info("in linkInvoicesToBillingProcess method with {} linkingStartDate : {}", entityId, linkingStartDate);

        Date lastBillingDate;
        BillingProcessDAS billingProcessDAS = new BillingProcessDAS();
        lastBillingDate  = null != linkingStartDate ? linkingStartDate : billingProcessDAS.getLastBillingProcessDate(entityId);
        if (null ==lastBillingDate) {
            logger.info("lastBillingDate is null {}", lastBillingDate);
            return;
        }
        Integer[] unlinkedInvoiceIds = new InvoiceBL().getBillingProcessUnlinkedInvoices(entityId,lastBillingDate);
        InvoiceDAS invoiceDAS = new InvoiceDAS();
        OrderProcessDAS orderProcessDAS = new OrderProcessDAS();

        try {
            for (Integer invoiceId : unlinkedInvoiceIds) {
                InvoiceDTO invoice = invoiceDAS.find(invoiceId);
                Date periodEndDateOfInvoice = null;
                for (OrderProcessDTO orderProcess : orderProcessDAS.getOrderProcessByInvoiceId(invoice.getId())) {
                    if(Constants.ORDER_BILLING_PRE_PAID.equals(orderProcess.getPurchaseOrder().getBillingTypeId())){
                        periodEndDateOfInvoice = orderProcess.getPeriodStart();
                    }else{
                        periodEndDateOfInvoice = orderProcess.getPeriodEnd();
                    }
                }
                if(periodEndDateOfInvoice == null) {
                    periodEndDateOfInvoice = invoice.getCreateDatetime();
                }

                Integer billingProcessId = billingProcessDAS.getBillingProcessIdForInvoice(periodEndDateOfInvoice,invoice.getBaseUser().getCompany().getId());
                if(null != billingProcessId){
                    logger.info("Linking billing process id : {} with invoice of id {} of entity {}",
                            billingProcessId, invoice.getId(), entityId);
                    invoice.setBillingProcess(billingProcessDAS.find(billingProcessId));
                    invoiceDAS.billingProceessLinkLog(invoice.getId(), billingProcessId);
                    invoice.getOrderProcesses().forEach(orderProcess-> orderProcess.setBillingProcess(invoice.getBillingProcess()));
                }
            }
        } catch (Exception e) {
            logger.error("Exception occured on linkInvoiceToBillingProcess : ", e);
            throw new SessionInternalError(e);
        }

    }

    @Override
    public List<Integer> getInvoiceIdsByBillingProcessAndByUser(Integer processId, Integer userId) {
        return new InvoiceDAS().getInvoiceIdsByBillingProcessAndUser(processId, userId);
    }

    @Override
    public Integer getInvoiceCountByBillingProcessId(Integer processId) {
        return new BillingProcessBL().getInvoiceProcessCount(processId).intValue();
    }
}