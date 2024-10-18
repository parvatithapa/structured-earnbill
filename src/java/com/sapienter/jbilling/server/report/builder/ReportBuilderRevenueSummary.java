package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.report.util.ReportUtil;
import com.sapienter.jbilling.server.spa.DistributelTaxHelper;
import com.sapienter.jbilling.server.spa.TaxDistributel;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.drools.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReportBuilderRevenueSummary report
 * 
 * @author Leandro Bagur
 * @since 08/08/17.
 */
public class ReportBuilderRevenueSummary extends AbstractReportBuilderRevenue {

    public ReportBuilderRevenueSummary(int month, int year) {
        super(month, year);
    }
    
    /**
     * Process invoices list
     * @param revenueInvoices revenue invoices
     */
    public List<Map<String, ?>> getData(List<RevenueInvoice> revenueInvoices) {
        DistributelTaxHelper distributelTaxHelper = new DistributelTaxHelper();
        List<RowRevenueSummary> allRows = new ArrayList<>();
        ItemDAS itemDAS = new ItemDAS();
        UserDAS userDAS = new UserDAS();
        for (RevenueInvoice revenueInvoice : revenueInvoices) {
            String currency = revenueInvoice.getCurrency();
            LocalDate createdInvoice = DateConvertUtils.asLocalDate(revenueInvoice.getCreatedDate());
            String invoicedEntered = createdInvoice.getMonthValue() == month ? CURRENT_INVOICE : PREVIOUS_INVOICE;
            String province = revenueInvoice.getProvince();

            ItemDTO item = itemDAS.find(revenueInvoice.getItemId());
            String category = getCategoryDescription(item.getItemTypes());
            if (StringUtils.isEmpty(category)) {
                continue;
            }

            BigDecimal earned = calculateEarned(revenueInvoice);
            if (earned.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            UserDTO user = userDAS.find(revenueInvoice.getUserId());
            // Add earned row
            List<TaxDistributel> taxesEarnedCategory = null;
            if (!revenueInvoice.isTaxExempt()) {
                taxesEarnedCategory = distributelTaxHelper.getTaxList(user, revenueInvoice.getCreatedDate(), earned, province);
            }
            allRows.add(new RowRevenueSummary.RowRevenueSummaryBuilder()
                                            .invoiceId(revenueInvoice.getInvoiceId())
                                            .invoicedEntered(invoicedEntered)
                                            .revenueType(REVENUE_TYPE_EARNED)
                                            .category(category)
                                            .currency(currency)
                                            .amount(earned)
                                            .pstHst(calculateTotalTax(taxesEarnedCategory, false))
                                            .gst(calculateTotalTax(taxesEarnedCategory, true))
                                            .province(province)
                                            .build());

            BigDecimal deferred = revenueInvoice.getAmount().subtract(earned);
            if (deferred.compareTo(BigDecimal.ZERO) == 0) continue;

            List<TaxDistributel> taxesDeferredCategory = null;
            if (!revenueInvoice.isTaxExempt()) {
                taxesDeferredCategory = distributelTaxHelper.getTaxList(user, revenueInvoice.getCreatedDate(), deferred);
            }
            allRows.add(new RowRevenueSummary.RowRevenueSummaryBuilder()
                                            .invoiceId(revenueInvoice.getInvoiceId())
                                            .invoicedEntered(invoicedEntered)
                                            .revenueType(REVENUE_TYPE_DEFERRED)
                                            .category(category)
                                            .currency(currency)
                                            .amount(deferred)
                                            .pstHst(calculateTotalTax(taxesDeferredCategory, false))
                                            .gst(calculateTotalTax(taxesDeferredCategory, true))
                                            .province(province)
                                            .build());
        }
        return getGroupAndSortData(allRows);
    }

    /**
     * Get category description
     * @param itemTypes item types
     * @return String
     */
    private String getCategoryDescription(Set<ItemTypeDTO> itemTypes) {
        String categoryDescription = itemTypes.stream()
                                              .filter(category -> category.getDescription().contains(REPORT_GROUP_CATEGORY))
                                              .map(ItemTypeDTO::getDescription)
                                              .findFirst()
                                              .orElse(StringUtils.EMPTY);
        return (!categoryDescription.isEmpty()) ? categoryDescription.split("-")[1].trim() : categoryDescription;
    }
    
    /**
     * Get a new row
     * @param invoiceEntered Current or Previous
     * @param revenue        Deferred or Earned
     * @param group          Product category name
     * @param amount         total amount
     * @param province    tax province
     * @return Map
     */
    private Map<String, Object> getRowData(String invoiceEntered, String revenue, String group, String currency,
                                           BigDecimal amount, BigDecimal totalPstHst, BigDecimal totalGst, String province, long totalInvoices) {
        Map<String, Object> row = new HashMap<>();
        row.put("invoiced_entered", invoiceEntered);
        row.put("revenue", revenue);
        row.put("group", group);
        row.put("currency", currency);
        row.put("amount", amount);
        row.put("pstHst", totalPstHst);
        row.put("gst", totalGst);
        row.put("province", province);
        row.put("nro_invoices", totalInvoices);
        return row;
    }
    
    private List<Map<String, ?>> getGroupAndSortData(List<RowRevenueSummary> allRows) {
        List<Map<String, ?>> data = new ArrayList<>();
        Map<String, List<RowRevenueSummary>> rowsByCategory = allRows.stream()
            .collect(ReportUtil.sortedGroupingBy(RowRevenueSummary::getCategory));
        rowsByCategory.forEach(
            (category, rowByCategory) -> {
                Map<String, List<RowRevenueSummary>> rowsByProvince = rowByCategory.stream()
                    .collect(ReportUtil.sortedGroupingBy(RowRevenueSummary::getProvince));
                rowsByProvince.forEach(
                    (province, rowByProvince) -> {
                        Map<String, List<RowRevenueSummary>> rowsByInvoicedEntered = rowByProvince.stream()
                            .collect(ReportUtil.sortedGroupingBy(RowRevenueSummary::getInvoicedEntered));
                        rowsByInvoicedEntered.forEach(
                            (invoicedEntered, rowByInvoicedEntered) -> {
                                Map<String, List<RowRevenueSummary>> rowsByRevenueType= rowByInvoicedEntered.stream()
                                    .collect(ReportUtil.sortedGroupingBy(RowRevenueSummary::getRevenueType));
                                rowsByRevenueType.forEach(
                                    (revenueType, rowByRevenueType) -> {
                                        Map<String, List<RowRevenueSummary>> rowsByCurrency = rowByRevenueType.stream()
                                            .collect(ReportUtil.sortedGroupingBy(RowRevenueSummary::getCurrency));
                                        rowsByCurrency.forEach(
                                            (currency, rows) -> {
                                                BigDecimal totalAmount = rows.stream().map(RowRevenueSummary::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                                                BigDecimal totalPstHst = rows.stream().map(RowRevenueSummary::getPstHst).reduce(BigDecimal.ZERO, BigDecimal::add);
                                                BigDecimal totalGst = rows.stream().map(RowRevenueSummary::getGst).reduce(BigDecimal.ZERO, BigDecimal::add);
                                                long totalInvoices = rows.stream().map(RowRevenueSummary::getInvoiceId).distinct().count();
                                                data.add(getRowData(invoicedEntered, revenueType, category, currency, totalAmount, totalPstHst, totalGst, province, totalInvoices));
                                            });
                                    });
                            });
                    });
            });
        return data;
    }
}
