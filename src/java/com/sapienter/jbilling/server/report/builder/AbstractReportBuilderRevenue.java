package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.spa.DistributelTaxType;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.TaxDistributel;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AbstractReportBuilderRevenue class
 * 
 * @author Leandro Bagur
 * @since 08/08/17.
 */
public abstract class AbstractReportBuilderRevenue {
    public static final String REPORT_GROUP_CATEGORY = "Report Group - ";
    public static final String CURRENT_INVOICE = "Current";
    public static final String PREVIOUS_INVOICE = "Previous";
    public static final String REVENUE_TYPE_EARNED = "Earned";
    public static final String REVENUE_TYPE_DEFERRED = "Deferred";
    
    protected final int month;
    protected final int year;
    
    protected AbstractReportBuilderRevenue(int month, int year) {
        this.month = month;
        this.year = year;
    }
    
    /**
     * Function to calculate earned amount from revenue invoice
     * @param revenueInvoice revenue invoice
     * @return BigDecimal
     */
    protected BigDecimal calculateEarned(RevenueInvoice revenueInvoice) {
        if (Constants.ORDER_BILLING_POST_PAID.equals(revenueInvoice.getOrderType()) ||
            (Constants.ORDER_BILLING_PRE_PAID.equals(revenueInvoice.getOrderType()) && SpaConstants.MONTHLY == revenueInvoice.getMonthTerm())) {
            return revenueInvoice.getAmount();
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate createdInvoiceLocalDate = DateConvertUtils.asLocalDate(revenueInvoice.getCreatedDate());
        long monthDiff = MONTHS.between(createdInvoiceLocalDate, startDate) + 1;
        return (revenueInvoice.getAmount().multiply(new BigDecimal(monthDiff))).divide(new BigDecimal(revenueInvoice.getMonthTerm()), 2);
    }

    /**
     * Calculate total gst or total pst/hst 
     * @param taxes taxes list
     * @param isGst is gst?
     * @return BigDecimal
     */
    protected BigDecimal calculateTotalTax(List<TaxDistributel> taxes, boolean isGst) {
        if (taxes == null) {
            return BigDecimal.ZERO;
        }
        return taxes.stream()
            .filter(tax -> (isGst) ? tax.getType() == DistributelTaxType.GTS : tax.getType() != DistributelTaxType.GTS)
            .map(TaxDistributel::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Function to get a range of years between the current and n year before
     * @param rangeSize range size
     * @return int[]
     */
    public static int[] getLastYearsFromCurrent(int rangeSize) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        return IntStream.rangeClosed(currentYear - rangeSize, currentYear).toArray();
    }

    /**
     * Get all category names by entity. Used for example to fill dropdown into deferred revenue detailed gsp.
     * @param entityId entity id
     * @return List
     */
    public static List<String> getReportGroupCategoryNamesByEntity(int entityId) {
        List<String> reportGroupCategories = new ItemTypeDAS().findByEntityId(entityId).stream()
                .filter(item -> item.getDescription().contains(REPORT_GROUP_CATEGORY))
                .map(item -> item.getDescription().split("-")[1].trim())
                .collect(Collectors.toList());
        reportGroupCategories.add(0, SpaConstants.ALL_GROUPS);
        return reportGroupCategories;
    }
}
