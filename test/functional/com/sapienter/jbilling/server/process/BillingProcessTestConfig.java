/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.process;

import static com.sapienter.jbilling.test.TestUtils.buildDescriptions;

import com.sapienter.jbilling.api.automation.orders.OrdersTestHelper;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;

@Configuration
public class BillingProcessTestConfig {

    @Autowired
    private JbillingAPI api;

    @Bean
    public OrderPeriodWS daily () {
        return OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(Constants.PERIOD_UNIT_DAY, 1, "Daily", api);
    }

    @Bean
    public OrderPeriodWS weekly () {
        return OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(Constants.PERIOD_UNIT_WEEK, 1, "Weekly", api);
    }

    @Bean
    public OrderPeriodWS semiMonthly () {
        return OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(Constants.PERIOD_UNIT_SEMI_MONTHLY, 1, "Semi-Monthly", api);
    }

    @Bean
    public OrderPeriodWS monthly () {
        return OrdersTestHelper.INSTANCE.getOrCreateOrderPeriod(Constants.PERIOD_UNIT_MONTH, 1, "Monthly", api);
    }

    @Bean
    public OrderChangeStatusWS applyToOrderYes () {
        for (OrderChangeStatusWS status : api.getOrderChangeStatusesForCompany()) {
            if (ApplyToOrder.YES.equals(status.getApplyToOrder()))
                return status;
        }
        throw new BeanCreationException("applyToOrderYes", "YES status not found in list of statuses for company");
    }

}
