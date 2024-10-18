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

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;

import org.hibernate.ScrollableResults;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * IAgeingTask
 *
 * @author Brian Cowdery
 * @since 28/04/11
 */
public interface IAgeingTask {

    /**
     * Retrieve all the users that are candidates for the ageing process
     *
     * @param entityId company id
     * @param ageingDate date when the ageing was initiated
     * @return users cursor-candidates for ageing
     */
    public ScrollableResults findUsersToAge(Integer entityId, Date ageingDate);

    /**
     *  Review the user and evaluate if the user has to be aged
     *
     * @param entityId company id
     * @param steps ageing steps defined per company
     * @param userId id if the user to be reviews for ageing
     * @param today today's date
     * @param executorId executor id
     */
    public List<InvoiceDTO> reviewUser(Integer entityId, Set<AgeingEntityStepDTO> steps, Integer userId, Date today, Integer executorId, Boolean revaluate);

    /**
     * Age the user by moving the user one step forward in the ageing process
     *
     * @param steps ageing steps
     * @param user user to age
     * @param overdueInvoice ovedue invoice
     * @param today today's date
     * @param executorId executor id
     * @param chronologicalAgeing if it is chronological ageing
     * @return the resulting ageing step for the user after ageing
     */
    public AgeingEntityStepDTO ageUser(Set<AgeingEntityStepDTO> steps, UserDTO user, InvoiceDTO overdueInvoice, Date today, Integer executorId, boolean chronologicalAgeing, Boolean revaluate);

    /**
     * Removes a user from the ageing process (makes them active).
     *
     * @param user user to make active
     * @param excludedInvoiceId invoice id to ignore when determining if the user CAN be made active
     * @param executorId executor id
     * @param effectiveDate date in where the user wil change the status history
     */
    public void removeUser(UserDTO user, Integer excludedInvoiceId, Integer executorId, Date effectiveDate);

    /**
     * Returns true if the user requires ageing.
     *
     * @param user user being reviewed
     * @param overdueInvoice earliest overdue invoice
     * @param today today's date
     * @return true if user requires ageing, false if not
     */
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today);

    /**
     * Sets the users status.
     *
     * @param user user
     * @param status status to set
     * @param today today's date
     * @param executorId executor id
     */
    public boolean setUserStatus(UserDTO user, UserStatusDTO status, Date today, Integer executorId);

}
