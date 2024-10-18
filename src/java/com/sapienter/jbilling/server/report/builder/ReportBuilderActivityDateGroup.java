package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.report.util.ReportUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReportBuilderActivityDateGroup
 * 
 * @author Leandro Bagur
 * @since 03/10/17
 */
public class ReportBuilderActivityDateGroup extends AbstractReportBuilderActivity {

    public ReportBuilderActivityDateGroup(Integer entityId, List<Integer> childEntities, Map<String, Object> parameters) {
        super(entityId, childEntities, parameters);
    }
    
    @Override
    public List<Map<String, ?>> getData() {
        List<Map<String, ?>> data = new ArrayList<>();
        Map<Date, List<RowActivityReport>> rowsByDate = rows.stream()
                                                            .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getCreateDate));

        rowsByDate.entrySet().stream().forEach(
            entryByDate -> {
                Date date = entryByDate.getKey();
                Map<String, List<RowActivityReport>> rowsByProduct = entryByDate.getValue().stream()
                                                                        .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getProductGroup));
                rowsByProduct.entrySet().stream().forEach(
                    entryByProduct -> {
                        int totalTerm = 0;
                        int totalServices = 0;
                        Set<Integer> customers = new HashSet<>();
                        BigDecimal totalPrice = BigDecimal.ZERO;

                        for (RowActivityReport row : entryByProduct.getValue()) {
                            totalTerm += row.getTerm();
                            totalServices ++;
                            customers.add(row.getCustomerId());
                            totalPrice = totalPrice.add(row.getTotalPrice());
                        }

                        int months = totalServices * totalTerm;

                        data.add(getRow(date, entryByProduct.getKey(), totalServices, customers.size(), months,
                            totalPrice.setScale(2, BigDecimal.ROUND_DOWN),
                            totalPrice.divide(new BigDecimal(months), 2, BigDecimal.ROUND_DOWN),
                            totalPrice.divide(new BigDecimal(totalTerm), 2, BigDecimal.ROUND_DOWN)));
                    }
                );
            }
        );
        return data;
    }

    public Map<String, Object> getRow(Date createDate, String productGroup, int totalServices, int totalCustomers, int months, BigDecimal totalPrice, BigDecimal priceMonth, BigDecimal revenue) {
        BigDecimal servCust = new BigDecimal(totalServices).divide(new BigDecimal(totalCustomers), 4, BigDecimal.ROUND_DOWN);
        Map<String, Object> row = new HashMap<>();
        row.put(CREATE_DATE_COLUMN, dateFormatter.format(LocalDateTime.ofInstant(createDate.toInstant(), ZoneId.systemDefault())));
        row.put(PRODUCT_GROUP_COLUMN, productGroup);
        row.put(SERVICES_COLUMN, totalServices);
        row.put(CUSTOMERS_COLUMN, totalCustomers);
        row.put(SERVICE_CUSTOMER_COLUMN, servCust);
        row.put(TOTAL_PRICE_COLUMN, totalPrice);
        row.put(MONTHS_COLUMN, months);
        row.put(PRICE_COLUMN, priceMonth);
        row.put(REVENUE_COLUMN, revenue);
        return row;
    }
}
