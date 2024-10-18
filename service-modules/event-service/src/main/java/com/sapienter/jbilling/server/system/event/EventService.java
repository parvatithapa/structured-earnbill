package com.sapienter.jbilling.server.system.event;

/**
 * Created by marcolin on 29/10/15.
 */
public interface EventService {
    static final String BEAN_NAME = "eventService";

    Class<Event>[] retrieveServices(Events... events);
}
