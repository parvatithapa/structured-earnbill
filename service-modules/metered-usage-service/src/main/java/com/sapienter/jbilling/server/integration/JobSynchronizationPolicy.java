package com.sapienter.jbilling.server.integration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tarun.rathor on 4/5/18.
 */
public enum JobSynchronizationPolicy {
    SERIAL(1),
    CONCURRENT(2),
    CHAINED(3),
    NONE(-1);

    Integer policyType;
    private static Map<Integer, JobSynchronizationPolicy> map= new HashMap();

    JobSynchronizationPolicy(int policyType){
        this.policyType = policyType;
    }

    static {
        for(JobSynchronizationPolicy policy: JobSynchronizationPolicy.values()){
            map.put(policy.getValue(), policy);
        }
    }

    public static JobSynchronizationPolicy valueOf(Integer policyType) {
        return map.getOrDefault(policyType, NONE);
    }

    public Integer getValue() {
        return policyType;
    }
}
