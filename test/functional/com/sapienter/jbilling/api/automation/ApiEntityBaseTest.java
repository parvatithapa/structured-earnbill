package com.sapienter.jbilling.api.automation;

import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;

/**
 * Created by hristijan on 6/8/16.
 */
public class ApiEntityBaseTest {
    public Integer createCategory(String categoryCode,boolean global,
                                  TestEnvironmentBuilder testEnvironmentBuilder, JbillingAPI api){

        return testEnvironmentBuilder.itemBuilder(api)
                .itemType()
                .withCode(categoryCode)
                .global(global)
                .build();
    }
}
