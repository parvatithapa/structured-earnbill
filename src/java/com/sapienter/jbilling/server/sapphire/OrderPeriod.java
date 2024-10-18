package com.sapienter.jbilling.server.sapphire;

public enum OrderPeriod {

    MONTHLY(1),
    ONETIME(null),
    YEARLY(4),
    WEEKLY(2),
    SEMI_MONTHLY(5);
    private Integer periodUnti;

    private OrderPeriod(Integer periodUnti) {
        this.periodUnti = periodUnti;
    }

    public Integer getPeriodUnti() {
        return periodUnti;
    }
}
