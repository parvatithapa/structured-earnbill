package com.sapienter.jbilling.server.util.audit;

import com.sapienter.jbilling.common.InvalidArgumentException;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;

import java.util.Date;

/**
 * Created by Martin on 10/7/2016.
 */
public class EventLogRepresentation {
    EventLogDTO eventLogDTO;

    public EventLogRepresentation(EventLogDTO eventLog){
        if(eventLog==null) throw new InvalidArgumentException("EventLogDTO can not be null!",500);
        eventLogDTO=eventLog;
    }
    private Integer getAffectedUser(){
        return eventLogDTO.getAffectedUser()!=null?eventLogDTO.getAffectedUser().getId():null;
    }
    private String getTableName(){
        return eventLogDTO.getJbillingTable()!=null?eventLogDTO.getJbillingTable().getName():"";
    }
    private Integer getModuleId() {
        return eventLogDTO.getEventLogModule() != null ? eventLogDTO.getEventLogModule().getId() : null;
    }
    private String getMessage(){
        return eventLogDTO.getEventLogMessage()!=null?eventLogDTO.getEventLogMessage().getDescription():"";
    }

    @Override
    public String toString() {

        return new LogMessage.Builder().message(getMessage())
                .affectedUser(getAffectedUser())
                .table(getTableName())
                .moduleId(getModuleId())
                .rowId(eventLogDTO.getForeignId())
                .oldInt(eventLogDTO.getOldNum()).oldStr(eventLogDTO.getOldStr()).oldDate(eventLogDTO.getOldDate())
                .build().toString();
    }
}
