package com.sapienter.jbilling.server.item.db;

public enum FreeTrialPeriod {
    DAYS("free.trial.period.day"),
    MONTHS("free.trial.period.month"),
    YEARS("free.trial.period.year"),
    BILLING_CYCLE("free.trial.period.billing.cycle");

    private final String messageKey;

    FreeTrialPeriod(String messageKey) {
        this.messageKey = messageKey;
    }

    String getMessageKey() {
        return this.messageKey;
    }

    String getKey(){ return name();}

    public static final FreeTrialPeriod[] ALL = {DAYS, MONTHS, YEARS, BILLING_CYCLE};
}
