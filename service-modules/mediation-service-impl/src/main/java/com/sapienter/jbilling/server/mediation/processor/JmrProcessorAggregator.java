package com.sapienter.jbilling.server.mediation.processor;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

import java.util.Collection;

/**
 * Interface for any class which wants to aggregate JMRs during the JMR processing stage.
 */
public interface JmrProcessorAggregator {

    /**
     * Clear any cache data. This will be called once per customer.
     */
    public void clear();

    /**
     * Check if a JMR must be aggregated. The JMR will have all properties set.
     * Perform aggregation at the same time.
     *
     * @param jmr
     * @return true if the JMR was aggregated.
     */
    public boolean aggregate(JbillingMediationRecord jmr);

    /**
     * Return all aggregate JMRs which was created for the user.
     * @return
     */
    public Collection<JbillingMediationRecord> getAggregates();
}
