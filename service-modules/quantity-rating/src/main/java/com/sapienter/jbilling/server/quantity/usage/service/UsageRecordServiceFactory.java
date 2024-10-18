package com.sapienter.jbilling.server.quantity.usage.service;

public interface UsageRecordServiceFactory {

    String BEAN_NAME = "usageRecordServiceFactory";

    IUsageRecordService getService(String name);
}
