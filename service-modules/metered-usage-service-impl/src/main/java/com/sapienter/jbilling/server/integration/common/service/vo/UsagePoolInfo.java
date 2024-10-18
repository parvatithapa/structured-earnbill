package com.sapienter.jbilling.server.integration.common.service.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UsagePoolInfo {

    private int id;
    private BigDecimal quantity;
    private String resetValue;
}
