package com.sapienter.jbilling.server.integration.common.appdirect.client;

import com.sapienter.jbilling.appdirect.vo.UsageBean;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.FreeSubscriptionExpiredException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.NetworkTimeoutException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionNotFoundException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionUsageNotAllowed;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.UnAuthorizedTransientException;


@FunctionalInterface
public interface IntegrationClient {
     boolean send(UsageBean usageBean) throws
                                        UnAuthorizedTransientException,
                                        FreeSubscriptionExpiredException,
                                        SubscriptionNotFoundException,
                                        SubscriptionUsageNotAllowed,
                                        NetworkTimeoutException;
}
