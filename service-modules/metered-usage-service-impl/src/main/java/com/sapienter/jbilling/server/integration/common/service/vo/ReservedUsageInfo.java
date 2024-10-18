package com.sapienter.jbilling.server.integration.common.service.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservedUsageInfo {

    private Integer planId;

    private BigDecimal quantity;
}
