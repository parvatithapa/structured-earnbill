package com.sapienter.jbilling.server.distributel;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;


@Getter
@ToString
@Builder
@Setter
public class DistributelPriceUpdateRequest {

    private int id;
    private String scheduledDateForAdjustment;
    private Integer orderId;
    private Integer customerId;
    private Integer productId;
    private BigDecimal newOrderLinePrice;
    private String status;
    private String invoiceNote;


    public DistributelPriceUpdateRequest validate() {
        if(StringUtils.isEmpty(scheduledDateForAdjustment) || null == orderId
                || null == customerId || null == productId || null == newOrderLinePrice) {
            throw new IllegalArgumentException("Price update Record has invalid data!");
        }
        return this;
    }

}
