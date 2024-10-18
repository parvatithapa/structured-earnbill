package com.sapienter.jbilling.server.integration.common.service.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderLineTierInfo {

    private Integer orderLineid;
    private Integer tierNumber;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal tierFrom;
    private BigDecimal tierTo;


}
