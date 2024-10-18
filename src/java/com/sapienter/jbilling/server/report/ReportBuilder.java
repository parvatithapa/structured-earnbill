package com.sapienter.jbilling.server.report;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.le.api.ReportDataProvider;
import com.sapienter.jbilling.le.support.ReportDataProviderLoader;
import com.sapienter.jbilling.server.invoice.db.DistributelInvoiceDAS;
import com.sapienter.jbilling.server.report.builder.AverageRevenueData;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivity;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityDate;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityDateGroup;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityDateTerm;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityStaffDate;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityStaffDateTerm;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityStaffProduct;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityStaffTerm;
import com.sapienter.jbilling.server.report.builder.ReportBuilderActivityStaffTermCategory;
import com.sapienter.jbilling.server.report.builder.ReportBuilderAverageRevenue;
import com.sapienter.jbilling.server.report.builder.ReportBuilderRevenueDetailed;
import com.sapienter.jbilling.server.report.builder.ReportBuilderRevenueSummary;
import com.sapienter.jbilling.server.report.builder.RevenueInvoice;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 * Report Builder class
 *
 * Class to handle all reports that need a special handling and their data can not be obtained with a SQL query directly.
 *
 * @author Leandro Bagur
 * @since 01/08/17.
 */
public enum ReportBuilder implements IReportBuilder {



    REVENUE("deferred_revenue_summary") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            int month = (int) parameters.get("month");
            int year = (int) parameters.get("year");

            parameters.put("month_name", Month.of(month).name());

            ReportBuilderRevenueSummary reportBuilderRevenueSummary = new ReportBuilderRevenueSummary(month, year);
            List<RevenueInvoice> revenueInvoices = new DistributelInvoiceDAS().getRevenueInvoices(month, year, entityId);
            return reportBuilderRevenueSummary.getData(revenueInvoices);
        }
    },

    REVENUE_DETAILED("deferred_revenue_detailed") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            String group = (String) parameters.get("group");
            String invoicedEntered = (String) parameters.get("invoice_entered");
            String revenueType = (String) parameters.get("revenue_type");
            int month = (int) parameters.get("month");
            int year = (int) parameters.get("year");

            parameters.put("month_name", Month.of(month).name());

            ReportBuilderRevenueDetailed reportBuilderRevenueDetailed = new ReportBuilderRevenueDetailed(month, year);
            List<RevenueInvoice> revenueInvoices = new DistributelInvoiceDAS().getRevenueInvoices(month, year, entityId);
            return reportBuilderRevenueDetailed.getData(revenueInvoices, invoicedEntered, revenueType, group);
        }
    },

    ACTIVITY_FULL("activity_full") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivity(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_DATE("activity_date") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityDate(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_DATE_GROUP("activity_date_group") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityDateGroup(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_STAFF_DATE("activity_staff_date") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityStaffDate(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_STAFF_DATE_TERM("activity_staff_date_term") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityStaffDateTerm(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_STAFF_TERM("activity_staff_term") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityStaffTerm(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_STAFF_TERM_CATEGORY("activity_staff_term_category") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityStaffTermCategory(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_STAFF_PRODUCT("activity_staff_product") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityStaffProduct(entityId, childs, parameters).getData();
        }
    },

    ACTIVITY_DATE_TERM("activity_date_term") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            return new ReportBuilderActivityDateTerm(entityId, childs, parameters).getData();
        }
    },

    AVERAGE_REVENUE("average_revenue") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {
            int month = (int) parameters.get("month");
            int year = (int) parameters.get("year");

            parameters.put("month_name", Month.of(month).name());

            ReportBuilderAverageRevenue averageRevenueReportBuilder = new ReportBuilderAverageRevenue();
            List<AverageRevenueData> averageRevenueDataList = new DistributelInvoiceDAS().getAverageRevenueData(month, year, entityId);
            return averageRevenueReportBuilder.getData(averageRevenueDataList);
        }
    },

    PLATFORM_NET_REVENUE("platform_net_revenue") {
        @Override
        public List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters) {

            parameters.put("installation", Util.getSysProp("licensee"));

            ReportDataProviderLoader loader = Context.getBean("reportDataProviderLoader");

            ReportDataProvider instance = loader.createReportDataProvider();

            Map<String, ?> reportDataMap = instance.generateReportData();
            parameters.put(HASH, reportDataMap.get(HASH));

            // Before changing this code, please, see at correspondent parts in jbilling-le
            LocalDate endDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            LocalDate startDate = endDate.minusMonths(12);
            parameters.put("start_date", DateConvertUtils.asUtilDate(startDate));
            parameters.put("end_date", DateConvertUtils.asUtilDate(endDate.minusDays(1)));

            return (List<Map<String, ?>>) reportDataMap.get(REPORT_RECORD_LIST);
        }
    };

    public static final String REPORT_RECORD_LIST = "reportRecordList";
    public static final String HASH = "hash";
    private String name;
    
    ReportBuilder(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }

    /**
     * Get report builder by name
     * @param name name
     * @return ReportBuilder
     */
    static public ReportBuilder getReport(String name) {
        return Arrays.stream(ReportBuilder.values())
                    .filter(report -> report.getName().equals(name))
                    .findFirst()
                    .orElse(null);
    }
    
}
