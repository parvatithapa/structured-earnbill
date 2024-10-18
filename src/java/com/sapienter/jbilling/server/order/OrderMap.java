package com.sapienter.jbilling.server.order;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class OrderMap {

    private String orderNote;
    private String tabType;
    private Integer orderPeriod;
    private Map<String, String> orderLineMap =new HashMap<>();

    public void addOrderLine(String productCode, String quantity) {
        orderLineMap.put(productCode, quantity);
    }

}
