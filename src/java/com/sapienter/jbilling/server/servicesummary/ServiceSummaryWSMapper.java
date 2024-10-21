package com.sapienter.jbilling.server.servicesummary;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class ServiceSummaryWSMapper implements RowMapper<ServiceSummaryWS> {

    @Override
    public ServiceSummaryWS mapRow(ResultSet rs, int i) throws SQLException {
        ServiceSummaryWS serviceSummary = new ServiceSummaryWS();

        serviceSummary.setId(rs.getInt("id"));
        serviceSummary.setInvoiceId(rs.getInt("invoice_id"));
        serviceSummary.setUserId(rs.getInt("user_id"));
        serviceSummary.setPlanId(rs.getInt("plan_id"));
        serviceSummary.setInvoiceLineId(rs.getInt("invoice_line_id"));
        serviceSummary.setItemId(rs.getInt("item_id"));
        serviceSummary.setPlanDescription(rs.getString("plan_description"));
        serviceSummary.setServiceDescription(rs.getString("service_description"));
        serviceSummary.setServiceId(rs.getString("service_id"));
        serviceSummary.setStartDate(rs.getDate("start_date"));
        serviceSummary.setEndDate(rs.getDate("end_date"));
        serviceSummary.setIsPlan(rs.getBoolean("is_plan"));
        serviceSummary.setDisplayIdentifier(rs.getString("display_identifier"));
        serviceSummary.setSubscriptionOrderId(rs.getInt("subscription_order_id"));
        serviceSummary.setCreditNoteId(0 == rs.getInt("credit_note_id") ? null : rs.getInt("credit_note_id"));
        return serviceSummary;
    }

}
