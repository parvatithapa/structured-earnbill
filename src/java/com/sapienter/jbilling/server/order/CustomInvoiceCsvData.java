package com.sapienter.jbilling.server.order;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class CustomInvoiceCsvData {
    private Integer customerId;
    private String customInvoiceNumber;
    private String invoiceDate;
    private String invoiceDueDate;
    private List<String> sacCodes;

    private List<OrderMap> orderMaps =new ArrayList<>();
    public void addOrderMap(OrderMap orderMap) {
        orderMaps.add(orderMap);
    }
}