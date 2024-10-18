package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;

/**
 * Created by marcomanzicore on 26/11/15.
 */
public abstract class AbstractBuilder {
    protected JbillingAPI api;
    protected TestEnvironment testEnvironment;

    protected AbstractBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        this.api = api;
        this.testEnvironment = testEnvironment;
    }

}
