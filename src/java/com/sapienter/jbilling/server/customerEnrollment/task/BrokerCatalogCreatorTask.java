package com.sapienter.jbilling.server.customerEnrollment.task;

import com.sapienter.jbilling.server.item.event.NewPlanEvent;
import com.sapienter.jbilling.server.item.event.PlanDeletedEvent;
import com.sapienter.jbilling.server.item.event.PlanUpdatedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.commons.lang.RandomStringUtils;

public class BrokerCatalogCreatorTask extends PluggableTask implements IInternalEventsTask {

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        NewPlanEvent.class,
        PlanUpdatedEvent.class,
        PlanDeletedEvent.class
    };

    private final CompanyDAS companyDAS = new CompanyDAS();


    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        CompanyDTO company = companyDAS.findNow(event.getEntityId());
        String catalogVersion = this.generateCatalogVersion(company.getBrokerCatalogVersion());
        company.setBrokerCatalogVersion(catalogVersion);
        companyDAS.save(company);
    }

    private String generateCatalogVersion(String brokerCatalogVersion) {
        String catalogVersion;

        do {
            catalogVersion = RandomStringUtils.randomAlphanumeric(6);
        }
        while (catalogVersion.equals(brokerCatalogVersion));

        return catalogVersion;
    }
}