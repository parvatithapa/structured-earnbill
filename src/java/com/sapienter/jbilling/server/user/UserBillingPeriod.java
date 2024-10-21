package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;

public enum UserBillingPeriod {

	MONTHLY(PeriodUnitDTO.MONTH),
	DAILY(PeriodUnitDTO.DAY),
	YEARLY(PeriodUnitDTO.YEAR),
	WEEKLY(PeriodUnitDTO.WEEK),
	SEMI_MONTHLY(PeriodUnitDTO.SEMI_MONTHLY);
	private Integer periodUnti;

	private UserBillingPeriod(Integer periodUnti) {
		this.periodUnti = periodUnti;
	}

	public Integer getPeriodUnti() {
		return periodUnti;
	}
}