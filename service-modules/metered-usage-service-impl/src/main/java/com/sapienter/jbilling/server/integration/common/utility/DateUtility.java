package com.sapienter.jbilling.server.integration.common.utility;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.sapienter.jbilling.server.integration.common.service.vo.ChargePeriod;

public class DateUtility {

	private DateUtility() {}

	public static void setTimeToEndOfDay(Calendar calendar) {

		calendar.set(Calendar.HOUR_OF_DAY, 11);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
	}

	public static void setTimeToStartOfDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	public static Date earliest(Date a, Date b) {

		if (a == null || b == null) {
			return a == null ? b : a;
		}

		return a.before(b) ? a : b;
	}

	public static Integer numberOfDaysInMonth(Date date) {

		if (date == null) {
			return 0;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	public static Date lastDayOfMonth(Date date) {
		if (date == null) {
			return null;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int lastDateInt = calendar.getActualMaximum(Calendar.DATE);
		calendar.set(Calendar.DATE, lastDateInt);
		return calendar.getTime();
	}

	public static int getDifferenceInMonths(Date fromDate, Date toDate) {

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(fromDate);
		int fromMonth = calendar.get(Calendar.MONTH);
		int fromYear = calendar.get(Calendar.YEAR);

		calendar.setTime(toDate);
		int toMonth = calendar.get(Calendar.MONTH);
		int toYear = calendar.get(Calendar.YEAR);

		return ((toYear - fromYear) * calendar.getMaximum(Calendar.MONTH)) +
			(toMonth - fromMonth);
	}

	public static Optional<Date> addMonthsToDate(Date date, int noOfMonths) {

		if (date == null) {
			return  Optional.empty();
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, noOfMonths);
		return  Optional.of(calendar.getTime());
	}

	public static Date addDaysToDate(Date date, int noOfDays) {

		if (date == null) {
			return  null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, noOfDays);
		return  calendar.getTime();
	}

	public static Set<ChargePeriod> divideRangeMonthWise(Date fromDate, Date toDate) {

		Set<ChargePeriod> chargePeriods = new HashSet<>();

		if (fromDate == null || toDate == null || fromDate.after(toDate)) {
			return chargePeriods;
		}

		Calendar calendarToDate = Calendar.getInstance();
		calendarToDate.setTime(toDate);
		setTimeToEndOfDay(calendarToDate);
		toDate = calendarToDate.getTime();

		Calendar calendarFirstDay = Calendar.getInstance();
		calendarFirstDay.setTime(fromDate);
		setTimeToStartOfDay(calendarFirstDay);

		Calendar calendarlastDay = Calendar.getInstance();
		calendarlastDay.setTime(lastDayOfMonth(fromDate));
		setTimeToEndOfDay(calendarlastDay);
		Date lastDay = calendarlastDay.getTime();

		do {

			chargePeriods.add(ChargePeriod.builder()
				.firstDay(calendarFirstDay.getTime())
				.lastDay(earliest(lastDay, toDate))
				.build());

			calendarFirstDay.add(Calendar.MONTH, 1);
			calendarFirstDay.set(Calendar.DAY_OF_MONTH, 1);
			setTimeToStartOfDay(calendarFirstDay);

			calendarlastDay.setTime(lastDayOfMonth(calendarFirstDay.getTime()));
			setTimeToEndOfDay(calendarlastDay);
			lastDay = calendarlastDay.getTime();
		}

		while (getDifferenceInMonths(lastDay, toDate) >= 0);
		return chargePeriods;
	}
}

