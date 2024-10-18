package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.util.Arrays;

/**
 * @author Bojan Dikovski
 * @since 28-JUN-2016
 */
public class OrderPeriodBuilder extends AbstractBuilder {

    private String description;
    private Integer value;
    private Integer unitId;

    private OrderPeriodBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static OrderPeriodBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment){
        return new OrderPeriodBuilder(api, testEnvironment);
    }

    public OrderPeriodBuilder withDescription(String description) {

        this.description = description;
        return this;
    }

    public OrderPeriodBuilder withValue(Integer value) {

        this.value = value;
        return this;
    }

    public OrderPeriodBuilder withUnitId(Integer unitId) {

        this.unitId = unitId;
        return this;
    }

    public Integer build() {

    	OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(this.value.intValue() == period.getValue().intValue() &&
					this.unitId.intValue() == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}

        OrderPeriodWS periodWS = new OrderPeriodWS();
        periodWS.setEntityId(api.getCallerCompanyId());
        periodWS.setPeriodUnitId(null == unitId ? PeriodUnitDTO.MONTH : unitId);
        periodWS.setValue(null == value ? Integer.valueOf(1) : value);
        periodWS.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                null == description ? "testOrderPeriod" : description)));

        Integer periodId = api.createOrderPeriod(periodWS);
        testEnvironment.add(description, periodId, description, api, TestEntityType.ORDER_PERIOD);
        return periodId;
    }
}
