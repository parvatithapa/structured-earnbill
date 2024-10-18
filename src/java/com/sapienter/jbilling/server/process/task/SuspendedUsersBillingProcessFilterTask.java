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

import org.hibernate.ScrollableResults;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;

import java.util.Date;


/**
 * Suspended users filter task for returning the appropriate customers to run through the billing cycle.
 * The task returns all active customers + suspended customers - except the ones in the last stage of collection.
 * This task covers below points :  
 * 1. This task is useful to pick up suspended customers for billing process who otherwise can get left behind 
 *    in the billing process as their next invoice date would never be incremented once they are not picked up and left behind. 
 * 2. This task is to pick up suspended user in billing process and keep updating next invoice date in order to avoid manual intervention. 
 * 3. This task will also generate late fee orders invoices when user status is suspended.
 * 
 * @author Ashok Kale
 *
 */
public class SuspendedUsersBillingProcessFilterTask extends PluggableTask implements IBillingProcessFilterTask {

    public ScrollableResults findUsersToProcess(Integer theEntityId, Date billingDate){        
        return new BillingProcessDAS().findAllUsersExceptLastStepOfCollectionToProcess(theEntityId);              
    }
}
