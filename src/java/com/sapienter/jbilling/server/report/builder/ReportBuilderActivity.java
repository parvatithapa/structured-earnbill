package com.sapienter.jbilling.server.report.builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReportBuilderActivity class.
 * 
 * @author Leandro Bagur
 * @since 29/09/17.
 */
public class ReportBuilderActivity extends AbstractReportBuilderActivity {
    
    private final static Comparator<RowActivityReport> FULL_COMPARATOR = (RowActivityReport o1, RowActivityReport o2) -> {
        int result = o1.getStaff().compareTo(o2.getStaff());
        if (result == 0) {
            result = o1.getCreateDate().compareTo(o2.getCreateDate());
            if (result == 0) {
                result = o1.getProductId().compareTo(o2.getProductId());
            }
        }
        return result;
    }; 
    
    public ReportBuilderActivity(Integer entityId, List<Integer> childEntities, Map<String, Object> parameters) {
        super(entityId, childEntities, parameters);
    }
    
    public List<Map<String, ?>> getData() {
        rows.sort(FULL_COMPARATOR);
        return rows.stream()
                   .map(row -> getRow(row.getStaff(), row.getCreateDate(), row.getProductId(),
                                        row.getProductName(), row.getProductGroup(), row.getTerm(), row.getTotalPrice()))
                   .collect(Collectors.toList());
    }
    
    protected Map<String, Object> getRow(String staffName, Date createDate, Integer itemId, String itemName, String productGroup, int term, BigDecimal totalPrice) {        
        Map<String, Object> row = new HashMap<>();
        row.put(STAFF_NAME_COLUMN, staffName);
        row.put(CREATE_DATE_COLUMN, dateFormatter.format(LocalDateTime.ofInstant(createDate.toInstant(), ZoneId.systemDefault())));
        row.put(PRODUCT_ID_COLUMN, itemId);
        row.put(PRODUCT_NAME_COLUMN, itemName);
        row.put(PRODUCT_GROUP_COLUMN, productGroup);
        row.put(TERM_COLUMN, term);
        row.put(SERVICES_COLUMN, 1);
        row.put(CUSTOMERS_COLUMN, 1);
        row.put(SERVICE_CUSTOMER_COLUMN, 1);
        row.put(TOTAL_PRICE_COLUMN, totalPrice.setScale(2, BigDecimal.ROUND_DOWN));
        row.put(MONTHS_COLUMN, term);
        row.put(PRICE_COLUMN, totalPrice.divide(new BigDecimal(term), 2, BigDecimal.ROUND_DOWN));
        row.put(REVENUE_COLUMN, totalPrice.divide(new BigDecimal(term), 2, BigDecimal.ROUND_DOWN));
        return row;
    }
    
}
