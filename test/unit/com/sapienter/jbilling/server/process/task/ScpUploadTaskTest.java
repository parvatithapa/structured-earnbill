/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.Util;
import junit.framework.TestCase;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Brian Cowdery
 * @since 08-06-2010
 */
public class ScpUploadTaskTest extends TestCase {

    private static final String BASE_DIR = Util.getSysProp("base_dir");
    private static final File PATH = new File(BASE_DIR);
    private static final String PATH_TO_LOGOS = BASE_DIR + File.separator + "logos";

    public static final List<String> EXPECTED_LOGO_FILES = Arrays.asList("agl_bpay_logo.png", "agl_logo.png", "agl_post_bill_pay.png", "austpost_bill_pay.png", "bpay_logo.png", "chat.png", "computer.png", "credit_card.png", "direct_debit.png","entity-1-jbilling.png", "entity-1.png", "mail.png", "phone.png");

    public static final List<String> EXPECTED_REPORT_FILES = Arrays.asList(
            "agl_invoice.jasper",
            "agl_invoice_account_charges_sub_report.jasper",
            "agl_invoice_adjustments_sub_report.jasper",
            "agl_invoice_bar_graph_sub_report.jasper",
            "agl_invoice_bar_graph_sub_sub_report.jasper",
            "agl_invoice_last_billing_summary_sub_report2.jasper",
            "agl_invoice_line_asset_summary.jasper",
            "agl_invoice_line_asset_summary_subreport.jasper",
            "agl_invoice_line_report.jasper",
            "agl_invoice_payment_instruments_sub_report.jasper",
            "agl_invoice_payments_received_subreport.jasper",
            "agl_invoice_plan_group.jasper",
            "agl_invoice_plan_group_sub_report.jasper",
            "agl_invoice_subreport.jasper",
            "commission_run.jasper",
            "distributel_payman_details_sub.jasper",
            "distributel_tax_details_sub.jasper",
            "invoice_design.jasper",
            "invoice_design_ac.jasper",
            "invoice_design_ac_summary_of_charges.jasper",
            "invoice_design_ac_uk.jasper",
            "invoice_design_ac_userid_summary.jasper",
            "invoice_design_adjustments_section_fc_hosted.jasper",
            "invoice_design_amount_of_last_statement_fc.jasper",
            "invoice_design_charges_ac_uk.jasper",
            "invoice_design_fc.jasper",
            "invoice_design_fc_hosted.jasper",
            "invoice_design_fees_section_fc_hosted.jasper",
            "invoice_design_monthly_charges_section_fc_hosted.jasper",
            "invoice_design_page2.jasper",
            "invoice_design_payments_and_refunds_fc.jasper",
            "invoice_design_payments_and_refunds_fc_hosted.jasper",
            "invoice_design_payments_received_fc.jasper",
            "invoice_design_payments_refunds_ac_uk.jasper",
            "invoice_design_sub.jasper",
            "invoice_design_sub_ac_uk.jasper",
            "invoice_design_subreport_adjustments_fc.jasper",
            "invoice_design_subreport_fees_fc.jasper",
            "invoice_design_subreport_taxes_fc.jasper",
            "invoice_design_summary_of_charges_fc.jasper",
            "invoice_design_taxes_section_fc_hosted.jasper",
            "invoice_design_usage_charges_section_fc_hosted.jasper",
            "invoice_design_usage_charges_section_group_by_plan_sub_totals_fc_hosted.jasper",
            "invoice_design_userid_summary_fc.jasper",
            "invoice_design_userid_summary_subreport_group_sub_totals_fc.jasper",
            "invoice_taxes_ac_uk.jasper",
            "movius_invoice_template.jasper",
            "movius_invoice_template_3.jasper",
            "movius_invoice_template_5.jasper",
            "nges_invoice_design.jasper",
            "one_time_invoice_note_fc_hosted.jasper",
            "origination_charges.jasper",
            "payment_notification_attachment.jasper",
            "payment_notification_spc.jasper",
            "simple_invoice.jasper",
            "simple_invoice_b2b.jasper",
            "simple_invoice_telco.jasper",
            "simple_invoice_telco_events.jasper",
            "spc_invoice_adjustments_v3.jasper",
            "spc_invoice_bar_graph_sub_report.jasper",
            "spc_invoice_bar_graph_sub_report_v2.jasper",
            "spc_invoice_bar_graph_sub_report_v3.jasper",
            "spc_invoice_bpay_info_sub_report.jasper",
            "spc_invoice_bpay_info_sub_report2.jasper",
            "spc_invoice_bpay_info_sub_report2_v2.jasper",
            "spc_invoice_bpay_info_sub_report2_v3.jasper",
            "spc_invoice_bpay_info_sub_report_v2.jasper",
            "spc_invoice_bpay_info_sub_report_v3.jasper",
            "spc_invoice_last_billing_summary_sub_report.jasper",
            "spc_invoice_last_billing_summary_sub_report2.jasper",
            "spc_invoice_last_billing_summary_sub_report2_v2.jasper",
            "spc_invoice_last_billing_summary_sub_report2_v3.jasper",
            "spc_invoice_last_billing_summary_sub_report_adjustments_credit_v3.jasper",
            "spc_invoice_last_billing_summary_sub_report_adjustments_debit_v3.jasper",
            "spc_invoice_last_billing_summary_sub_report_v2.jasper",
            "spc_invoice_last_billing_summary_sub_report_v3.jasper",
            "spc_invoice_line_asset_summary.jasper",
            "spc_invoice_line_asset_summary_subreport.jasper",
            "spc_invoice_line_asset_summary_subreport_v2.jasper",
            "spc_invoice_line_asset_summary_subreport_v3.jasper",
            "spc_invoice_line_asset_summary_v2.jasper",
            "spc_invoice_line_asset_summary_v3.jasper",
            "spc_invoice_line_report.jasper",
            "spc_invoice_line_report_v2.jasper",
            "spc_invoice_line_report_v3.jasper",
            "spc_invoice_line_sub_report1.jasper",
            "spc_invoice_line_sub_report2.jasper",
            "spc_invoice_line_sub_report2_v2.jasper",
            "spc_invoice_line_sub_report2_v3.jasper",
            "spc_invoice_line_sub_report3.jasper",
            "spc_invoice_main_report.jasper",
            "spc_invoice_main_report_v2.jasper",
            "spc_invoice_main_report_v3.jasper",
            "spc_invoice_plan_group_sub_report_v2.jasper",
            "spc_invoice_plan_group_sub_report_v3.jasper",
            "spc_invoice_plan_group_v2.jasper",
            "spc_invoice_plan_group_v3.jasper",
            "subscription_charges.jasper",
            "subscription_charges_5.jasper",
            "summary_of_account_history.jasper",
            "summary_of_account_history_fc.jasper",
            "summary_of_account_history_fc_hosted.jasper",
            "summary_of_account_history_new_customer.jasper",
            "summary_of_account_history_subreport_adjustements_total_fc.jasper",
            "summary_of_account_history_subreport_late_fees_fc.jasper",
            "summary_of_account_history_subreport_new_charges_fc.jasper",
            "summary_of_account_history_subreport_taxes_and_fees_fc.jasper",
            "termination_charges_call_sms.jasper",
            "termination_charges_call_sms_3.jasper",
            "total_due.jasper",
            "total_due_fc.jasper");

    private ScpUploadTask task = new ScpUploadTask(); // task under test

    public void testCollectFilesNonRecursive() throws Exception {
        File path = new File(PATH_TO_LOGOS);

        List<File> files = task.collectFiles(path, ".*\\.png", false);
        Collections.sort(files);

        assertEquals(13, files.size());
        assertEquals(EXPECTED_LOGO_FILES, files.stream().map(File::getName).collect(Collectors.toList()));
    }

    public void testCollectFilesRecursive() throws Exception {
        // all jasper report designs are in a sub directory of the root
        // jbilling/resources/ path, and won't be found unless we scan recursively.
        List<File> nonRecursive = task.collectFiles(PATH, ".*designs.*\\.jasper", false);
        assertEquals(0, nonRecursive.size());

        // jasper report designs from jbilling/resources/designs/
        // found because we're scanning recursively.
        List<File> files = task.collectFiles(PATH, ".*designs.*\\.jasper", true);
        Collections.sort(files);

        assertEquals(111, files.size());
        assertEquals(EXPECTED_REPORT_FILES, files.stream().map(File::getName).collect(Collectors.toList()));
    }

    public void testCollectFilesCompoundRegex() throws Exception {
        // look for multiple files recursively matching *.jasper and *.jpg
        // should find files in jbilling/resources/design/ and jbilling/resources/logos/
        List<File> files = task.collectFiles(PATH, "(.*designs.*\\.jasper|.*\\.png)", true);
        Collections.sort(files);

        List<String> allExpectedFiles = new ArrayList<>(EXPECTED_REPORT_FILES);
        allExpectedFiles.addAll(EXPECTED_LOGO_FILES);

        assertEquals(124, files.size());
        assertEquals(allExpectedFiles, files.stream().map(File::getName).collect(Collectors.toList()));
    }
    
}

