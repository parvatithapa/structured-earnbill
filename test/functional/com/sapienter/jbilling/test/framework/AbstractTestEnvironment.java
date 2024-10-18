package com.sapienter.jbilling.test.framework;

import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import java.util.concurrent.Callable;

/**
 * Created by marcolin on 06/11/15.
 */
public abstract class AbstractTestEnvironment {

    public JbillingAPI getPrancingPonyApi() {
        return withExceptionConversion(() -> JbillingAPIFactory.getAPI());
    }

    public JbillingAPI getResellerApi() {
        return withExceptionConversion(() -> JbillingAPIFactory.getAPI("apiClient4"));
    }

    public <T> T apiFor(RemoteContext.Name api) {
        return (T) withExceptionConversion(() -> JbillingAPIFactory.getAPI(api.getName()));
    }

    public static <T> T withExceptionConversion(Callable callable) {
        try {
            return (T) callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> T apiFor(String apiName) {
    	return (T) withExceptionConversion(() -> JbillingAPIFactory.getAPI(apiName));
    }

}
