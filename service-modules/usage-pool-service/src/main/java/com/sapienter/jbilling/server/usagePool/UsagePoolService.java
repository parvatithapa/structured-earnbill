package com.sapienter.jbilling.server.usagePool;

import java.util.List;

/**
 * Created by marcolin on 30/10/15.
 */
public interface UsagePoolService {
    public static final String BEAN_NAME = "usagePoolService";
    List<CustomerUsagePoolWS> customerUsagePools(Integer customerId);
}
