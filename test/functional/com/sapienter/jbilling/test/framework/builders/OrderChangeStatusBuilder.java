package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

/**
 * @author Bojan Dikovski
 * @since 28-JUN-2016
 */
public class OrderChangeStatusBuilder extends AbstractBuilder {

    private String description;
    private ApplyToOrder applyToOrder;
    private Integer order;
    private Integer deleted;

    private OrderChangeStatusBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static OrderChangeStatusBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        return new OrderChangeStatusBuilder(api, testEnvironment);
    }

    public OrderChangeStatusBuilder withDescription(String description) {

        this.description = description;
        return this;
    }

    public OrderChangeStatusBuilder withApplyToOrder(ApplyToOrder applyToOrder) {

        this.applyToOrder = applyToOrder;
        return this;
    }

    public OrderChangeStatusBuilder withOrder(Integer order) {

        this.order = order;
        return this;
    }

    public OrderChangeStatusBuilder withDeleted(Integer deleted) {

        this.deleted = deleted;
        return this;
    }

    public Integer build() {

        OrderChangeStatusWS statusWS = new OrderChangeStatusWS();
        statusWS.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                null == description ? "testOrderChangeStatus" : description));
        statusWS.setApplyToOrder(null == applyToOrder ? ApplyToOrder.YES : applyToOrder);
        statusWS.setOrder(null == order ? Integer.valueOf(1) : order);
        statusWS.setDeleted(null == deleted ? Integer.valueOf(0) : deleted);

        Integer statusId =  api.createOrderChangeStatus(statusWS);
        testEnvironment.add(description, statusId, description, api, TestEntityType.ORDER_CHANGE_STATUS);
        return statusId;
    }

}
