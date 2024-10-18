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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDAS;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.task.security.AgeingTask;
import com.sapienter.jbilling.server.user.CancellationRequestBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.PreferenceBL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.hibernate.ScrollableResults;

/**
 * BasicAgeingTask
 *
 * @author Brian Cowdery
 * @since 28/04/11
 */
public class BasicAgeingTask extends AgeingTask {

    /**
     *  Get all users that are about to be aged
     *
     * @param entityId
     * @param ageingDate
     * @return all users eligible for ageing
     */
    public ScrollableResults findUsersToAge(Integer entityId, Date ageingDate) {
        logger.debug("Reviewing users for entity " + entityId + " ...");
        return new UserDAS().findUserIdsWithUnpaidInvoicesForAgeing(entityId);
    }

    /**
     * Review user for the given day, and age if it has an outstanding invoices over
     * the set number of days for an ageing step.
     *
     * @param steps ageing steps
     * @param today today's date
     * @param executorId executor id
     */
    public List<InvoiceDTO> reviewUser(Integer entityId, Set<AgeingEntityStepDTO> steps, Integer userId, Date today, Integer executorId, Boolean revaluate) {
        logger.debug("Reviewing user for ageing {} ...", userId);

        UserDAS userDas = new UserDAS();
        UserDTO user = userDas.find(userId);

        InvoiceDAS invoiceDas = new InvoiceDAS();
        logger.debug("Reviewing invoices for user {}", user.getId());

        boolean isCancelledUser = false;
        // Check if the CancellationInvoiceAgeingTask is configured
        PluggableTaskWS[] pluggableTasks = PluggableTaskBL.getByClassAndEntity(entityId, CancellationInvoiceAgeingTask.class.getName());
		if(!ArrayUtils.isEmpty(pluggableTasks)){
			// check if Collection Steps for Cancelled Account are defined
			if(CollectionUtils.isNotEmpty(new AgeingEntityStepDAS().findAgeingStepsForEntity(entityId,CollectionType.CANCELLATION_INVOICE))){
				// check if the user has cancellation request with processed status
				isCancelledUser = (new CancellationRequestBL().isUserCancelled(user));
			}
		}

		List<InvoiceDTO> userOverdueInvoices = new ArrayList<>();
        if(!isCancelledUser){
            // get only regular collections steps for ageing
            Set<AgeingEntityStepDTO> regularSteps = steps.stream()
                                                         .filter(step -> step.getCollectionType()
                                                                             .equals(CollectionType.REGULAR))
                                                         .collect(Collectors.toSet());

            AgeingEntityStepDTO ageingEntityStepDTO = null;
            Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_CHRONOLOGICAL_AGEING);
            boolean chronologicalAgeing = (prefValue != null && prefValue == 1);
            List<InvoiceDTO> invoices = invoiceDas.findProccesableByUser(user);
            if (!chronologicalAgeing) {
            	for (InvoiceDTO invoice : invoices) {
            		if (!invoice.getDueDate().after(today)) {
                        ageingEntityStepDTO = ageUser(regularSteps, user, invoice, today, executorId, false, revaluate);
            			if (ageingEntityStepDTO != null) {
            				userOverdueInvoices.add(invoice);
            				break;
            			}
            		}
            	}
            } else {
            	if (!invoices.isEmpty()) {
            		InvoiceDTO invoice = invoices.get(0); // the oldest one
            		if (!invoice.getDueDate().after(today)) {
                        ageingEntityStepDTO = ageUser(regularSteps, user, invoice, today, executorId, true, revaluate);
            		}
            		if (ageingEntityStepDTO != null) {
            			userOverdueInvoices.add(invoices.get(invoices.size() - 1)); //the last with all carried
            		}
            	}
            }
        
        }

        return userOverdueInvoices;
    }

    @Override
    protected boolean allowFUPProration() {
        return true;
    }
}