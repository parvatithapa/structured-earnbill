package com.sapienter.jbilling.api.automation.orders;

import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static com.sapienter.jbilling.test.TestUtils.buildDescriptions;

/**
 * @author Vojislav Stanojevikj
 * @since 29-Jun-2016.
 */
public enum OrdersTestHelper {

    INSTANCE();

    public Integer buildAndPersistCustomer(TestEnvironmentBuilder environmentBuilder,
                                           String username, Date nextInvoiceDate,
                                           Integer orderPeriodId, JbillingAPI api){

        UserWS userWS = environmentBuilder
                .customerBuilder(api)
                .withUsername(username)
                .addTimeToUsername(false)
                .build();

        if (null != nextInvoiceDate){
            DateTime nid = new DateTime(nextInvoiceDate);
            userWS.setMainSubscription(new MainSubscriptionWS(orderPeriodId, nid.getDayOfMonth()));
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
        }

        return userWS.getId();
    }

    public Integer buildAndPersistOrder(TestEnvironmentBuilder environmentBuilder, String code, Integer userId, Date activeSince,
                                        Date activeUntil, Integer orderPeriodId,
                                        JbillingAPI api, Map<Integer, BigDecimal> productQuantityMap) {
        OrderBuilder orderBuilder = environmentBuilder
                .orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withPeriod(orderPeriodId);

        for (Map.Entry<Integer, BigDecimal> entry : productQuantityMap.entrySet()){
            orderBuilder.withOrderLine(
                    orderBuilder.orderLine()
                            .withItemId(entry.getKey())
                            .withQuantity(entry.getValue())
                            .build());
        }

        return orderBuilder.build();
    }

    public OrderPeriodWS getOrCreateOrderPeriod(Integer periodUnit, Integer value, String description, JbillingAPI api) {
        for (OrderPeriodWS period : api.getOrderPeriods()) {
            if (period.getPeriodUnitId().intValue() == periodUnit.intValue()
                    && period.getValue().intValue() == value.intValue()) {
                return period;
            }
        }
        return createOrderPeriod(periodUnit, value, description, api);
    }

    private OrderPeriodWS createOrderPeriod(Integer periodUnit, Integer value, String description, JbillingAPI api) {
        OrderPeriodWS orderPeriod = new OrderPeriodWS(999, api.getCallerCompanyId(), periodUnit, value);
        orderPeriod.setDescriptions(buildDescriptions(new InternationalDescriptionWS(api.getCallerLanguageId(),
                description)));
        orderPeriod.setId(api.createOrderPeriod(orderPeriod));

        return orderPeriod;
    }
}
