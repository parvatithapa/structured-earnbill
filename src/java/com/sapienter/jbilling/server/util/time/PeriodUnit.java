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

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.PersistentEnum;

;

/**
 * Utility class for working with period units. Can be persisted by Hibernate.
 *
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since 2015-04-10
 *
 */
public enum PeriodUnit implements PersistentEnum {

    MONTHLY(1) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusMonths(amount);
        }
    },
    WEEKLY(2) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusWeeks(amount);
        }

        @Override
        public LocalDate getForDay(LocalDate temporal, int day) {
            int dayOfWeek = temporal.getDayOfWeek().getValue() + 1;
            if (dayOfWeek == day) {
                return temporal;
            }
            if (dayOfWeek > day) {
                day = 7 - (dayOfWeek - day);
            } else if (dayOfWeek < day) {
                day =  day - dayOfWeek;
            }
            return temporal.plusDays(day);
        }
    },
    DAYLY(3) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusDays(amount);
        }
    },
    ANNUAL(4) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            return temporal.plusYears(amount);
        }

        @Override
        public LocalDate getForDay(LocalDate temporal, int day) {
            return temporal.withDayOfYear(day);
        }
    },
    SEMI_MONTHLY(5) {
        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {
            LocalDate temporalToAdd = midMonthShouldActLike14OfThatMonth(max29ForMonth(temporal));
            int initialDay = temporalToAdd.getDayOfMonth();
            boolean inLastHalfOfMonth = initialDay > 15;
            LocalDate result = temporalToAdd.plusMonths(amount / 2);

            if (amount % 2 != 0) {
                if (inLastHalfOfMonth && (amount > 0)) {
                    result = result.plusMonths(1);
                } else if (!inLastHalfOfMonth && (amount < 0)) {
                    result = result.minusMonths(1);
                }
                result = (inLastHalfOfMonth) ? result.withDayOfMonth(initialDay - 15) : result.withDayOfMonth(Math.min(
                        result.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth(), initialDay + 15));
            }

            //Non Leap year, the 28 of february should go to 14 of next month
            if (!temporalToAdd.isLeapYear() && temporalToAdd.getMonth().equals(Month.FEBRUARY) &&
                    temporalToAdd.getDayOfMonth() == 28)
                result = result.plusDays(1);
            return result;
        }

        protected LocalDate max29ForMonth(LocalDate temporal) {
            if (temporal.getDayOfMonth() == 31)
                return temporal.minusDays(2);
            else if  (temporal.getDayOfMonth() == 30)
                return temporal.minusDays(1);
            return temporal;
        }

        protected LocalDate midMonthShouldActLike14OfThatMonth(LocalDate temporal) {
            if (temporal.getDayOfMonth() == 15)
                return temporal.minusDays(1);
            return temporal;
        }
    },
    SEMI_MONTHLY_EOM(6) {

        @Override
        protected LocalDate _addTo (LocalDate temporal, long amount) {

            boolean endOfMonth = isTheLastDayOfMonth(temporal);
            LocalDate result = temporal.plusMonths(amount / 2);

            if (amount % 2 == 0) {
                if (endOfMonth) {
                    result = result.with(TemporalAdjusters.lastDayOfMonth());
                }
            } else {
                if (endOfMonth && (amount > 0)) {
                    result = result.plusMonths(1);
                } else if (!endOfMonth && (amount < 0)) {
                    result = result.minusMonths(1);
                }
                result = (endOfMonth) ? result.withDayOfMonth(15) : result.with(TemporalAdjusters.lastDayOfMonth());
            }
            return result;
        }

        private boolean isTheLastDayOfMonth (LocalDate temporal) {
            return temporal.equals(temporal.with(TemporalAdjusters.lastDayOfMonth()));
        }

        @Override
        public void validate (LocalDate temporal) {
            if ((temporal.getDayOfMonth() == 15) || isTheLastDayOfMonth(temporal)) {
                return;
            }
            throw new IllegalArgumentException("Day of month in [" + temporal + "] should be 15 or the end of month");
        }
    }; // , BI_WEEKLY(7), QUATERLY(8), SEMI_ANNUAL(9);

    private final int id;

    PeriodUnit (int id) {
        this.id = id;
    }

    @Override
    public int getId () {
        return id;
    }

    public LocalDate addTo (LocalDate temporal, long amount) {
        validate(temporal);
        return _addTo(temporal, amount);
    }

    protected abstract LocalDate _addTo (LocalDate temporal, long amount);

    public LocalDate getForDay(LocalDate temporal, int day) {
        return temporal.withDayOfMonth(day);
    }

    public void validate (LocalDate temporal) throws IllegalArgumentException {
    }

    /**
     * Finds the type of PeriodUnit from its start day of month and period unit identifier
     * 
     * @param dayOfMonth     period start day of month
     * @param periodUnitId   period unit identifier
     * @return 
     */
    public static PeriodUnit valueOfPeriodUnit (int dayOfMonth, int periodUnitId) {

        if (periodUnitId == Constants.PERIOD_UNIT_MONTH) {
            return PeriodUnit.MONTHLY;
        } else if (periodUnitId == Constants.PERIOD_UNIT_WEEK) {
            return PeriodUnit.WEEKLY;
        } else if (periodUnitId == Constants.PERIOD_UNIT_DAY) {
            return PeriodUnit.DAYLY;
        } else if (periodUnitId == Constants.PERIOD_UNIT_YEAR) {
            return PeriodUnit.ANNUAL;
        } else if (periodUnitId == Constants.PERIOD_UNIT_SEMI_MONTHLY) {
            return (dayOfMonth == 15) ? PeriodUnit.SEMI_MONTHLY_EOM : PeriodUnit.SEMI_MONTHLY;
        }

        throw new IllegalArgumentException("Unsupported PeriodUnitDTO id[" + periodUnitId + "]");
    }
}
