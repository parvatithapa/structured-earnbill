package com.jbilling.test.ui;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

public interface ClearingData {

    public String getRoutingNumber ();

    public String getCustomerName ();

    public String getAccountNumber ();

    public String getBankName ();

    public String getAccountType ();

    @Immutable
    public final static class DefaultClearingData implements ClearingData {

        private final String routingNumber;
        private final String customerName;
        private final String accountNumber;
        private final String bankName;
        private final String accountType;

        @Override
        public String getRoutingNumber () {
            return routingNumber;
        }

        @Override
        public String getCustomerName () {
            return customerName;
        }

        @Override
        public String getAccountNumber () {
            return accountNumber;
        }

        @Override
        public String getBankName () {
            return bankName;
        }

        @Override
        public String getAccountType () {
            return accountType;
        }

        // public ClearingData build () {
        //
        // }
        public DefaultClearingData(String routingNumber, String customerName, String accountNumber, String bankName,
                String accountType) {
            this.routingNumber = Objects.requireNonNull(routingNumber);
            this.customerName = Objects.requireNonNull(customerName);
            this.accountNumber = Objects.requireNonNull(accountNumber);
            this.bankName = Objects.requireNonNull(bankName);
            this.accountType = Objects.requireNonNull(accountType);
        }

        @Override
        public String toString () {
            StringBuilder builder = new StringBuilder();
            // @formatter:off
            builder.append("DefaultClearingData [routingNumber=").append(routingNumber)
                    .append(", customerName=").append(customerName)
                    .append(", accountNumber=").append(accountNumber)
                    .append(", bankName=").append(bankName)
                    .append(", accountType=").append(accountType)
                    .append(", getClass()=").append(getClass())
                    .append(", hashCode()=").append(hashCode()).append("]");
            // @formatter:on
            return builder.toString();
        }
    }
}
