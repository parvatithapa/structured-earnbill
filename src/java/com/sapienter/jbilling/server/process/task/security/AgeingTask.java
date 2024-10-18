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

package com.sapienter.jbilling.server.process.task.security;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.NewActivateOrderEvent;
import com.sapienter.jbilling.server.order.event.NewSuspendOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDAS;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.process.event.ReactivatedStatusEvent;
import com.sapienter.jbilling.server.process.event.SuspendedStatusEvent;
import com.sapienter.jbilling.server.process.task.IAgeingTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.user.event.AgeingNotificationEvent;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 *
 * @author Leandro Zoi
 * @since 01/15/18
 */
public abstract class AgeingTask extends PluggableTask implements IAgeingTask {

    private static final String ACTIVE = "Active";
    private static final String SYSTEM = "System";

    protected static final Comparator<AgeingEntityStepDTO> ByDays = (AgeingEntityStepDTO s1, AgeingEntityStepDTO s2) -> s1.getDays() - s2.getDays();
    protected static final Comparator<AgeingEntityStepDTO> ByDaysReverse = (AgeingEntityStepDTO s1, AgeingEntityStepDTO s2) -> s2.getDays() - s1.getDays();
    protected static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected final EventLogger eLogger = EventLogger.getInstance();

    protected abstract boolean allowFUPProration();
    /**
     * Null safe convenience method to return the status description.
     *
     * @param status user status
     * @return description
     */
    private static String getStatusDescription(UserStatusDTO status, UserDTO user) {
        if (status != null) {
            AgeingEntityStepDTO step = status.getAgeingEntityStep();
            String description = step != null ? step.getDescription(user.getLanguage().getId()) : ACTIVE;

            return description != null ? description : StringUtils.EMPTY;

        }

        return ACTIVE;
    }

    private static String getUserLoggedName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? ((CompanyUserDetails) authentication.getPrincipal()).getPlainUsername() : SYSTEM;
    }

    /**
     * Returns true if the cancelled user requires ageing.
     *
     * @param user cancelled user being reviewed
     * @param overdueInvoice overdue invoice initiating the ageing
     * @param stepDays current ageing step days of the cancelled user
     * @param today today's date
     * @return true if cancelled user requires ageing, false if not
     */
    @Override
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today) {
        Date invoiceDueDate = Util.truncateDate(overdueInvoice.getDueDate());
        Date statusExpirationDate = DateUtils.addDays(invoiceDueDate, stepDays);

        if (statusExpirationDate.equals(today) || statusExpirationDate.before(today)) {
            logger.debug("User {} status has expired (last change {} plus {} days is before today {})", user.getId(), invoiceDueDate, stepDays, today);
            return true;
        }

        logger.debug("User {} does not need to be aged (last change {} plus {} days is after today {})", user.getId(), invoiceDueDate, stepDays, today);
        return false;
    }


    /**
     * Sets the user status to the given "aged" status. If the user status is already set to the aged status
     * no changes will be made. This method also performs an HTTP callback and sends a notification
     * message when a status change is made.
     *
     * If the user becomes suspended and can no longer log-in to the system, all of their active orders will
     * be automatically suspended.
     *
     * If the user WAS suspended and becomes active (and can now log-in to the system), any automatically
     * suspended orders will be re-activated.
     *
     * @param user user
     * @param status status to set
     * @param today today's date
     * @param executorId executor id
     */
    @Override
    public boolean setUserStatus(UserDTO user, UserStatusDTO status, Date today, Integer executorId) {
        // only set status if the new "aged" status is different from the users current status
        if (status.getId() == user.getStatus().getId()) {
            return false;
        }

        AgeingEntityStepDTO nextAgeingStep = status.getAgeingEntityStep();

        if (executorId != null) {
            // this came from the gui
            eLogger.audit(executorId,
                    user.getId(),
                    Constants.TABLE_BASE_USER,
                    user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.STATUS_CHANGE,
                    user.getStatus().getId(), null, null);
        } else {
            // this is from a process, no executor involved
            eLogger.auditBySystem(user.getCompany().getId(),
                    user.getId(),
                    Constants.TABLE_BASE_USER,
                    user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.STATUS_CHANGE,
                    user.getStatus().getId(), null, null);
        }

        // make the change
        UserStatusDTO oldStatus = user.getStatus();

        user.setUserStatus(status);
        user.setLastStatusChange(today);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                // perform callbacks and notifications after committing actual transaction
                performAgeingCallback(user, oldStatus, status);
            }

        });

        sendAgeingNotification(user, status);

        // status changed from active to suspended
        // suspend customer orders
        if (nextAgeingStep != null && nextAgeingStep.getSuspend() == 1) {
            logger.debug("Suspending orders for user {}", user.getUserId());
            setOrderStatus(user, executorId, OrderStatusFlag.INVOICE, OrderStatusFlag.SUSPENDED_AGEING, today);
        } else {
            // status changed from suspended to active
            // adding reactivate entry in customer status change history
            EventManager.process(new ReactivatedStatusEvent(user, DateConvertUtils.getNow(), user.getEntity().getId(), getUserLoggedName(),
                    getStatusDescription(status, user), status));
            // re-active suspended customer orders
            if (nextAgeingStep == null && status.getId() == UserDTOEx.STATUS_ACTIVE
                    && oldStatus.getAgeingEntityStep() != null && oldStatus.getAgeingEntityStep().getSuspend() == 1) {
                logger.debug("Activating orders for user {}", user.getUserId());
                // user out of ageing, activate suspended orders
                setOrderStatus(user, executorId, OrderStatusFlag.SUSPENDED_AGEING, OrderStatusFlag.INVOICE, today);
            }
        }

        // emit NewUserStatusEvent
        NewUserStatusEvent event = new NewUserStatusEvent(user, user.getCompany().getId(), oldStatus.getId(), status.getId());
        EventManager.process(event);
        return true;
    }

    private boolean performAgeingCallback(UserDTO user, UserStatusDTO oldStatus, UserStatusDTO newStatus) {
        String url = null;
        try {
            url = PreferenceBL.getPreferenceValue(user.getEntity().getId(), Constants.PREFERENCE_URL_CALLBACK);

        } catch (EmptyResultDataAccessException e) {
            /* ignore, no callback preference configured */
        }

        if (url != null && url.length() > 0) {
            try {
                logger.debug("Performing ageing HTTP callback for URL: {}", url);

                // cook the parameters to be sent
                NameValuePair[] data = new NameValuePair[6];
                data[0] = new NameValuePair("cmd", "ageing_update");
                data[1] = new NameValuePair("user_id", String.valueOf(user.getId()));
                data[2] = new NameValuePair("login_name", user.getUserName());
                data[3] = new NameValuePair("from_status", String.valueOf(oldStatus.getId()));
                data[4] = new NameValuePair("to_status", String.valueOf(newStatus.getId()));
                data[5] = new NameValuePair("can_login", String.valueOf(newStatus.getCanLogin()));

                // make the call
                HttpClient client = new HttpClient();
                client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
                PostMethod post = new PostMethod(url);
                post.setRequestBody(data);
                client.executeMethod(post);

            } catch (Exception e) {
                logger.error("Exception occurred posting ageing HTTP callback for URL: {}, {}", url, e);
                return false;
            }
        }
        return true;
    }

    private boolean sendAgeingNotification(UserDTO user, UserStatusDTO newStatus) {

        AgeingEntityStepDTO nextStep = newStatus.getAgeingEntityStep();

        if (nextStep == null || nextStep.getSendNotification() == 1) {
            logger.debug("Sending notification to user {} during ageing/reactivating", user.getUserId());
            // process the ageing notification event to find and send the notification message for the ageing step
            try {
                EventManager.process(new AgeingNotificationEvent(user.getEntity().getId(),
                        user.getLanguage().getId(),
                        nextStep != null ? newStatus.getId() : null,
                                user.getId()));

            } catch (Exception exception) {
                logger.warn("Cannot send notification on ageing: {}", user.getId());
            }
        }

        return true;
    }

    protected boolean isUserAlreadyPassAgeingStep(UserStatusDTO userStatus, AgeingEntityStepDTO step,
            List<AgeingEntityStepDTO> orderedSteps, boolean chronologicalAgeing) {

        AgeingEntityStepDTO currentStep = userStatus.getAgeingEntityStep();
        if (step == null && currentStep == null) {
            return true; // same status
        } else if (currentStep == null) { // now user is active and does not take part in ageing process
            return false;
        } else if (step == null) {
            return false; //now user ageing, but should be active
        } else {
            int currentIndex = orderedSteps.indexOf(currentStep);
            int nextIndex = orderedSteps.indexOf(step);
            if (chronologicalAgeing) {
                return nextIndex == currentIndex;
            }
            return nextIndex <= currentIndex;
        }
    }

    protected boolean validateUserWithOutOverDueInvoice(UserDTO user, Integer excludedInvoiceId, Date effectiveDate) {
        try {
            if (UserBL.isUserBalanceEnoughToAge(user, effectiveDate) &&
                    new InvoiceBL().isUserWithOverdueInvoices(user.getUserId(), companyCurrentDate(), excludedInvoiceId)) {
                logger.debug("User {} still has overdue invoices, cannot remove from ageing.", user.getId());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Exception occurred checking for overdue invoices.", e);
            return true;
        }

        return false;
    }

    private void setOrderStatus(UserDTO user, Integer executorId, OrderStatusFlag flag, OrderStatusFlag flagTo, Date today) {
        OrderBL orderBL = new OrderBL();
        ScrollableResults orders = null;

        try{
            orders = new OrderDAS().findByUser_Status(user.getId(), flag);
            while (orders.next()) {
                OrderDTO order = (OrderDTO) orders.get()[0];
                orderBL.set(order);
                orderBL.setStatus(executorId, new OrderStatusDAS().getDefaultOrderStatusId(flagTo, order.getUser().getCompany().getId()));
                if (allowFUPProration() && order.hasPlanWithFreeUsagePool()) {
                    if (flagTo.equals(OrderStatusFlag.SUSPENDED_AGEING)) {
                        EventManager.process(new NewSuspendOrderEvent(order.getId(), today));
                    } else if (flagTo.equals(OrderStatusFlag.INVOICE)) {
                        EventManager.process(new NewActivateOrderEvent(order.getId(), order.getPlantWithUsagePool(), today));
                    }
                }
            }
        } finally {
            if (orders != null) {
                orders.close();
            }
        }
    }

    protected AgeingEntityStepDTO getStep(List<AgeingEntityStepDTO> ageingSteps, UserDTO user, InvoiceDTO unpaidInvoice,
            Date todayTruncated, boolean chronologicalAgeing) {
        for (AgeingEntityStepDTO step : ageingSteps) {
            // run this step
            if (isAgeingRequired(user, unpaidInvoice, step.getDays(), todayTruncated)) {
                // possible multiple runs at a day, check status
                if (!isUserAlreadyPassAgeingStep(user.getStatus(), step, ageingSteps, chronologicalAgeing)) {
                    logger.debug("User: {} needs to be aged to '{}'", user.getId(), getStatusDescription(step.getUserStatus(), user));
                    return step;
                } else {
                    if (chronologicalAgeing) {
                        break;
                    }
                }
            }
        }

        return null;
    }


    /**
     * Fetch the appropriate ageing status as per number of overdue period.
     */
    protected AgeingEntityStepDTO getRevaluatedStep(List<AgeingEntityStepDTO> ageingSteps, InvoiceDTO unpaidInvoice, Date today) {
        for (AgeingEntityStepDTO step : ageingSteps) {
            Date invoiceDueDate = Util.truncateDate(unpaidInvoice.getDueDate());
            Date statusExpirationDate = DateUtils.addDays(invoiceDueDate, step.getDays());
            if (statusExpirationDate.equals(today)) {
                logger.debug("User {} status has expired (last change {} plus {} days is today {})", unpaidInvoice.getUserId(), invoiceDueDate, step.getDays(), today);
                return step;
            } else if (statusExpirationDate.after(today)) {
                // Fetch previous steps of collection because statusExpirationDate is after todays date.
                int previousIndex = ageingSteps.indexOf(step)-1;
                return ageingSteps.get(previousIndex);
            }
        }
        return null;
    }

    protected void setStatus(UserStatusDTO nextStatus, UserDTO user, Date today) {
        // set status
        if (nextStatus != null) {
            setUserStatus(user, nextStatus, today, null);
        } else {
            logger.debug("Next status of user {} is null, no further ageing steps are available.", user.getId());
            eLogger.warning(user.getEntity().getId(),
                    user.getUserId(),
                    user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.NO_FURTHER_STEP,
                    Constants.TABLE_BASE_USER);
        }
    }

    /**
     * Moves a user one step forward in the ageing process.The
     * user will only be moved if enough days have passed from their overdue invoice due date
     * Return the next ageing step
     *
     * @param steps ageing steps
     * @param user user to age
     * @param today today's date
     * @param chronologicalAgeing if it is chronological ageing
     * @return the resulting ageing step for the user after ageing
     */
    @Override
    public AgeingEntityStepDTO ageUser(Set<AgeingEntityStepDTO> steps, UserDTO user, InvoiceDTO unpaidInvoice, Date today, Integer executorId,
            boolean chronologicalAgeing, Boolean revaluate) {
        if (!UserBL.isUserBalanceEnoughToAge(user,today)) {
            logger.debug("Wants to age user: {} but invoice balance is not enough to age: {}", user.getId(), unpaidInvoice.getId());
            return null;
        }

        logger.debug("Ageing user {} for unpaid invoice: ", user.getId(), unpaidInvoice.getId());
        List<AgeingEntityStepDTO> ageingSteps = new LinkedList<>(steps);
        if(chronologicalAgeing){
            Collections.sort(ageingSteps, ByDaysReverse);
        } else {
            Collections.sort(ageingSteps, ByDays);
        }

        Date todayTruncated = Util.truncateDate(today);

        AgeingEntityStepDTO ageingStep = null;
        if (getAgeRevaluationPreferenceValue(user.getEntity().getId()) && revaluate) {
            ageingStep = getRevaluatedStep(ageingSteps, unpaidInvoice, todayTruncated);
        } else {
            ageingStep = getStep(ageingSteps, user, unpaidInvoice, todayTruncated, chronologicalAgeing);
        }

        suspendUser(user, today, ageingStep.getUserStatus());
        setStatus(ageingStep.getUserStatus(), user, todayTruncated);

        return ageingStep;
    }


    /**
     * Removes a user from the ageing process (makes them active), ONLY if they do not
     * still have overdue invoices.
     *
     * @param user user to make active
     * @param excludedInvoiceId invoice id to ignore when determining if the user CAN be made active
     * @param executorId executor id
     */
    @Override
    public void removeUser(UserDTO user, Integer excludedInvoiceId, Integer executorId, Date effectiveDate) {
        UserStatusDTO userStatusDTO = user.getUserStatus();
        // validate that the user actually needs a status change
        if (UserDTOEx.STATUS_ACTIVE.equals(userStatusDTO.getId())) {
            logger.debug("User {} is already active, no need to remove from ageing.", user.getId());
            return;
        }

        if (Constants.CUSTOMER_CANCELLATION_STATUS_DESCRIPTION.equals(user.getUserStatus().getDescription(user.getLanguage().getId()))) {
            logger.debug("User {} is already in Cancelled on Request, no need to remove from ageing.", user.getId());
            return;
        }

        AgeingEntityStepDTO currentAgeingStep = userStatusDTO.getAgeingEntityStep();

        // Here also need to check if Customer has requested for the cancellation of the services.
        if (currentAgeingStep != null && currentAgeingStep.getStopActivationOnPayment() == 1) {
            logger.debug("Current ageing step has stop activation on payment checked, therefore cannot remove user {} from ageing", user.getId());
            return;
        }

        // validate that the user does not still have overdue invoices
        if (validateUserWithOutOverDueInvoice(user, excludedInvoiceId, effectiveDate)) {
            if (getAgeRevaluationPreferenceValue(user.getEntity().getId())) {
                Integer entityId = user.getEntity().getId();
                CompanyDTO company = new EntityBL(entityId).getEntity();
                reviewUser(entityId, company.getAgeingEntitySteps(), user.getId(), new Date(), executorId, Boolean.TRUE);
            }
            return;
        }

        // If Collection type is in Cancellation invoice the set user status to Cancelled on Request else set status as active.
        List<UserStatusDTO> list = new AgeingEntityStepDAS().findUserStatusDTOForEntity(user.getEntity().getId(), CollectionType.CANCELLATION_INVOICE);
        UserStatusDTO status;
        if (!CollectionUtils.isEmpty(list) && list.contains(user.getUserStatus())){
            status = new UserStatusDAS().findByDescription(Constants.CUSTOMER_CANCELLATION_STATUS_DESCRIPTION, user.getLanguage().getId());
        }else{
            status = new UserStatusDAS().find(UserDTOEx.STATUS_ACTIVE);
        }

        // make the status change.
        logger.debug("Removing user {} from ageing (making active).", user.getUserId());
        EventManager.process(new ReactivatedStatusEvent(user, effectiveDate, user.getEntity().getId(), getUserLoggedName(),
                getStatusDescription(status, user), status));
        setStatus(status, user, effectiveDate);
    }

    public static void suspendUser(UserDTO user, Date today, UserStatusDTO userStatus) {
        EventManager.process(new SuspendedStatusEvent(user, today, user.getEntity().getId(), getUserLoggedName(),
                getStatusDescription(userStatus, user), userStatus));
    }

    private boolean getAgeRevaluationPreferenceValue(Integer entityId) {
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_AGEING_REVALUATION);
        return (prefValue != null && prefValue == 1);
    }
}
