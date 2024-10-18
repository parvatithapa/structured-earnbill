package com.sapienter.jbilling.server.mediation.movius;

public enum CDRType {

	CALL("monthly_termination_costs_calls.jasper"),
	SMS("monthly_termination_costs_sms.jasper") ;

	private final String reportName;

	CDRType(String reportName) {
		this.reportName = reportName;
	}

    public String getReportName() {
        return reportName;
    }
}
