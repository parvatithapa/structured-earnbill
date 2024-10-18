package com.sapienter.jbilling.server.usageRatingScheme.util;

import java.util.Calendar;

public enum ResetCycle {

    DAY {
        @Override
        public Integer getDayOfCycle() {
            return Calendar.DAY_OF_MONTH;
        }

        @Override
        public Integer map() {
            return Calendar.DAY_OF_YEAR;
        }
    },

    WEEK {
        @Override
        public Integer getDayOfCycle() {
            return Calendar.DAY_OF_WEEK;
        }

        @Override
        public Integer map() {
            return Calendar.WEEK_OF_YEAR;
        }
    },

    MONTH {
        @Override
        public Integer getDayOfCycle() {
            return Calendar.DAY_OF_MONTH;
        }

        @Override
        public Integer map() {
            return Calendar.MONTH;
        }
    },

    YEAR {
        @Override
        public Integer getDayOfCycle() {
            return Calendar.DAY_OF_YEAR;
        }

        @Override
        public Integer map() {
            return Calendar.YEAR;
        }
    };

    public String getKey() {
        return name();
    }

    public abstract Integer getDayOfCycle();

    public abstract Integer map();
}
