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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.ScrollableResults;

/**
 * CancellationInvoiceAgeingTask : to handle invoices for cancelled users
 *
 * @author Leandro Zoi
 * @since 11/27/2017
 */
public class CancellationInvoiceAgeingTask extends AgeingTask {

    /**
     *  Get all the cancelled users that are about to be aged
     *
     * @param entityId
     * @param ageingDate
     * @return all cancelled users eligible for ageing
     */
    public ScrollableResults findUsersToAge(Integer entityId, Date ageingDate) {
        logger.debug("Reviewing cancelled users for entity {} ...", entityId);
        return new UserDAS().findUserIdsWithUnpaidInvoicesForAgeing(entityId);
    }

    /**
     * Review cancelled user for the given day, and age if it has an outstanding invoices over
     * the set number of days for an ageing step.
     *
     * @param steps ageing steps
     * @param today today's date
     * @param executorId executor id
     */
    public List<InvoiceDTO> reviewUser(Integer entityId, Set<AgeingEntityStepDTO> steps, Integer userId, Date today, Integer executorId, Boolean revaluate) {
        logger.debug("Reviewing cancelled user for ageing {} ...", userId);
        UserDAS userDas = new UserDAS();
        UserDTO user = userDas.find(userId);

        InvoiceDAS invoiceDas = new InvoiceDAS();
        logger.debug("Reviewing invoices for user {}", user.getId());
        // check if the user has cancellation request with processed status
        boolean isCancelledUser = false;
        // Check if the CancellationInvoiceAgeingTask is configured
        PluggableTaskWS[] pluggableTasks = PluggableTaskBL.getByClassAndEntity(entityId,CancellationInvoiceAgeingTask.class.getName());
		if(pluggableTasks.length == 1){
			// check if Collection Steps for Cancelled Account are defined
			if(CollectionUtils.isNotEmpty(new AgeingEntityStepDAS().findAgeingStepsForEntity(
  					entityId,CollectionType.CANCELLATION_INVOICE))){
				// check if the user has cancellation request with processed status
				isCancelledUser = (new CancellationRequestBL().isUserCancelled(user));
			}
		}

        List<InvoiceDTO> userOverdueInvoices = new ArrayList<>();
        if(isCancelledUser){
            // get only collections steps for cancelled accounts for ageing
            Set<AgeingEntityStepDTO> cancelledSteps = new HashSet<>();
            for(AgeingEntityStepDTO ageingEntityStepDTO : steps){
            	if(ageingEntityStepDTO.getCollectionType().equals(CollectionType.CANCELLATION_INVOICE)){
            		cancelledSteps.add(ageingEntityStepDTO);
            	}
            }

        	AgeingEntityStepDTO ageingEntityStepDTO = null;
            Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_CHRONOLOGICAL_AGEING);
            boolean chronologicalAgeing = (prefValue != null && prefValue == 1);
            List<InvoiceDTO> invoices = invoiceDas.findProccesableByUser(user);
            if (!chronologicalAgeing) {
    	        for (InvoiceDTO invoice : invoices) {
    	        	if (!invoice.getDueDate().after(today)) {
    	        		logger.debug("Ageing invoice for Cancelled Accounts {}", invoice.getId());
                        ageingEntityStepDTO = ageUser(cancelledSteps, user, invoice, today, executorId,false, revaluate);
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
                    	logger.debug("Ageing invoice for Cancelled Accounts {}", invoice.getId());
                        ageingEntityStepDTO = ageUser(cancelledSteps, user, invoice, today, executorId, true, revaluate);
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
        return false;
    }
}