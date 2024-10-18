package com.sapienter.jbilling.server.integration.common.service.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class OrderInfo {
    private Integer orderId;
    private Integer languageId;
    private Integer currencyId;
    private Integer planId;
    private Date activeSince;
    private Date activeUntil;
}
