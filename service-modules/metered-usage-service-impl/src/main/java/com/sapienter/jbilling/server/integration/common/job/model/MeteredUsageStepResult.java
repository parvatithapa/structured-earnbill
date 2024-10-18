package com.sapienter.jbilling.server.integration.common.job.model;


import java.util.List;

import lombok.Data;

@Data
public class MeteredUsageStepResult {
    private Integer entityId;
    private Integer userId;
    private String  accountIdenfitier;
    private Integer currencyId;
    private Integer orderId;
    private Integer languageId;

    List<MeteredUsageItem> items;
}

