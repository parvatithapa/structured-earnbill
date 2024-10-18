package com.jbilling.test.ui;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

public interface CreditCard {

    public String getHolder ();

    public String getNumber ();

    public String getExpiryDate ();

    @Immutable
    public final static class DefaultCreditCard implements CreditCard {

        private final String holder;
        private final String number;
        private final String expiryDate;

        @Override
        public String getHolder () {
            return holder;
        }

        @Override
        public String getNumber () {
            return number;
        }

        @Override
        public String getExpiryDate () {
            return expiryDate;
        }

        public DefaultCreditCard(String holder, String number, String expiryDate) {
            this.holder = Objects.requireNonNull(holder);
            this.number = Objects.requireNonNull(number);
            this.expiryDate = Objects.requireNonNull(expiryDate);
        }

        @Override
        public String toString () {
            StringBuilder builder = new StringBuilder();
            // @formatter:off
            builder.append("DefaultCreditCard [holder=").append(holder)
                    .append(", number=").append(number)
                    .append(", expiryDate=").append(expiryDate)
                    .append(", getClass()=").append(getClass())
                    .append(", hashCode()=").append(hashCode()).append("]");
            // @formatter:on
            return builder.toString();
        }
    }
}
