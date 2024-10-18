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

package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import org.apache.log4j.Logger;

/**
 *  Provision partner customers to external system.
 *
 *  Customer is a partner customer if the value for the partner metafield
 *  matches the partner value provided as a task parameter
 *
 *  @author Panche Isajeski
 *  @since 12/05/2012
 */
public class ExternalCustomerProvisioningTask extends PluggableTask implements IInternalEventsTask {

    private static Logger LOG = Logger.getLogger(ExternalCustomerProvisioningTask.class);

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            NewContactEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        // TODO (pai) implement customer provisioning to external system

    }
}
