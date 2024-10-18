package jbilling

import org.apache.log4j.MDC

class JBillingMDCFilters {

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                MDC.put("controller", controllerName);
                MDC.put("action", actionName ?: '');
            }
            after = { Map model ->
                MDC.remove("controller");
                MDC.remove("action");
            }
        }
    }
}
