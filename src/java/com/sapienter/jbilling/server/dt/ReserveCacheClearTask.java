package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.DtReserveInstanceCache;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.event.ReserveCacheEvent;

public class ReserveCacheClearTask extends PluggableTask implements IInternalEventsTask {
    
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{ ReserveCacheEvent.class };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if(event instanceof ReserveCacheEvent) {
            DtReserveInstanceCache.cleanUp(((ReserveCacheEvent) event).getKey());
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
