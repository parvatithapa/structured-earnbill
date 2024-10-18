package com.sapienter.jbilling.server.spc.wookie.crm;

import java.util.Map;

import com.sapienter.jbilling.server.system.event.Event;

public class SpcOutBoundInterchangeRequest {
    private final Event event;
    private final Map<String, String> parameters;

    SpcOutBoundInterchangeRequest(final Event event, final Map<String, String> parameters) {
        this.event = event;
        this.parameters = parameters;
    }

    public Event getEvent() {
        return event;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

}