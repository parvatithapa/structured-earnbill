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

package com.sapienter.jbilling.server.util.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;

/**
 * Utility class for calculate the initial pro rate for periods
 *
 * @author Leandro Zoi
 * @since 11-16-2017
 *
 */
public enum ProRatePeriodCalculator {

    MONTHLY(1) {

        @Override
        public LocalDate getDate(LocalDate localDate, Date nextInvoiceDate, MainSubscriptionDTO mainSubscription) {
            int year = localDate.getYear();
            int month = localDate.getMonthValue();
            int dayOfMonth = 0;
            int numberOfdayInMonth = getDayOfMonth(nextInvoiceDate);
            // Scenario : 1. 29th Monthly customer with prorated post order with active since date as 29th Jan 2021.
            // 2. Bill run of 29th Jan, moved NID to 28th Feb
            // 3. Bill run of 28th Feb,
            // Result :  Post Paid order charged period - 29/01/2021 to 27/02/2021 and amount charged correctly 
            //           Pre Paid order charged period - 28/02/2021 to 28/03/2021 and amount charged correctly.
            
            if ((numberOfdayInMonth == 28 || Year.isLeap(year)) && mainSubscription.getNextInvoiceDayOfPeriod() > numberOfdayInMonth) {
                dayOfMonth =checkValidDate(year, month, mainSubscription.getNextInvoiceDayOfPeriod());
            } else {
                dayOfMonth = checkValidDate(year, month, numberOfdayInMonth);
            }
            
            if(null != mainSubscription && mainSubscription.getNextInvoiceDayOfPeriod() == 31) {    
                dayOfMonth = getLengthOfMonth(year, month);
            } 
            return LocalDate.of(year, month, dayOfMonth);
        }

        @Override
        public LocalDate getNextBeforeDate(LocalDate localDate, MainSubscriptionDTO mainSubscription) {            

        	localDate = localDate.minusMonths(1);

        	if (localDate.getMonthValue() != 2 && localDate.getDayOfMonth() != mainSubscription.getNextInvoiceDayOfPeriod()) {        		
        		if(mainSubscription.getNextInvoiceDayOfPeriod() > localDate.getDayOfMonth()) {
        			return localDate.withDayOfMonth(localDate.getDayOfMonth());
        		}
        		return localDate.withDayOfMonth(mainSubscription.getNextInvoiceDayOfPeriod());
        	}
        	return localDate;
        }
    },
    WEEKLY(2) {

        @Override
        public LocalDate getDate(LocalDate localDate, Date nextInvoiceDate, MainSubscriptionDTO mainSubscription) {
            return localDate.with(DayOfWeek.valueOf(MainSubscriptionWS.weekDaysMap
                    .get(getDayOfWeek(nextInvoiceDate))
                    .toUpperCase()));
        }

        @Override
        public LocalDate getNextBeforeDate(LocalDate localDate, MainSubscriptionDTO mainSubscription) {
            return localDate.minusWeeks(1);
        }

        private int getDayOfWeek(Date aDate) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(aDate);
            return cal.get(Calendar.DAY_OF_WEEK);
        }

    },
    DAILY(3) {

        @Override
        public LocalDate getDate(LocalDate localDate, Date nextInvoiceDate, MainSubscriptionDTO mainSubscription) {
            return localDate;
        }

        @Override
        public LocalDate getNextBeforeDate(LocalDate localDate, MainSubscriptionDTO mainSubscription) {
            return localDate.minusDays(1);
        }
    },
    ANNUAL(4) {

        @Override
        public LocalDate getDate(LocalDate localDate, Date nextInvoiceDate, MainSubscriptionDTO mainSubscription) {
            LocalDate yearly = localDate.withDayOfYear(getDayOfYear(nextInvoiceDate));
            int year = localDate.getYear();
            int month = yearly.getMonthValue();
            int dayOfMonth = checkValidDate(year, month, yearly.getDayOfMonth());

            return LocalDate.of(year, month, dayOfMonth);
        }

        @Override
        public LocalDate getNextBeforeDate(LocalDate localDate, MainSubscriptionDTO mainSubscription) {
            localDate = localDate.minusYears(1);
            LocalDate yearly = localDate.withDayOfYear(mainSubscription.getNextInvoiceDayOfPeriod());

            if (localDate.getMonthValue() != 2 && localDate.getDayOfMonth() != yearly.getDayOfMonth()) {
                return localDate.withDayOfMonth(yearly.getDayOfMonth());
            }

            return localDate;
        }

        private int getDayOfYear(Date aDate) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(aDate);
            int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
            if(Year.isLeap(cal.get(Calendar.YEAR)) && dayOfYear > 60) {
                dayOfYear = dayOfYear - 1;
            }
            return dayOfYear;
        }
    },
    SEMI_MONTHLY(5) {

        @Override
        public LocalDate getDate(LocalDate localDate, Date nextInvoiceDate, MainSubscriptionDTO mainSubscription) {
            int day = localDate.getDayOfMonth();
            int nextInvoiceDayOfMonth = getDayOfSemiMonth(nextInvoiceDate);

            if (day <= 15) {
                day = 0;
            } else if (localDate.lengthOfMonth() < nextInvoiceDayOfMonth + 15) {
                day = localDate.lengthOfMonth() - nextInvoiceDayOfMonth;
            } else {
                day = 15;
            }

            int year = localDate.getYear();
            int month = localDate.getMonthValue();
            int dayOfMonth = nextInvoiceDayOfMonth + day;

            return LocalDate.of(year, month, dayOfMonth);
        }

        @Override
        public LocalDate getNextBeforeDate(LocalDate localDate, MainSubscriptionDTO mainSubscription) {
            if (localDate.getDayOfMonth() == 15) {
                localDate = localDate.minusMonths(1).withDayOfMonth(localDate.lengthOfMonth());
            } else if (localDate.getDayOfMonth() > 15) {
                LocalDate weekly = localDate.minusDays(15);
                if (localDate.getDayOfMonth() == 2) {
                    if (mainSubscription.getNextInvoiceDayOfPeriod() == 15) {
                        localDate = weekly.withDayOfMonth(15);
                    } else if (mainSubscription.getNextInvoiceDayOfPeriod() == 14) {
                        localDate = weekly.withDayOfMonth(14);
                    }
                } else {
                    localDate = localDate.minusDays(15);
                }
            } else {
                localDate = localDate.minusMonths(1).plusDays(15);
            }

            return localDate;
        }

        private int getDayOfSemiMonth(Date date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            if(dayOfMonth > 15) {
                dayOfMonth = dayOfMonth - 15;
            }
            return dayOfMonth;
        }
    };

    private final int id;

    ProRatePeriodCalculator(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public abstract LocalDate getDate(LocalDate localDate, Date nextInvoiceDate, MainSubscriptionDTO mainSubscription);
    public abstract LocalDate getNextBeforeDate (LocalDate localDate, MainSubscriptionDTO mainSubscription);

    public static ProRatePeriodCalculator valueOfPeriodUnit(int id) {
        return Arrays.stream(ProRatePeriodCalculator.values())
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Avoid a RunTimeException where create a invalid date for non leap years
     *
     * @param year           period start year
     * @param month          period start month
     * @param dayOfMonth     period start day of month
     * @return
     */
    private static int checkValidDate(int year, int month, int dayOfMonth) {
        if (month == 2 && dayOfMonth > 28) {
            if (Year.isLeap(year) && dayOfMonth >= 29) {
                return 29;
            } else {
                return 28;
            }
        }
        return dayOfMonth;
    }

    private static int getDayOfMonth(Date aDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(aDate);
        return cal.get(Calendar.DAY_OF_MONTH);
    }
    
    private static int getLengthOfMonth(int year, int month){
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }

}