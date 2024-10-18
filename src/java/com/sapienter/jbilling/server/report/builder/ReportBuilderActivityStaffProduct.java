package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.report.util.ReportUtil;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportBuilderActivityStaffProduct class.
 *
 * @author Leandro Bagur
 * @since 03/10/17
 */
public class ReportBuilderActivityStaffProduct extends AbstractReportBuilderActivity {
    
    public ReportBuilderActivityStaffProduct(Integer entityId, List<Integer> childEntities, Map<String, Object> parameters) {
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
                Map<Integer, List<RowActivityReport>> rowsByProduct = entryByStaff.getValue().stream()
                    .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getProductId));
                rowsByProduct.entrySet().stream().forEach(
                    entryByProduct -> {
                        int productId = entryByProduct.getKey();

                        Map<Date, List<RowActivityReport>> rowsByDate = entryByProduct.getValue().stream()
                            .collect(ReportUtil.sortedGroupingBy(RowActivityReport::getCreateDate));

                        rowsByDate.entrySet().stream().forEach(
                            entryByDate -> {
                                int totalRows = 0;
                                int totalTerm = 0;
                                BigDecimal totalPrice = BigDecimal.ZERO;

                                for (RowActivityReport row : entryByDate.getValue()) {
                                    totalRows++;
                                    totalTerm += row.getTerm();
                                    totalPrice = totalPrice.add(row.getTotalPrice());
                                }
                                data.add(getRow(staff, productId, entryByDate.getKey(), totalTerm, totalRows,
                                    totalPrice.setScale(2, BigDecimal.ROUND_DOWN),
                                    totalPrice.divide(new BigDecimal(totalTerm), 2, BigDecimal.ROUND_DOWN)));
                            }
                        );
                    }
                );
            }
        );
        return data;
    }

    public Map<String, Object> getRow(String staff, Integer productId, Date createDate, int term, int total, BigDecimal totalPrice, BigDecimal priceRevenue) {
        Map<String, Object> row = new HashMap<>();
        row.put(STAFF_NAME_COLUMN, staff);
        row.put(PRODUCT_ID_COLUMN, productId);
        row.put(CREATE_DATE_COLUMN, dateFormatter.format(TimezoneHelper.convertToTimezone(LocalDateTime.ofInstant(createDate.toInstant(), ZoneId.systemDefault()),
                TimezoneHelper.getCompanyLevelTimeZone(entityId))));
        row.put(TERM_COLUMN, term);
        row.put(CATEGORIES_COLUMN, total);
        row.put(CUSTOMERS_COLUMN, total);
        row.put(CATEGORY_CUSTOMER_COLUMN, new BigDecimal(total).setScale(4, BigDecimal.ROUND_CEILING));
        row.put(TOTAL_PRICE_COLUMN, totalPrice);
        row.put(MONTHS_COLUMN, term);
        row.put(PRICE_COLUMN, priceRevenue);
        row.put(REVENUE_COLUMN, priceRevenue);
        return row;
    }
}
