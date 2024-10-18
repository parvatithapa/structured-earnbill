package com.sapienter.jbilling.server.integration.common.service.vo;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderLineInfo {
    private Integer orderId;
    private Integer orderLineId;
    private Integer itemId;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String description;
    private Date createDateTime;
}
