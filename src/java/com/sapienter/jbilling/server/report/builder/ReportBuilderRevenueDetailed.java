package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.spa.DistributelTaxHelper;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.TaxDistributel;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ReportBuilderRevenueDetailed report
 * 
 * @author Leandro Bagur 
 * @since 08/08/17.
 */
public class ReportBuilderRevenueDetailed extends AbstractReportBuilderRevenue {

    /**
     * Invoice number is an String attribute but need to be sorted as Integer to get the desired result.
     */
    private static final Comparator<Map<String, ?>> INVOICE_NUMBER_COMPARATOR = (Map<String, ?> o1, Map<String, ?> o2) -> {
            Integer number1 = Integer.valueOf(o1.get("invoice_number").toString());
            Integer number2 = Integer.valueOf(o2.get("invoice_number").toString());
            return number1.compareTo(number2);
    };

    /**
     * Comparator for ordering rows first by group and after that by invoice number.
     */
    private static final Comparator<Map<String, ?>> INVOICE_NUMBER_GROUP_COMPARATOR = (Map<String, ?> o1, Map<String, ?> o2) -> {
        int groupComparison = o1.get("group").toString().compareTo(o2.get("group").toString());
        if (groupComparison != 0) {
            return  groupComparison;
        }
        Integer number1 = Integer.valueOf(o1.get("invoice_number").toString());
        Integer number2 = Integer.valueOf(o2.get("invoice_number").toString());
        return number1.compareTo(number2);
    };
    
    public ReportBuilderRevenueDetailed(int month, int year) {
        super(month, year);
    }
    
    /**
     * Get data
     * @param revenueInvoices revenue invoices
     * @param invoicedEntered invoice entered (Current/Previous)
     * @param revenueType revenue type (Earned/Deferred)
     * @param paramGroup paramGroup
     */
    public List<Map<String, ?>> getData(List<RevenueInvoice> revenueInvoices, String invoicedEntered, String revenueType, String paramGroup) {
        DistributelTaxHelper distributelTaxHelper = new DistributelTaxHelper();
        List<Map<String, ?>> data = new ArrayList<>();
        ItemDAS itemDAS = new ItemDAS();
        UserDAS userDAS = new UserDAS();
        Map<String, List<RevenueInvoice>> rowsByInvoiceNumber = revenueInvoices.stream()
                                .collect(Collectors.groupingBy(RevenueInvoice::getInvoiceNumber));
        boolean isAllGroups = SpaConstants.ALL_GROUPS.equals(paramGroup);
        rowsByInvoiceNumber.forEach(
            (invoiceNumber, revenueInvoicesByNumber) -> {
                Set<GroupRow> invoiceGroupList = new HashSet<>();
                BigDecimal totalAmount;
                BigDecimal totalPstHst;
                BigDecimal totalGst;
                String customerName = null;
                String currency = null;
                String province = null;
                boolean first = true;
                
                for (RevenueInvoice revenueInvoice : revenueInvoicesByNumber) {
                    UserDTO user = userDAS.find(revenueInvoice.getUserId());
                    if (first) {
                        customerName = user.getUserName();
                        currency = revenueInvoice.getCurrency();
                        province = revenueInvoice.getProvince();
                        first = false;
                    }
                    
                    LocalDate createdInvoice = DateConvertUtils.asLocalDate(revenueInvoice.getCreatedDate());

                    if ((invoicedEntered.equalsIgnoreCase(CURRENT_INVOICE) && createdInvoice.getMonthValue() != month) ||
                            (invoicedEntered.equalsIgnoreCase(PREVIOUS_INVOICE) && createdInvoice.getMonthValue() == month) ||
                            (invoicedEntered.equalsIgnoreCase(PREVIOUS_INVOICE) && SpaConstants.MONTHLY == revenueInvoice.getMonthTerm())) {
                        continue;
                    }
                    
                    ItemDTO item = itemDAS.find(revenueInvoice.getItemId());
                    ItemTypeDTO itemType = item.getItemTypes().stream().filter(type -> type.getDescription().startsWith(REPORT_GROUP_CATEGORY)).findFirst().orElse(null);
                    if (itemType == null) {
                        continue;
                    }
                    String itemReportGroup = itemType.getDescription().replace(REPORT_GROUP_CATEGORY, "");
                    if (!isAllGroups && !paramGroup.equals(itemReportGroup)) {
                        continue;
                    }
                    String groupRowId = invoiceNumber + itemReportGroup;
                    GroupRow groupRow = invoiceGroupList.stream().filter(gr -> gr.id.equals(groupRowId)).findFirst().orElse(null);
                    if (groupRow == null) {
                        totalAmount = BigDecimal.ZERO;
                        totalPstHst = BigDecimal.ZERO;
                        totalGst = BigDecimal.ZERO;
                    } else {
                        totalAmount = groupRow.totalAmount;
                        totalPstHst = groupRow.totalPstHst;
                        totalGst = groupRow.totalGst;
                    }
                    
                    BigDecimal earned = calculateEarned(revenueInvoice);
                    BigDecimal amount = (revenueType.equalsIgnoreCase(REVENUE_TYPE_EARNED)) ? earned : revenueInvoice.getAmount().subtract(earned);

                    if (amount.compareTo(BigDecimal.ZERO) == 0) continue;

                    totalAmount = totalAmount.add(amount);
                    if (!revenueInvoice.isTaxExempt()) {
                        List<TaxDistributel> taxes = distributelTaxHelper.getTaxList(user, revenueInvoice.getCreatedDate(), amount, revenueInvoice.getProvince());
                        totalPstHst = totalPstHst.add(calculateTotalTax(taxes, false));
                        totalGst = totalGst.add(calculateTotalTax(taxes, true));
                    }
                    
                    if (groupRow != null) {
                        invoiceGroupList.remove(groupRow);
                    }
                    invoiceGroupList.add(new GroupRow(groupRowId, itemReportGroup, totalAmount, totalPstHst, totalGst));
                }

                for (GroupRow gr : invoiceGroupList) {
                    data.add(getRowData(gr.group, invoiceNumber, customerName, currency, gr.totalAmount, gr.totalPstHst, gr.totalGst, province));
                }
        });
        
        data.sort(isAllGroups ? INVOICE_NUMBER_GROUP_COMPARATOR : INVOICE_NUMBER_COMPARATOR);
        return data;
    }
    
    /**
     * Get row data 
     * @param amount amount
     * @param pstHst pst/hst
     * @param gst gst
     * @param taxProvince tax province
     * @return Map
     */
    private Map<String, Object> getRowData(String group, String invoiceNumber, String customerName, String currency, BigDecimal amount, BigDecimal pstHst, BigDecimal gst, String taxProvince) {
        Map<String, Object> row = new HashMap<>();
        row.put("group", group);
        row.put("invoice_number", invoiceNumber);
        row.put("customer_name", customerName);
        row.put("currency", currency);
        row.put("amount", amount);
        row.put("pstHst", pstHst);
        row.put("gst", gst);
        row.put("tax_province", taxProvince);
        return row;
    }
    
    private class GroupRow {
        private String id;
        private String group;
        private BigDecimal totalAmount;
        private BigDecimal totalPstHst;
        private BigDecimal totalGst;

        public GroupRow(String id, String group, BigDecimal totalAmount, BigDecimal totalPstHst, BigDecimal totalGst) {
            this.id = id;
            this.group = group;
            this.totalAmount = totalAmount;
            this.totalPstHst = totalPstHst;
            this.totalGst = totalGst;
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof GroupRow && id.equals(((GroupRow)o).id);
        }
        
        @Override
        public int hashCode() {
            return id.concat(group).hashCode();
        }
    }

}
