package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.report.util.ReportUtil;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

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
 * ReportBuilderActivityStaffDateTerm class.
 *
 * @author Leandro Bagur
 * @since 03/10/17
 */
public class ReportBuilderActivityStaffDateTerm extends AbstractReportBuilderActivity {
    
    public ReportBuilderActivityStaffDateTerm(Integer entityId, List<Integer> childEntities, Map<String, Object> parameters) {
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
                Map<Date, List<RowActivityReport>> rowsByDate = entryByStaff.getValue().stream()
                    .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getCreateDate));
                rowsByDate.entrySet().stream().forEach(
                    entryByDate -> {
                        Date date = entryByDate.getKey();

                        Map<Integer, List<RowActivityReport>> rowsByTerm = entryByDate.getValue().stream()
                                                                            .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getTerm));
                        rowsByTerm.entrySet().stream().forEach(
                            entryByTerm -> {
                                int term = entryByTerm.getKey();
                                int totalRows = 0;
                                Set<Integer> customers = new HashSet<>();
                                BigDecimal totalPrice = BigDecimal.ZERO;

                                for (RowActivityReport row : entryByTerm.getValue()) {
                                    totalRows ++;
                                    customers.add(row.getCustomerId());
                                    totalPrice = totalPrice.add(row.getTotalPrice());
                                }
                                int months = totalRows * term;
                                data.add(getRow(staff, date, term, totalRows, customers.size(), months,
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

    public Map<String, Object> getRow(String staff, Date createDate, int term, int totalCategories, int totalCustomers, int months, BigDecimal totalPrice, BigDecimal priceMonth, BigDecimal revenue) {
        BigDecimal catCust = new BigDecimal(totalCategories).divide(new BigDecimal(totalCustomers), 4, BigDecimal.ROUND_DOWN);
        Map<String, Object> row = new HashMap<>();
        row.put(STAFF_NAME_COLUMN, staff);
        row.put(CREATE_DATE_COLUMN, dateFormatter.format(TimezoneHelper.convertToTimezone(LocalDateTime.ofInstant(createDate.toInstant(), ZoneId.systemDefault()),
                TimezoneHelper.getCompanyLevelTimeZone(entityId))));
        row.put(TERM_COLUMN, term);
        row.put(CATEGORIES_COLUMN, totalCategories);
        row.put(CUSTOMERS_COLUMN, totalCustomers);
        row.put(CATEGORY_CUSTOMER_COLUMN, catCust);
        row.put(TOTAL_PRICE_COLUMN, totalPrice);
        row.put(MONTHS_COLUMN, months);
        row.put(PRICE_COLUMN, priceMonth);
        row.put(REVENUE_COLUMN, revenue);
        return row;
    }
}
