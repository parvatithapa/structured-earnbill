package com.sapienter.jbilling.server.integration;

import java.util.Date;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class MeteredUsageContext {
    private int entityId;
    private int productKey;
    private String endpoint;
    private String consumerKey;
    private String consumerSecret;
    private int asyncMode;

    private long connectTimeout;
    private long readTimeout;
    private long retries;
    private long retryWait;

    private int orderStatusUploaded;
    private int orderStatusActive;
    private int orderStatusUploadFailed;
    private ChargeType chargeType;
    private Date lastSuccessMediationRunDate;
}
