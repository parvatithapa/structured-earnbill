package com.sapienter.jbilling.server.integration.common.service.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservedPlanInfo {
    private int entityId;
    private int planId;
    private String description;
    private BigDecimal price;
    private String duration;
    private String paymentOption;

}
