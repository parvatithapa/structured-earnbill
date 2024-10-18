package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.audit.db.EventLogDAS;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 31/10/14.
 */
public class EventLogCopyTask extends AbstractCopyTask{

    EventLogDAS eventLogDAS = null;
    CompanyDAS companyDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EventLogCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<EventLogDTO> eventLogDTOs = new EventLogDAS().getEventsByCompany(targetEntityId);
        return eventLogDTOs != null && !eventLogDTOs.isEmpty();
    }

    public EventLogCopyTask() {
        init();
    }

    private void init() {
        eventLogDAS = new EventLogDAS();
        companyDAS = new CompanyDAS();
    }


    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create EventLogCopyTask");
        CompanyDTO entity = companyDAS.find(targetEntityId);
        List<EventLogDTO> eventLogDTOs = eventLogDAS.getEventsByCompany(entityId);

        for(EventLogDTO eventLogDTO: eventLogDTOs) {
            EventLogDTO dto = new EventLogDTO(null, eventLogDTO.getJbillingTable(), null,
                    eventLogDTO.getAffectedUser(), eventLogDTO.getEventLogMessage(),
                    eventLogDTO.getEventLogModule(), entity, eventLogDTO.getForeignId(),
                    eventLogDTO.getLevelField(), null, null, null);
            eventLogDAS.save(dto);
        }
        LOG.debug("EventLogCopyTask has been completed successfully");
    }
}