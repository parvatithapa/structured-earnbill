package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.report.util.ReportUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReportBuilderActivityStaffTermCategory class.
 *
 * @author Leandro Bagur
 * @since 03/10/17
 */
public class ReportBuilderActivityStaffTermCategory extends AbstractReportBuilderActivity {
    
    public ReportBuilderActivityStaffTermCategory(Integer entityId, List<Integer> childEntities, Map<String, Object> parameters) {
        super(entityId, childEntities, parameters);
    }

    @Override
    public List<Map<String, ?>> getData() {
        List<Map<String, ?>> data = new ArrayList<>();
        Map<String, List<RowActivityReport>> rowsByStaff = rows.stream()
            .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getStaff));
        rowsByStaff.entrySet().stream().forEach(
            entryByStaff -> {
                String staff = entryByStaff.getKey();
                Map<Integer, List<RowActivityReport>> rowsByTerm = entryByStaff.getValue().stream()
                    .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getTerm));
                rowsByTerm.entrySet().stream().forEach(
                    entryByTerm -> {
                        int term = entryByTerm.getKey();
                        Map<String, List<RowActivityReport>> rowsByCategory = entryByTerm.getValue().stream()
                            .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getProductGroup));
                        rowsByCategory.entrySet().stream().forEach(
                            entryByCategory -> {
                                int totalRows = 0;
                                Set<String> services = new HashSet<>();
                                BigDecimal totalPrice = BigDecimal.ZERO;

                                for (RowActivityReport row : entryByCategory.getValue()) {
                                    totalRows ++;
                                    services.add(row.getService());
                                    totalPrice = totalPrice.add(row.getTotalPrice());
                                }
                                int months = services.size() * term;
                                data.add(getRow(staff, term, entryByCategory.getKey(), totalRows, months,
                                    totalPrice.setScale(2, BigDecimal.ROUND_DOWN),
                                    totalPrice.divide(new BigDecimal(months), 2, BigDecimal.ROUND_DOWN),
                                    totalPrice.divide(new BigDecimal(term), 2, BigDecimal.ROUND_DOWN)));        
                            }
                        );
                    }
                );
            }
        );
        return data;
    }

    public Map<String, Object> getRow(String staff, int term, String category, int total, int months, BigDecimal totalPrice, BigDecimal priceMonth, BigDecimal revenue) {
        Map<String, Object> row = new HashMap<>();
        row.put(STAFF_NAME_COLUMN, staff);
        row.put(TERM_COLUMN, term);
        row.put(CATEGORY_COLUMN, category);
        row.put(CATEGORIES_COLUMN, total);
        row.put(CUSTOMERS_COLUMN, total);
        row.put(CATEGORY_CUSTOMER_COLUMN, new BigDecimal(total).setScale(4, BigDecimal.ROUND_CEILING));
        row.put(TOTAL_PRICE_COLUMN, totalPrice);
        row.put(MONTHS_COLUMN, months);
        row.put(PRICE_COLUMN, priceMonth);
        row.put(REVENUE_COLUMN, revenue);
        return row;
    }
}
